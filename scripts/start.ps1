param([switch]$ManagedModels, [switch]$Gpu)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & "$PSScriptRoot\init-env.ps1"
    if (!(Test-Path backend/target/service-desk-1.0.0.jar) -or !(Test-Path frontend/dist/index.html)) { throw 'Run scripts/build.ps1 first, or use the delivery package.' }
    $composeArgs = @('compose','-f','compose.yaml')
    if ($ManagedModels) { $composeArgs += @('-f','compose.models.yaml') }
    if ($Gpu) { if (!$ManagedModels) { throw '-Gpu requires -ManagedModels' }; $composeArgs += @('-f','compose.gpu.yaml') }
    if ($ManagedModels) {
        & docker @composeArgs up -d ollama
        if ($LASTEXITCODE -ne 0) { throw 'Model service failed' }
        foreach ($model in @('qwen3:4b-instruct-2507-q4_K_M','qwen3-embedding:0.6b')) {
            & docker @composeArgs exec -T ollama ollama pull $model
            if ($LASTEXITCODE -ne 0) { throw "Failed to pull $model" }
        }
    }
    & docker @composeArgs up -d --build --wait
    if ($LASTEXITCODE -ne 0) { throw 'Deployment failed; inspect docker compose logs' }
    Write-Output 'Open the WEB_PORT from .env (default http://127.0.0.1:19082).'
} finally { Pop-Location }
