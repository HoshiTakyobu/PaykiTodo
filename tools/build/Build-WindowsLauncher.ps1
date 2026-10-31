param([Parameter(Mandatory)][string]$OutputDirectory)
$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$launcherOutput = Join-Path $repo ('desktop/obj/launcher/' + [Guid]::NewGuid().ToString('N'))
dotnet publish (Join-Path $repo 'desktop/PaykiTodo.Launcher/PaykiTodo.Launcher.csproj') -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:DebugType=None -o $launcherOutput
if ($LASTEXITCODE -ne 0) { throw 'Launcher publish failed' }
Copy-Item -LiteralPath (Join-Path $launcherOutput 'PaykiTodo.Launcher.exe') -Destination $OutputDirectory
