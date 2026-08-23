[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [string]$HostAddress = "127.0.0.1",
    [int]$Port = 8081,
    [int]$Threads = 4,
    [int]$ContextSize = 4096,
    [int]$Parallel = 1,
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}

if (-not $PSBoundParameters.ContainsKey("HostAddress") -and $env:LLAMA_HOST) {
    $HostAddress = $env:LLAMA_HOST
}
if (-not $PSBoundParameters.ContainsKey("Port") -and $env:LLAMA_PORT) {
    $Port = [int]$env:LLAMA_PORT
}
if (-not $PSBoundParameters.ContainsKey("Threads") -and $env:LLAMA_THREADS) {
    $Threads = [int]$env:LLAMA_THREADS
}
if (-not $PSBoundParameters.ContainsKey("ContextSize") -and $env:LLAMA_CTX_SIZE) {
    $ContextSize = [int]$env:LLAMA_CTX_SIZE
}
if (-not $PSBoundParameters.ContainsKey("Parallel") -and $env:LLAMA_PARALLEL) {
    $Parallel = [int]$env:LLAMA_PARALLEL
}
if (-not $PSBoundParameters.ContainsKey("StartupTimeoutSeconds") -and $env:LLAMA_STARTUP_TIMEOUT_SECONDS) {
    $StartupTimeoutSeconds = [int]$env:LLAMA_STARTUP_TIMEOUT_SECONDS
}

function Resolve-ProjectPath {
    param(
        [string]$ConfiguredPath,
        [string]$DefaultRelativePath
    )

    $path = if ([string]::IsNullOrWhiteSpace($ConfiguredPath)) {
        Join-Path $ProjectRoot $DefaultRelativePath
    } elseif ([System.IO.Path]::IsPathRooted($ConfiguredPath)) {
        $ConfiguredPath
    } else {
        Join-Path $ProjectRoot $ConfiguredPath
    }

    return [System.IO.Path]::GetFullPath($path)
}

$serverPath = Resolve-ProjectPath $env:LLAMA_SERVER_BIN "ai-models\llama-server\llama-server.exe"
$modelPath = Resolve-ProjectPath $env:LLAMA_MODEL "ai-models\SmolVLM2-2.2B-Instruct-Q4_K_M.gguf"
$mmprojPath = Resolve-ProjectPath $env:LLAMA_MMPROJ "ai-models\mmproj-SmolVLM2-2.2B-Instruct-f16.gguf"
$runtimeDirectory = Join-Path $ProjectRoot ".runtime\llama-server"
$pidFile = Join-Path $runtimeDirectory "llama-server.pid"
$stdoutLog = Join-Path $runtimeDirectory "stdout.log"
$stderrLog = Join-Path $runtimeDirectory "stderr.log"
$baseUrl = "http://${HostAddress}:$Port"
$modelAlias = if ($env:LLAMA_ALIAS) {
    $env:LLAMA_ALIAS
} elseif ($env:POST_AI_MODERATION_MODEL) {
    $env:POST_AI_MODERATION_MODEL
} else {
    "local-aimoderation"
}

function Test-LlamaHealth {
    try {
        $response = Invoke-WebRequest `
            -UseBasicParsing `
            -Uri ($baseUrl + "/health") `
            -TimeoutSec 3
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

if (Test-LlamaHealth) {
    Write-Output "llama-server is already healthy at $baseUrl."
    exit 0
}

foreach ($requiredPath in @($serverPath, $modelPath, $mmprojPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required llama-server file was not found: $requiredPath"
    }
}

if (Test-Path -LiteralPath $pidFile) {
    $staleProcessId = Get-Content -LiteralPath $pidFile -ErrorAction SilentlyContinue
    if ($staleProcessId) {
        $staleProcess = Get-Process -Id $staleProcessId -ErrorAction SilentlyContinue
        if ($staleProcess) {
            throw "Managed llama-server process $staleProcessId is running but health check failed."
        }
    }
    Remove-Item -LiteralPath $pidFile -Force
}

New-Item -ItemType Directory -Force -Path $runtimeDirectory | Out-Null

$quotedModelPath = '"' + $modelPath.Replace('"', '\"') + '"'
$quotedMmprojPath = '"' + $mmprojPath.Replace('"', '\"') + '"'
$argumentLine = @(
    "--model", $quotedModelPath,
    "--mmproj", $quotedMmprojPath,
    "--alias", $modelAlias,
    "--host", $HostAddress,
    "--port", $Port,
    "--threads", $Threads,
    "--threads-batch", $Threads,
    "--parallel", $Parallel,
    "--ctx-size", $ContextSize,
    "--no-webui"
) -join " "

$process = Start-Process `
    -FilePath $serverPath `
    -ArgumentList $argumentLine `
    -WorkingDirectory (Split-Path -Parent $serverPath) `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

Set-Content -LiteralPath $pidFile -Value $process.Id -NoNewline
Write-Output "Starting llama-server with PID $($process.Id)..."

$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 1
    $process.Refresh()

    if ($process.HasExited) {
        Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
        $lastErrors = if (Test-Path -LiteralPath $stderrLog) {
            Get-Content -LiteralPath $stderrLog -Tail 40
        } else {
            @()
        }
        throw "llama-server exited during startup.`n$($lastErrors -join [Environment]::NewLine)"
    }

    if (Test-LlamaHealth) {
        Write-Output "llama-server is healthy at $baseUrl."
        exit 0
    }
}

Stop-Process -Id $process.Id -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
throw "llama-server did not become healthy within $StartupTimeoutSeconds seconds. See $stderrLog."
