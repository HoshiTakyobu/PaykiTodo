param(
    [string]$WixPath = 'wix',
    [string]$ServerConfig,
    [string]$VersionOverride,
    [string]$WebView2Bootstrapper
)
$ErrorActionPreference = 'Stop'
$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$project = Join-Path $repo 'desktop/PaykiTodo.Desktop/PaykiTodo.Desktop.csproj'
[xml]$csproj = Get-Content -LiteralPath $project -Raw
$version = [string]$csproj.Project.PropertyGroup.Version
if ($VersionOverride) { $version = $VersionOverride }
if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Expected three-part numeric version' }
$build = Join-Path $repo ('desktop/obj/installer/' + $version + '-' + [Guid]::NewGuid().ToString('N'))
$payload = Join-Path $build 'payload'
$output = Join-Path $repo 'desktop/installer-output'
New-Item -ItemType Directory -Path $payload,$output -Force | Out-Null
dotnet publish $project -c Release -r win-x64 --self-contained true -p:PublishSingleFile=false -o $payload
if ($LASTEXITCODE -ne 0) { throw 'Windows publish failed' }
& (Join-Path $PSScriptRoot 'Build-WindowsLauncher.ps1') -OutputDirectory $payload
if ($ServerConfig) {
    $config = Get-Content -LiteralPath $ServerConfig -Raw | ConvertFrom-Json
    if (@($config.PSObject.Properties.Name).Count -ne 1 -or !$config.serverUrl) { throw 'Server config must contain only serverUrl' }
    $uri = [Uri]$config.serverUrl
    if (!$uri.IsAbsoluteUri -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) { throw 'Server URL must be HTTPS without credentials or query parameters' }
    @{serverUrl=$config.serverUrl} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $payload 'client-config.json') -Encoding utf8
}
Copy-Item -LiteralPath (Join-Path $repo 'desktop/installer/README.md') -Destination (Join-Path $payload 'Windows-installation.md')
$runtimeSetup = Join-Path $payload 'MicrosoftEdgeWebview2Setup.exe'
if ($WebView2Bootstrapper) {
    Copy-Item -LiteralPath (Resolve-Path -LiteralPath $WebView2Bootstrapper).Path -Destination $runtimeSetup
} else {
    Invoke-WebRequest -Uri 'https://go.microsoft.com/fwlink/p/?LinkId=2124703' -OutFile $runtimeSetup -TimeoutSec 60
}
$signature = Get-AuthenticodeSignature -LiteralPath $runtimeSetup
if ($signature.Status -ne 'Valid' -or $signature.SignerCertificate.Subject -notmatch 'O=Microsoft Corporation') { throw 'Microsoft WebView2 installer signature validation failed' }
& (Join-Path $PSScriptRoot 'Write-WindowsUpdateBaseline.ps1') -PayloadDirectory $payload
$license = Join-Path $build 'License.rtf'
$licenseText = (Get-Content -LiteralPath (Join-Path $repo 'LICENSE') -Raw).Replace('\','\\').Replace('{','\{').Replace('}','\}').Replace("`n",'\par ')
[IO.File]::WriteAllText($license, '{\rtf1\ansi\deff0{\fonttbl{\f0 Segoe UI;}}\fs20 ' + $licenseText + '}')

$ns = 'http://wixtoolset.org/schemas/v4/wxs'
$doc = New-Object Xml.XmlDocument
$wix = $doc.CreateElement('Wix',$ns); [void]$doc.AppendChild($wix)
$fragment = $doc.CreateElement('Fragment',$ns); [void]$wix.AppendChild($fragment)
function Add-Element($parent, $name, $attributes) {
    $element = $doc.CreateElement($name,$ns)
    foreach($entry in $attributes.GetEnumerator()) { $element.SetAttribute([string]$entry.Key,[string]$entry.Value) }
    [void]$parent.AppendChild($element); return $element
}
function Stable-Id([string]$name) { 'p' + [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($name.ToLowerInvariant()))).Substring(0,30) }
function Stable-Guid([string]$name) { [Guid]::ParseExact([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes('PaykiTodo/installer/'+$name.ToLowerInvariant()))).Substring(0,32),'N').ToString() }
$directoryRef = Add-Element $fragment 'DirectoryRef' @{Id='INSTALLFOLDER'}
$group = Add-Element $fragment 'ComponentGroup' @{Id='ApplicationFiles'}
$directories = @{''=$directoryRef}
$files = Get-ChildItem -LiteralPath $payload -File -Recurse | Sort-Object FullName
if (Get-ChildItem -LiteralPath $payload -Recurse | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }) { throw 'Do not package links' }
$manifest = @()
foreach($file in $files) {
    $relative = [IO.Path]::GetRelativePath($payload,$file.FullName)
    if ($file.Extension -in '.db','.sqlite','.sqlite3','.key','.jks','.paykibak' -or $file.Name -match '^(credentials|account|profile|\.env)') { throw 'Unexpected private file in publish payload' }
    $folder = [IO.Path]::GetDirectoryName($relative)
    if (!$directories.ContainsKey($folder)) {
        $parent = ''; $parentNode = $directoryRef
        foreach($segment in $folder.Split([IO.Path]::DirectorySeparatorChar)) {
            $current = if ($parent) { Join-Path $parent $segment } else { $segment }
            if (!$directories.ContainsKey($current)) { $directories[$current] = Add-Element $parentNode 'Directory' @{Id=(Stable-Id ('dir/'+$current));Name=$segment} }
            $parentNode = $directories[$current]; $parent = $current
        }
    }
    $id = Stable-Id $relative
    $component = Add-Element $directories[$folder] 'Component' @{Id=$id;Guid=(Stable-Guid $relative)}
    [void](Add-Element $component 'File' @{Id=('f'+$id);Source=$file.FullName;Name=$file.Name})
    [void](Add-Element $component 'RegistryValue' @{Root='HKCU';Key='Software\PaykiTodo\Installer\Files';Name=$id;Type='integer';Value='1';KeyPath='yes'})
    [void](Add-Element $group 'ComponentRef' @{Id=$id})
    $manifest += [pscustomobject]@{Path=$relative;Length=$file.Length;SHA256=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash}
}
foreach($entry in $directories.GetEnumerator()) {
    $id = Stable-Id ('remove/'+$entry.Key)
    $component = Add-Element $entry.Value 'Component' @{Id=$id;Guid=(Stable-Guid ('remove/'+$entry.Key))}
    [void](Add-Element $component 'RemoveFolder' @{Id=('r'+$id);On='uninstall'})
    [void](Add-Element $component 'RegistryValue' @{Root='HKCU';Key='Software\PaykiTodo\Installer\Directories';Name=$id;Type='integer';Value='1';KeyPath='yes'})
    [void](Add-Element $group 'ComponentRef' @{Id=$id})
}
$manifestPath = Join-Path $build 'Files.wxs'; $doc.Save($manifestPath)
$msi = Join-Path $output ('PaykiTodo-'+$version+'-windows-x64.msi')
& $WixPath build (Join-Path $repo 'desktop/installer/Package.wxs') $manifestPath -arch x64 -ext WixToolset.UI.wixext/4.0.6 -culture zh-CN -d "Version=$version" -d "IconPath=$repo/desktop/PaykiTodo.Desktop/Assets/PaykiTodo.ico" -d "LicensePath=$license" -o $msi
if ($LASTEXITCODE -ne 0) { throw 'MSI build failed' }
$manifest | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath ($msi+'.files.json') -Encoding utf8
Get-FileHash -LiteralPath $msi -Algorithm SHA256
Write-Output "Payload: $payload"
