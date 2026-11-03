param([switch]$Check)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$toolDirectory = Join-Path $root '.tools'
$formatter = Join-Path $toolDirectory 'google-java-format.jar'
$expectedHash = '25157797A0A972C2290B5BC71530C4F7AD646458025E3484412A6E5A9B8C9AA6'
if (!(Test-Path -LiteralPath $formatter)) {
    New-Item -ItemType Directory -Path $toolDirectory -Force | Out-Null
    Invoke-WebRequest 'https://github.com/google/google-java-format/releases/download/v1.25.2/google-java-format-1.25.2-all-deps.jar' -OutFile $formatter
}
if ((Get-FileHash -LiteralPath $formatter -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Formatter checksum does not match the pinned version.'
}
$sources = @(Get-ChildItem (Join-Path $root 'backend/desk-*/src') -Filter '*.java' -Recurse | ForEach-Object FullName)
$sources += @(Get-ChildItem (Join-Path $root 'learning') -Filter '*.java' | ForEach-Object FullName)
if ($Check) { & java -jar $formatter --dry-run --set-exit-if-changed @sources }
else { & java -jar $formatter --replace @sources }
if ($LASTEXITCODE -ne 0) { throw 'Java format check failed.' }
