[CmdletBinding()]
param(
    [switch]$SkipDebugBuild,
    [switch]$OpenOutput
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$gradle = Join-Path $repo 'gradlew.bat'
$gradleFile = Join-Path $repo 'app\build.gradle.kts'
$signingFile = Join-Path $repo 'keystore.properties'

if (-not (Test-Path -LiteralPath $gradle)) { throw "找不到 Gradle wrapper: $gradle" }
if (-not (Test-Path -LiteralPath $signingFile)) {
    throw '找不到 keystore.properties。请先根据 keystore.properties.example 填写本机签名配置。'
}

$versionText = Get-Content -LiteralPath $gradleFile -Raw
$versionName = [regex]::Match($versionText, 'paykiVersionName\s*=\s*"([^"]+)"').Groups[1].Value
$versionCode = [regex]::Match($versionText, 'paykiVersionCode\s*=\s*(\d+)').Groups[1].Value
if ([string]::IsNullOrWhiteSpace($versionName) -or [string]::IsNullOrWhiteSpace($versionCode)) {
    throw '无法从 app/build.gradle.kts 读取当前版本。'
}

$debugApk = Join-Path $repo "app\build\outputs\apk\debug\PaykiTodo-$versionName-debug.apk"
$releaseApk = Join-Path $repo "app\build\outputs\apk\release\PaykiTodo-$versionName-release.apk"

Write-Host "PaykiTodo 正式包构建器"
Write-Host "当前源码版本：$versionName / versionCode $versionCode"
Write-Host '说明：Release 不是对 Debug APK 做二进制转换，而是用同一份源码和正式签名重新构建。'

if (-not $SkipDebugBuild -or -not (Test-Path -LiteralPath $debugApk)) {
    Write-Host '正在确认 Debug 包可构建…'
    & $gradle ':app:assembleDebug' '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Debug 构建失败，退出码 $LASTEXITCODE" }
}

Write-Host '正在构建签名 Release 包（启用 R8 与资源压缩）…'
& $gradle ':app:assembleRelease' '--console=plain'
if ($LASTEXITCODE -ne 0) { throw "Release 构建失败，退出码 $LASTEXITCODE" }
if (-not (Test-Path -LiteralPath $releaseApk)) { throw "构建完成但找不到输出：$releaseApk" }

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
    if ($LASTEXITCODE -ne 0) { throw "Release 签名校验失败，退出码 $LASTEXITCODE" }
    Write-Host '签名校验：通过'
} else {
    Write-Warning '未找到 apksigner，已完成 Gradle Release 构建，但未执行独立签名校验。'
}

$sizeMb = [math]::Round((Get-Item -LiteralPath $releaseApk).Length / 1MB, 2)
Write-Host "Release APK：$releaseApk"
Write-Host "Release 大小：$sizeMb MB"
Write-Host "SHA-256：$hash"
Write-Host "校验文件：$hashFile"

if ($OpenOutput) {
    Invoke-Item (Split-Path -Parent $releaseApk)
}
