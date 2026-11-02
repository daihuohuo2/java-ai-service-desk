param([Parameter(Mandatory=$true)][string]$Backup)
$ErrorActionPreference = 'Stop'
$backupPath = (Resolve-Path -LiteralPath $Backup).Path
$container = 'desk-restore-check-' + [Guid]::NewGuid().ToString('N').Substring(0,10)
$dbPassword = [Guid]::NewGuid().ToString('N')
try {
    & docker run -d --name $container --network none -e "POSTGRES_PASSWORD=$dbPassword" -e POSTGRES_DB=desk -e POSTGRES_USER=desk postgres:17.6-alpine | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Isolated restore container failed' }
    $ready=$false
    for ($i=0; $i -lt 30; $i++) { & docker exec $container pg_isready -U desk -d desk *> $null; if ($LASTEXITCODE -eq 0) { $ready=$true; break }; Start-Sleep -Seconds 1 }
    if (!$ready) { throw 'Restore database not ready' }
    & docker cp $backupPath "${container}:/tmp/restore.dump"
    if ($LASTEXITCODE -ne 0) { throw 'Backup copy failed' }
    & docker exec $container pg_restore -U desk -d desk --exit-on-error /tmp/restore.dump
    if ($LASTEXITCODE -ne 0) { throw 'Restore failed' }
    & docker exec $container psql -U desk -d desk -Atc "SELECT 'users='||count(*) FROM app_users UNION ALL SELECT 'tickets='||count(*) FROM tickets UNION ALL SELECT 'messages='||count(*) FROM chat_messages UNION ALL SELECT 'chunks='||count(*) FROM knowledge_chunks;"
    if ($LASTEXITCODE -ne 0) { throw 'Restored data query failed' }
    Write-Output 'PASS: isolated pg_restore completed, constraints restored and queries succeeded.'
} finally {
    # Only the randomly named test container created by this invocation is removed.
    & docker rm -f -v $container *> $null
}
