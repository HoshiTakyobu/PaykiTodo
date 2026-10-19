param(
    [string]$Evidence = "G:\Workspace\Scratch\PaykiTodo\ui-1.16.22",
    [switch]$PhysicalInput,
    [string]$PackagedExe,
    [switch]$IsolatedChild
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
New-Item -ItemType Directory -Path $Evidence -Force | Out-Null
if ($PhysicalInput) {
    throw '此入口不接管系统输入。物理验收需单独安排用户不操作电脑的时段，显式设置 PAYKI_VISIBLE_ACCEPTANCE=1。'
}
if ($Evidence.Contains('"') -or $PSCommandPath.Contains('"')) { throw 'Unsupported quote in path' }
if ($IsolatedChild) {
    Start-Transcript -LiteralPath (Join-Path $Evidence 'windows-isolated-transcript.txt') -Append | Out-Null
    try {
        dotnet test "$root\desktop\PaykiTodo.Desktop.Tests\PaykiTodo.Desktop.Tests.csproj" --configuration Debug --filter 'FullyQualifiedName!~VisibleUserAcceptanceTests' --logger 'trx;LogFileName=windows-regressions.trx' --results-directory $Evidence
        $testExit = $LASTEXITCODE
        if ($testExit -eq 0) {
            # WPF allows only one Application per process, even after Shutdown.
            dotnet test "$root\desktop\PaykiTodo.Desktop.Tests\PaykiTodo.Desktop.Tests.csproj" --configuration Debug --no-build --filter 'FullyQualifiedName~VisibleUserAcceptanceTests' --logger 'trx;LogFileName=windows-isolated-uia.trx' --results-directory $Evidence
            $testExit = $LASTEXITCODE
        }
    } finally { Stop-Transcript | Out-Null }
    exit $testExit
}
Add-Type -Path (Join-Path $PSScriptRoot 'DesktopIsolation.cs')
$oldUia = $env:PAYKI_UIA_ACCEPTANCE
$oldPhysical = $env:PAYKI_VISIBLE_ACCEPTANCE
$oldEvidence = $env:PAYKI_UI_EVIDENCE
$oldDesktop = $env:PAYKI_ISOLATED_DESKTOP
$oldPackaged = $env:PAYKI_PACKAGED_EXE
try {
    $env:PAYKI_UIA_ACCEPTANCE = '1'
    $env:PAYKI_VISIBLE_ACCEPTANCE = '0'
    $env:PAYKI_UI_EVIDENCE = $Evidence
    if ($PackagedExe) { $env:PAYKI_PACKAGED_EXE = (Resolve-Path -LiteralPath $PackagedExe).Path }
    $env:PAYKI_ISOLATED_DESKTOP = 'PaykiTodoTests-' + [Guid]::NewGuid().ToString('N')
    $shellPath = (Get-Process -Id $PID).Path
    $arguments = '-NoProfile -NonInteractive -File "' + $PSCommandPath + '" -IsolatedChild -Evidence "' + $Evidence + '"'
    $testExit = [PaykiDesktopIsolation]::Run($shellPath, $arguments, $root, $env:PAYKI_ISOLATED_DESKTOP)
    Get-Content -LiteralPath (Join-Path $Evidence 'windows-isolated-transcript.txt') -Tail 45
    if ($testExit -ne 0) { throw '独立桌面控件验收失败，见 TRX 和步骤记录。' }
} finally {
    $env:PAYKI_UIA_ACCEPTANCE = $oldUia
    $env:PAYKI_VISIBLE_ACCEPTANCE = $oldPhysical
    $env:PAYKI_UI_EVIDENCE = $oldEvidence
    $env:PAYKI_ISOLATED_DESKTOP = $oldDesktop
    $env:PAYKI_PACKAGED_EXE = $oldPackaged
}
