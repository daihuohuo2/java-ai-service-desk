param([string]$OutputDirectory = '.runtime/backups')
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $backupFile = Join-Path (Resolve-Path $OutputDirectory) ('desk-' + (Get-Date -Format yyyyMMdd-HHmmss) + '.dump')
    & docker compose exec -T db pg_dump -U desk -d desk -Fc -f /tmp/desk-backup.dump
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed' }
    & docker compose cp db:/tmp/desk-backup.dump $backupFile
    if ($LASTEXITCODE -ne 0) { throw 'Backup copy failed' }
    $hash = (Get-FileHash -LiteralPath $backupFile -Algorithm SHA256).Hash
    Set-Content -LiteralPath ($backupFile + '.sha256') -Value $hash -Encoding utf8
    Write-Output $backupFile
} finally { Pop-Location }
