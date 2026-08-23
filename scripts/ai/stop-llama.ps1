[CmdletBinding()]
param(
    [string]$ProjectRoot = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}

$serverPath = Join-Path $ProjectRoot "ai-models\llama-server\llama-server.exe"
$pidFile = Join-Path $ProjectRoot ".runtime\llama-server\llama-server.pid"

if (-not (Test-Path -LiteralPath $pidFile -PathType Leaf)) {
    Write-Output "No managed llama-server PID file was found."
    exit 0
}

$managedProcessId = Get-Content -LiteralPath $pidFile
$process = Get-Process -Id $managedProcessId -ErrorAction SilentlyContinue

if (-not $process) {
    Remove-Item -LiteralPath $pidFile -Force
    Write-Output "Removed stale llama-server PID file."
    exit 0
}

$expectedPath = [System.IO.Path]::GetFullPath($serverPath)
$actualPath = [System.IO.Path]::GetFullPath($process.Path)
if (-not $actualPath.Equals(
        $expectedPath,
        [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "PID $managedProcessId does not belong to the workspace llama-server executable."
}

Stop-Process -Id $managedProcessId
Wait-Process -Id $managedProcessId -Timeout 30 -ErrorAction Stop
Remove-Item -LiteralPath $pidFile -Force
Write-Output "Stopped managed llama-server process $managedProcessId."
