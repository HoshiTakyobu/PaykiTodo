<# Build local release assets. Uploading and changing rollout remain explicit release operations. #>
param(
    [Parameter(Mandatory)][string]$PayloadDirectory,
    [Parameter(Mandatory)][string]$OutputDirectory,
    [Parameter(Mandatory)][string]$Version,
    [string]$BaselineDirectory,
    [string]$BaselineVersion,
    [int]$RolloutPercent = 100,
    [string]$RolloutSeed,
    [string]$SigningKey
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$SigningKey) { $SigningKey = Join-Path $repo 'user/update-signing/private-key.pem' }
if ($Version -notmatch '^\d+\.\d+\.\d+$' -or $RolloutPercent -lt 0 -or $RolloutPercent -gt 100) { throw 'Invalid version or rollout' }
if (!$RolloutSeed) { $RolloutSeed = 'PaykiTodo-' + $Version }
$payload = (Resolve-Path -LiteralPath $PayloadDirectory).Path
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$output = (Resolve-Path -LiteralPath $OutputDirectory).Path
if ($output.StartsWith($payload + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or $output -eq $payload) { throw 'Output must be outside payload' }
function Get-Entries([string]$directory) {
    if (Get-ChildItem -LiteralPath $directory -Recurse | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }) { throw 'Links are not release files' }
    $entries = @()
    foreach ($file in Get-ChildItem -LiteralPath $directory -Recurse -File) {
        $relative = [IO.Path]::GetRelativePath($directory, $file.FullName).Replace('\','/')
        if ($relative -in 'client-config.json','update-files.json','update-manifest.json' -or $file.Extension -eq '.pdb') { continue }
        if ($file.Extension -in '.db','.sqlite','.sqlite3','.key','.pem','.jks','.paykibak','.log' -or $file.Name -match '^(credentials|account|profile|\.env)') { throw 'Private material in release payload' }
        $entries += [pscustomobject]@{path=$relative;size=$file.Length;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
    }
    # Canonical ordering is ordinal, independent of the build machine culture.
    $names = [string[]]@($entries.path); [Array]::Sort($names,[StringComparer]::Ordinal)
    @($names | ForEach-Object { $name = $_; $entries | Where-Object path -CEQ $name })
}
function Write-Archive([string]$name, [string]$directory, $entries) {
    $path = Join-Path $output $name
    $stream = [IO.File]::Open($path, [IO.FileMode]::CreateNew)
    $zip = [IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
    try { foreach ($entry in $entries) { [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,(Join-Path $directory $entry.path),$entry.path,[IO.Compression.CompressionLevel]::Optimal) | Out-Null } }
    finally { $zip.Dispose(); $stream.Dispose() }
    @{asset=$name;size=(Get-Item -LiteralPath $path).Length;sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$files = @(Get-Entries $payload)
if (!($files.path -ccontains 'PaykiTodo.exe') -or !($files.path -ccontains 'PaykiTodo.Launcher.exe')) { throw 'App and stable launcher required' }
$actualVersion = [Diagnostics.FileVersionInfo]::GetVersionInfo((Join-Path $payload 'PaykiTodo.exe')).FileVersion
if ($actualVersion -ne ($Version + '.0')) { throw 'Payload executable version does not match release' }
$full = Write-Archive ('PaykiTodo-' + $Version + '-windows-full.zip') $payload $files
$deltas = @()
if ($BaselineDirectory) {
    if ($BaselineVersion -notmatch '^\d+\.\d+\.\d+$' -or [version]$BaselineVersion -ge [version]$Version) { throw 'A lower numeric baseline version is required' }
    $baseline = (Resolve-Path -LiteralPath $BaselineDirectory).Path
    $old = @(Get-Entries $baseline)
    $canonical = -join @($old | ForEach-Object { $_.path + "`t" + $_.size + "`t" + $_.sha256 + "`n" })
    $baselineHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($canonical))).ToLowerInvariant()
    $changed = @($files | Where-Object { $new = $_; !($old | Where-Object { $_.path -ceq $new.path -and $_.size -eq $new.size -and $_.sha256 -ceq $new.sha256 }) })
    $delta = Write-Archive ('PaykiTodo-' + $BaselineVersion + '-to-' + $Version + '-windows-delta.zip') $payload $changed
    $delta.fromVersion = $BaselineVersion; $delta.baselineSha256 = $baselineHash; $deltas += $delta
}
$manifest = @{schema=1;version=$Version;tag=('v'+$Version);rollout=@{percent=$RolloutPercent;seed=$RolloutSeed};windows=@{entryPoint='PaykiTodo.exe';files=$files;full=$full;deltas=$deltas}}
$raw = [Text.Encoding]::UTF8.GetBytes(($manifest | ConvertTo-Json -Depth 10 -Compress))
$rsa = [Security.Cryptography.RSA]::Create()
try {
    $rsa.ImportFromPem([IO.File]::ReadAllText((Resolve-Path -LiteralPath $SigningKey).Path))
    $public = [Security.Cryptography.RSA]::Create()
    try {
        $public.ImportFromPem([IO.File]::ReadAllText((Join-Path $repo 'desktop/PaykiTodo.Desktop/update-public-key.txt')))
        $signature = $rsa.SignData($raw,[Security.Cryptography.HashAlgorithmName]::SHA256,[Security.Cryptography.RSASignaturePadding]::Pss)
        if (!$public.VerifyData($raw,$signature,[Security.Cryptography.HashAlgorithmName]::SHA256,[Security.Cryptography.RSASignaturePadding]::Pss)) { throw 'Signing key differs from client trust key' }
    } finally { $public.Dispose() }
    $envelope = @{manifest=[Convert]::ToBase64String($raw);signature=[Convert]::ToBase64String($signature)}
    [IO.File]::WriteAllText((Join-Path $output 'PaykiTodo-update-manifest.json'),($envelope | ConvertTo-Json -Compress))
} finally { $rsa.Dispose() }
[IO.File]::WriteAllText((Join-Path $payload 'update-files.json'),($files | ConvertTo-Json -Depth 5 -Compress))
Write-Output ('Local update assets ready: ' + $output)
