param([Parameter(Mandatory)][string]$Installer, [string]$PreviousInstaller, [Parameter(Mandatory)][string]$Evidence)
$ErrorActionPreference = 'Stop'
$Installer = (Resolve-Path -LiteralPath $Installer).Path
$evidenceRoot = [IO.Path]::GetFullPath($Evidence)
if ($evidenceRoot -eq [IO.Path]::GetPathRoot($evidenceRoot)) { throw 'Evidence must be a dedicated test directory' }
New-Item -ItemType Directory -Path $evidenceRoot -Force | Out-Null
if ((Get-Item -LiteralPath $evidenceRoot).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Test directory cannot be a link' }
$testRoot = Join-Path $evidenceRoot ('install-' + [Guid]::NewGuid().ToString('N'))
$installRoot = Join-Path $testRoot 'application'
$startMenu = Join-Path $testRoot 'start-menu'
$desktop = Join-Path $testRoot 'desktop'
$profile = Join-Path $testRoot 'profile'
New-Item -ItemType Directory -Path $testRoot,$profile -Force | Out-Null
function Installed-PaykiTodo {
    Get-ChildItem HKCU:/Software/Microsoft/Windows/CurrentVersion/Uninstall,HKLM:/Software/Microsoft/Windows/CurrentVersion/Uninstall,HKLM:/Software/WOW6432Node/Microsoft/Windows/CurrentVersion/Uninstall |
        Get-ItemProperty | Where-Object DisplayName -eq 'PaykiTodo'
}
$existing = Installed-PaykiTodo
if ($existing) { throw 'An installed PaykiTodo already exists; do not replace a user installation with a test' }
if (Get-Process PaykiTodo -ErrorAction SilentlyContinue) { throw 'Close test PaykiTodo processes before installer acceptance' }
function Read-MsiProperty([string]$path,[string]$property) {
    $engine = New-Object -ComObject WindowsInstaller.Installer
    $db = $engine.OpenDatabase($path,0)
    $view = $db.OpenView("SELECT Value FROM Property WHERE Property = '$property'"); [void]$view.Execute(); $record = $view.Fetch()
    $value = if ($record) { $record.StringData(1) } else { '' }
    [void]$view.Close(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($view); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($db); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($engine)
    return $value
}
function Invoke-Msi([string[]]$arguments,[string]$stage) {
    $log = Join-Path $testRoot ($stage+'.log')
    $process = Start-Process -FilePath msiexec.exe -ArgumentList ($arguments + @('/qn','/norestart','/l*v',('"'+$log+'"'))) -PassThru -WindowStyle Hidden
    $process.WaitForExit()
    if ($process.ExitCode -notin 0,3010) { throw "MSI $stage failed ($($process.ExitCode)); see $log" }
}
function User-DataFingerprint {
    $path = Join-Path $env:LOCALAPPDATA 'PaykiTodo'
    if (!(Test-Path -LiteralPath $path)) { return '' }
    $queue = [Collections.Generic.Stack[string]]::new(); $queue.Push($path); $lines = [Collections.Generic.List[string]]::new()
    while ($queue.Count) {
        foreach($entry in Get-ChildItem -LiteralPath $queue.Pop() -Force) {
            if ($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
            if ($entry.PSIsContainer) { $queue.Push($entry.FullName) }
            else { $lines.Add([IO.Path]::GetRelativePath($path,$entry.FullName)+'|'+(Get-FileHash -LiteralPath $entry.FullName -Algorithm SHA256).Hash) }
        }
    }
    return [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(($lines | Sort-Object) -join "`n")))
}
$beforeData = User-DataFingerprint
[IO.File]::WriteAllText((Join-Path $testRoot 'user-data-before.sha256'), $beforeData)
$sentinel = Join-Path $profile 'preserve-data.txt'
[IO.File]::WriteAllText($sentinel,'PaykiTodo installer preservation fixture')
$markerHash = (Get-FileHash -LiteralPath $sentinel).Hash
$properties = @(('INSTALLFOLDER="'+$installRoot+'"'),('APP_START_MENU_DIR="'+$startMenu+'"'),('DESKTOP_SHORTCUT_DIR="'+$desktop+'"'))
if ($PreviousInstaller) {
    $previous = (Resolve-Path -LiteralPath $PreviousInstaller).Path
    Invoke-Msi (@('/i',('"'+$previous+'"')) + $properties) 'install-previous'
    if (!(Test-Path -LiteralPath (Join-Path $installRoot 'PaykiTodo.exe'))) { throw 'Previous installation missing' }
    # No directory overrides: verify that an upgrade discovers the existing chosen paths.
    Invoke-Msi @('/i',('"'+$Installer+'"')) 'upgrade-current'
} else { Invoke-Msi (@('/i',('"'+$Installer+'"')) + $properties) 'install-current' }
$productCode = Read-MsiProperty $Installer 'ProductCode'
$version = Read-MsiProperty $Installer 'ProductVersion'
$installed = Installed-PaykiTodo
if (@($installed).Count -ne 1 -or $installed.DisplayVersion -ne $version) { throw 'Installed version/upgrade registry mismatch' }
$engine = New-Object -ComObject WindowsInstaller.Installer
if ($engine.ProductInfo($productCode,'AssignmentType') -ne '0') { throw 'Installer must use per-user assignment' }
[void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($engine)
if (!(Test-Path -LiteralPath (Join-Path $installRoot 'PaykiTodo.exe'))) { throw 'Current EXE missing from selected installation directory' }
$shell = New-Object -ComObject WScript.Shell
foreach($path in @((Join-Path $startMenu 'PaykiTodo.lnk'),(Join-Path $desktop 'PaykiTodo.lnk'))) {
    if (!(Test-Path -LiteralPath $path)) { throw 'Shortcut missing' }
    if ($shell.CreateShortcut($path).TargetPath -ne (Join-Path $installRoot 'PaykiTodo.exe')) { throw 'Shortcut target mismatch' }
}
Invoke-Msi @('/fa',$productCode) 'repair-current'
if (Test-Path -LiteralPath ($Installer+'.files.json')) {
    foreach($file in (Get-Content -LiteralPath ($Installer+'.files.json') -Raw | ConvertFrom-Json)) {
        $installedFile = Join-Path $installRoot $file.Path
        if (!(Test-Path -LiteralPath $installedFile) -or (Get-FileHash -LiteralPath $installedFile).Hash -ne $file.SHA256) { throw "Installed payload mismatch: $($file.Path)" }
    }
}
# The same isolated WPF launch/controls harness validates the actual installed EXE.
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
& (Join-Path $PSScriptRoot 'Invoke-DesktopAcceptance.ps1') -Evidence (Join-Path $testRoot 'installed-app') -PackagedExe (Join-Path $installRoot 'PaykiTodo.exe')
Invoke-Msi @('/x',$productCode) 'uninstall-current'
if (Test-Path -LiteralPath (Join-Path $installRoot 'PaykiTodo.exe')) { throw 'EXE left installed after uninstall' }
if (Test-Path -LiteralPath (Join-Path $desktop 'PaykiTodo.lnk')) { throw 'Desktop shortcut left after uninstall' }
if (Test-Path -LiteralPath (Join-Path $startMenu 'PaykiTodo.lnk')) { throw 'Start-menu shortcut left after uninstall' }
if ((Get-FileHash -LiteralPath $sentinel).Hash -ne $markerHash -or (User-DataFingerprint) -ne $beforeData) { throw 'User data changed during installer acceptance' }
[pscustomobject]@{Version=$version;ProductCode=$productCode;InstallerSHA256=(Get-FileHash -LiteralPath $Installer).Hash;TestRoot=$testRoot;Install=$true;Upgrade=[bool]$PreviousInstaller;Repair=$true;PayloadHashes=$true;InstalledApp=$true;Uninstall=$true;UserDataUnchanged=$true} |
    ConvertTo-Json | Set-Content -LiteralPath (Join-Path $testRoot 'result.json') -Encoding utf8
Get-Content -LiteralPath (Join-Path $testRoot 'result.json')
