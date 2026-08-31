[CmdletBinding()]
param(
    [switch]$FrameworkDependent,
    [switch]$OpenOutput
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$project = Join-Path $repo 'desktop\PaykiTodo.Desktop\PaykiTodo.Desktop.csproj'
$output = Join-Path $repo 'desktop\dist'

if (-not (Test-Path -LiteralPath $project)) { throw "找不到 Windows 客户端项目：$project" }

$runtimeArgs = @('--runtime', 'win-x64', '--self-contained', ($(if ($FrameworkDependent) { 'false' } else { 'true' })))

dotnet publish $project --configuration Release --output $output @runtimeArgs
if ($LASTEXITCODE -ne 0) { throw "Windows 客户端发布失败，退出码 $LASTEXITCODE" }

$exe = Join-Path $output 'PaykiTodo.exe'
if (-not (Test-Path -LiteralPath $exe)) { throw "发布完成但找不到 EXE：$exe" }
$hash = (Get-FileHash -LiteralPath $exe -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "Windows EXE：$exe"
Write-Host "SHA-256：$hash"
if ($OpenOutput) { Invoke-Item $output }
