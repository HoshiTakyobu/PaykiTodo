[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string]$ServerHost,
    [string]$User = 'azureuser',
    [Parameter(Mandatory = $true)] [string]$IdentityFile,
    [switch]$RestartNginx
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$serverDir = Join-Path $repo 'server'
$target = "$User@$ServerHost"
$sshArgs = @('-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=accept-new', '-i', $IdentityFile)

scp @sshArgs (Join-Path $serverDir 'paykitodo_sync_server.py') "$target`:/tmp/paykitodo_sync_server.py"
scp @sshArgs (Join-Path $serverDir 'paykitodo-sync.service') "$target`:/tmp/paykitodo-sync.service"
scp @sshArgs (Join-Path $serverDir 'panel.conf') "$target`:/tmp/paykitodo-panel.conf"

ssh @sshArgs $target 'set -eu; sudo install -d -o azureuser -g azureuser /home/azureuser/paykitodo-sync; sudo install -d -o azureuser -g azureuser /home/azureuser/.local/share/paykitodo-sync; sudo install -m 0755 /tmp/paykitodo_sync_server.py /home/azureuser/paykitodo-sync/paykitodo_sync_server.py; sudo install -m 0644 /tmp/paykitodo-sync.service /etc/systemd/system/paykitodo-sync.service; sudo install -m 0644 /tmp/paykitodo-panel.conf /etc/nginx/conf.d/panel.conf; sudo systemctl daemon-reload; sudo systemctl enable --now paykitodo-sync.service; sudo nginx -t'
if ($LASTEXITCODE -ne 0) { throw "服务器服务部署失败，退出码 $LASTEXITCODE" }

if ($RestartNginx) {
    ssh @sshArgs $target 'sudo systemctl reload nginx'
    if ($LASTEXITCODE -ne 0) { throw "Nginx 重载失败，退出码 $LASTEXITCODE" }
}

Write-Host "PaykiTodo 加密同步服务已部署到 $target"
Write-Host '手机端服务器地址应填写：例如 https://service.example.invalid/paykitodo-sync'
