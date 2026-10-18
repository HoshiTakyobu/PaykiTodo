param([string]$Device = 'emulator-5554', [string]$Evidence = 'G:\Workspace\Scratch\PaykiTodo\interactive-1.16.21')
$ErrorActionPreference = 'Stop'
$adb = 'C:\Users\hp\AppData\Local\Android\Sdk\platform-tools\adb.exe'
function Adb { & $adb -s $Device @args }
function Read-Ui {
    for ($attempt = 0; $attempt -lt 4; $attempt++) {
        $result = Adb shell uiautomator dump /sdcard/payki-external-ui.xml
        if ($result -match 'dumped') {
            [xml]$xml = (Adb shell cat /sdcard/payki-external-ui.xml)
            return $xml
        }
        Start-Sleep -Milliseconds 700
    }
    throw 'Could not read active Android UI'
}
function Show-Ui {
    (Read-Ui).SelectNodes('//node') | Where-Object { $_.text -or $_.'content-desc' } | ForEach-Object {
        $value = if ($_.password -eq 'true') { '[password hidden]' } else { $_.text }
        '{0} | {1} | {2}' -f $value,$_.'content-desc',$_.bounds
    }
}
function Tap-Node($node) {
    $b = [regex]::Matches($node.bounds, '\d+') | ForEach-Object { [int]$_.Value }
    Adb shell input tap ([int](($b[0]+$b[2])/2)) ([int](($b[1]+$b[3])/2))
    Start-Sleep -Milliseconds 800
}
function Click-Text([string]$Text) {
    $nodes = (Read-Ui).SelectNodes('//node')
    $node = $nodes | Where-Object { $_.'content-desc' -eq $Text } | Select-Object -First 1
    if (!$node) { $node = $nodes | Where-Object { $_.text -eq $Text } | Select-Object -First 1 }
    if (!$node) { throw "Visible control not found: $Text" }
    Tap-Node $node
}
function Type-Text([string]$Text) {
    if ($Text.Contains("'") -or $Text.Contains("`n") -or $Text.Contains("`r")) { throw 'Use single-line synthetic input without shell quotes' }
    Adb shell "input text '$($Text.Replace(' ', '%s'))'"
}
function Capture([string]$Name) {
    Adb shell screencap -p /sdcard/payki-external.png
    Adb pull /sdcard/payki-external.png (Join-Path $Evidence $Name) | Out-Null
}
function Record([string]$Id, [string]$Result) { Add-Content -LiteralPath (Join-Path $Evidence 'android-external-steps.txt') -Value "$Id PASS $Result" }
