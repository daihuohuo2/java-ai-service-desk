$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$envPath=Join-Path $projectRoot '.env'
if(Test-Path -LiteralPath $envPath){Write-Host '.env already exists; preserved.'; exit 0}
$randomBytes=[byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($randomBytes)
$password=[Convert]::ToHexString($randomBytes).ToLowerInvariant()
$content=[System.IO.File]::ReadAllText((Join-Path $projectRoot '.env.example')).Replace('replace-with-random-password',$password)
[System.IO.File]::WriteAllText($envPath,$content,[System.Text.UTF8Encoding]::new($false))
Write-Host '.env created. Credentials were not printed.'
