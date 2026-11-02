param([int]$JavaProcessId = 0)
$ErrorActionPreference = 'Stop'
Write-Output 'Project listening ports:'
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in @(19080,19081,19082,19083,19090,21980,21981) } |
    Select-Object LocalAddress,LocalPort,OwningProcess
if ($JavaProcessId -gt 0) {
    $process = Get-Process -Id $JavaProcessId
    if ($process.ProcessName -notmatch '^java') { throw 'Choose a Java process ID from the project.' }
    & jcmd $JavaProcessId VM.version
    & jcmd $JavaProcessId GC.heap_info
    & jcmd $JavaProcessId Thread.print
}
Write-Output 'No process was stopped and no configuration was changed. Keep diagnostic output local.'
