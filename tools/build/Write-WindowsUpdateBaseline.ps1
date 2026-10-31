param([Parameter(Mandatory)][string]$PayloadDirectory)
$ErrorActionPreference = 'Stop'
$payload = (Resolve-Path -LiteralPath $PayloadDirectory).Path
if (Get-ChildItem -LiteralPath $payload -Recurse | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }) { throw 'Do not baseline links' }
$files = @()
foreach ($file in Get-ChildItem -LiteralPath $payload -Recurse -File) {
    $relative = [IO.Path]::GetRelativePath($payload,$file.FullName).Replace('\','/')
    if ($relative -in 'client-config.json','update-files.json','update-manifest.json' -or $file.Extension -eq '.pdb') { continue }
    if ($file.Extension -in '.db','.sqlite','.sqlite3','.key','.pem','.jks','.paykibak','.log' -or $file.Name -match '^(credentials|account|profile|\.env)') { throw 'Private material in update baseline' }
    $files += [pscustomobject]@{path=$relative;size=$file.Length;sha256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$names = [string[]]@($files.path); [Array]::Sort($names,[StringComparer]::Ordinal)
$ordered = @($names | ForEach-Object { $name=$_; $files | Where-Object path -CEQ $name })
[IO.File]::WriteAllText((Join-Path $payload 'update-files.json'),($ordered | ConvertTo-Json -Depth 5 -Compress))
