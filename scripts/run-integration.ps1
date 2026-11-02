param([switch]$SkipStart)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$keys = @('TEST_DATABASE_URL','TEST_DATABASE_USER','TEST_DATABASE_PASSWORD','SPRING_PROFILES_ACTIVE','MIDDLEWARE_TESTS','TEST_REDIS_PORT','TEST_RABBITMQ_PORT')
$original = @{}
foreach ($key in $keys) { $original[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
Push-Location $root
try {
    if (!$SkipStart) {
        docker compose -f compose.test-services.yaml up -d --wait
        if ($LASTEXITCODE -ne 0) { throw 'Isolated test services failed to start' }
    }
    $env:TEST_DATABASE_USER='desk_test'
    $env:TEST_DATABASE_PASSWORD='test-only-password'
    $env:TEST_REDIS_PORT='26379'
    $env:TEST_RABBITMQ_PORT='25672'
    $env:SPRING_PROFILES_ACTIVE=''
    $env:TEST_DATABASE_URL='jdbc:postgresql://127.0.0.1:25432/desk_tests'
    $env:MIDDLEWARE_TESTS='true'
    & ./backend/mvnw.cmd -B -ntp -f backend/pom.xml test
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL / Redis / RabbitMQ tests failed' }
    $env:MIDDLEWARE_TESTS='false'
    $env:SPRING_PROFILES_ACTIVE='mysql'
    $env:TEST_DATABASE_URL='jdbc:mysql://127.0.0.1:23306/desk_tests?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8'
    & ./backend/mvnw.cmd -B -ntp -f backend/pom.xml test
    if ($LASTEXITCODE -ne 0) { throw 'MySQL tests failed' }
    Write-Output 'Integration suites passed. Test containers remain available; stop with docker compose -f compose.test-services.yaml stop.'
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $original[$key], 'Process') }
    Pop-Location
}
