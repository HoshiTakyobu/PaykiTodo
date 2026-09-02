[CmdletBinding()]
param(
    [switch]$SkipDebugBuild,
    [switch]$OpenOutput
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gradle = Join-Path $repo 'gradlew.bat'
$gradleFile = Join-Path $repo 'app\build.gradle.kts'
$signingFile = @(
    (Join-Path $repo 'user\keystore.properties'),
    (Join-Path $repo 'keystore.properties')
) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not (Test-Path -LiteralPath $gradle)) { throw "Gradle wrapper not found: $gradle" }
if ([string]::IsNullOrWhiteSpace($signingFile)) {
    throw 'Local signing configuration not found. Fill user\keystore.properties using keystore.properties.example.'
}

$versionText = Get-Content -LiteralPath $gradleFile -Raw
$versionName = [regex]::Match($versionText, 'paykiVersionName\s*=\s*"([^"]+)"').Groups[1].Value
$versionCode = [regex]::Match($versionText, 'paykiVersionCode\s*=\s*(\d+)').Groups[1].Value
if ([string]::IsNullOrWhiteSpace($versionName) -or [string]::IsNullOrWhiteSpace($versionCode)) {
    throw 'Could not read the current version from app/build.gradle.kts.'
}

$debugApk = Join-Path $repo "app\build\outputs\apk\debug\PaykiTodo-$versionName-debug.apk"
$releaseApk = Join-Path $repo "app\build\outputs\apk\release\PaykiTodo-$versionName-release.apk"

Write-Host 'PaykiTodo release builder'
Write-Host "Source version: $versionName / versionCode $versionCode"
Write-Host 'Release is rebuilt from source with the release configuration and signing key; it is not a binary conversion of a Debug APK.'

if (-not $SkipDebugBuild -or -not (Test-Path -LiteralPath $debugApk)) {
    Write-Host 'Checking that the Debug build succeeds...'
    & $gradle ':app:assembleDebug' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Debug build failed with exit code $LASTEXITCODE" }
}

Write-Host 'Building the signed Release APK with R8 and resource shrinking...'
& $gradle ':app:assembleRelease' '--console=plain'
if ($LASTEXITCODE -ne 0) { throw "Release build failed with exit code $LASTEXITCODE" }
if (-not (Test-Path -LiteralPath $releaseApk)) { throw "Build completed but output was not found: $releaseApk" }

$hash = (Get-FileHash -LiteralPath $releaseApk -Algorithm SHA256).Hash.ToLowerInvariant()
$hashFile = "$releaseApk.sha256"
"$hash  $(Split-Path $releaseApk -Leaf)" | Set-Content -LiteralPath $hashFile -Encoding ASCII

$sdkRoot = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdkRoot)) { $sdkRoot = $env:ANDROID_SDK_ROOT }
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    $localProperties = Join-Path $repo 'local.properties'
    if (Test-Path -LiteralPath $localProperties) {
        $sdkRoot = (Get-Content -LiteralPath $localProperties -Raw |
            Select-String -Pattern '(?m)^sdk\.dir=(.+)$').Matches.Groups[1].Value
        if ($sdkRoot) {
            $sdkRoot = $sdkRoot.Replace('\:', ':').Replace('\\', '\').Trim()
        }
    }
}
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$apksigner = $null
if ($sdkRoot -and (Test-Path -LiteralPath (Join-Path $sdkRoot 'build-tools'))) {
    $apksigner = Get-ChildItem -LiteralPath (Join-Path $sdkRoot 'build-tools') -Directory |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName 'apksigner.bat' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
}
if ($apksigner) {
    & $apksigner verify $releaseApk
    if ($LASTEXITCODE -ne 0) { throw "Release signature verification failed with exit code $LASTEXITCODE" }
    Write-Host 'Signature verification: passed'
} else {
    Write-Warning 'apksigner was not found. The Gradle Release build completed, but independent signature verification was skipped.'
}

$sizeMb = [math]::Round((Get-Item -LiteralPath $releaseApk).Length / 1MB, 2)
Write-Host "Release APK: $releaseApk"
Write-Host "Release size: $sizeMb MB"
Write-Host "SHA-256: $hash"
Write-Host "Checksum file: $hashFile"

if ($OpenOutput) {
    Invoke-Item (Split-Path -Parent $releaseApk)
}
