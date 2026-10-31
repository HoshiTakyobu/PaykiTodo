param([Parameter(Mandatory)][string]$Evidence, [Parameter(Mandatory)][string]$Launcher)
$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$test = Join-Path ([IO.Path]::GetFullPath($Evidence)) ([Guid]::NewGuid().ToString('N'))
$source = Join-Path $test 'fixture-source'
New-Item -ItemType Directory -Force -Path $source | Out-Null
$protocol = Join-Path $repo 'desktop/PaykiTodo.Desktop/DesktopUpdateProtocol.cs'
$store = Join-Path $repo 'desktop/PaykiTodo.Desktop/DesktopUpdateStore.cs'
$key = Join-Path $repo 'desktop/PaykiTodo.Desktop/update-public-key.txt'
$project = @"
<Project Sdk="Microsoft.NET.Sdk"><PropertyGroup><OutputType>Exe</OutputType><TargetFramework>net9.0-windows</TargetFramework><ImplicitUsings>enable</ImplicitUsings><Nullable>enable</Nullable><AssemblyName>PaykiTodo</AssemblyName></PropertyGroup><ItemGroup><Compile Include="$protocol" Link="DesktopUpdateProtocol.cs"/><Compile Include="$store" Link="DesktopUpdateStore.cs"/><EmbeddedResource Include="$key" LogicalName="PaykiTodo.UpdatePublicKey"/></ItemGroup></Project>
"@
[IO.File]::WriteAllText((Join-Path $source 'Fixture.csproj'),$project)
$program = @'
using PaykiTodo.Desktop;
var profile = args[1];
var version = typeof(DesktopUpdateStore).Assembly.GetName().Version!.ToString(3);
File.AppendAllText(Path.Combine(profile,"observed.txt"), version + Environment.NewLine);
if (version == "1.17.12" && File.Exists(Path.Combine(profile,"fail.txt"))) return;
var nonce = Environment.GetEnvironmentVariable("PAYKI_UPDATE_NONCE");
if (!string.IsNullOrEmpty(nonce)) new DesktopUpdateStore(Environment.GetEnvironmentVariable("PAYKI_UPDATE_SEED")!,Environment.GetEnvironmentVariable("PAYKI_UPDATE_ROOT")).Confirm(nonce);
'@
[IO.File]::WriteAllText((Join-Path $source 'Program.cs'),$program)
$seed = Join-Path $test 'seed'
$target = Join-Path $test 'target'
foreach($item in @(@{version='1.17.11';path=$seed},@{version='1.17.12';path=$target})) {
    dotnet publish (Join-Path $source 'Fixture.csproj') -c Release -r win-x64 --self-contained false ('-p:Version='+$item.version) ('-p:FileVersion='+$item.version+'.0') -o $item.path
    if ($LASTEXITCODE -ne 0) { throw 'Fixture publish failed' }
    Copy-Item -LiteralPath $Launcher -Destination $item.path
}
& (Join-Path $repo 'tools/build/New-WindowsUpdateRelease.ps1') -PayloadDirectory $target -OutputDirectory (Join-Path $test 'assets') -Version 1.17.12
$results = @()
foreach($mode in 'good','failure','interrupted','tampered') {
    $profile = Join-Path $test $mode
    $updates = Join-Path $profile 'updates'
    $versionDir = Join-Path $updates 'versions/1.17.12'
    New-Item -ItemType Directory -Force -Path $versionDir | Out-Null
    Copy-Item -Path (Join-Path $target '*') -Destination $versionDir -Recurse
    Copy-Item -LiteralPath (Join-Path $test 'assets/PaykiTodo-update-manifest.json') -Destination (Join-Path $versionDir 'update-manifest.json')
    $state = @{installId='fixture';pending='1.17.12';active=$null;previous=$null;attempt=$null;nonce=$null;failed=$null;lastCheck=0}
    if ($mode -eq 'failure') { [IO.File]::WriteAllText((Join-Path $profile 'fail.txt'),'fail candidate') }
    if ($mode -eq 'interrupted') { $state.attempt='1.17.12';$state.nonce='interrupted' }
    if ($mode -eq 'tampered') { [IO.File]::WriteAllText((Join-Path $versionDir 'PaykiTodo.dll'),'damaged') }
    [IO.File]::WriteAllText((Join-Path $updates 'state.json'),($state | ConvertTo-Json -Compress))
    [IO.File]::WriteAllText((Join-Path $profile 'user-data.bin'),'preserved synthetic user data')
    $start = [Diagnostics.ProcessStartInfo]::new((Join-Path $seed 'PaykiTodo.Launcher.exe'))
    $start.UseShellExecute=$false;$start.CreateNoWindow=$true
    $start.ArgumentList.Add('--data-directory');$start.ArgumentList.Add($profile)
    $process = [Diagnostics.Process]::Start($start)
    if (!$process.WaitForExit(55000)) { throw 'Launcher fixture timed out' }
    $observed = @(Get-Content -LiteralPath (Join-Path $profile 'observed.txt'))
    $final = Get-Content -LiteralPath (Join-Path $updates 'state.json') -Raw | ConvertFrom-Json
    if ($mode -eq 'good') { if ($observed[-1] -ne '1.17.12' -or $final.active -ne '1.17.12') { throw 'Candidate did not activate' } }
    else { if ($observed[-1] -ne '1.17.11' -or $final.failed -ne '1.17.12') { throw 'Candidate did not roll back' } }
    if ([IO.File]::ReadAllText((Join-Path $profile 'user-data.bin')) -ne 'preserved synthetic user data') { throw 'User fixture changed' }
    $results += @{mode=$mode;observed=$observed;active=$final.active;failed=$final.failed;userDataPreserved=$true}
}
$results | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $test 'result.json') -Encoding utf8
Write-Output ('Launcher acceptance passed: ' + $test)
