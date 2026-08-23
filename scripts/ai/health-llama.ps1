[CmdletBinding()]
param(
    [string]$BaseUrl = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = if ($env:LLAMA_HOST -or $env:LLAMA_PORT) {
        $hostAddress = if ($env:LLAMA_HOST) { $env:LLAMA_HOST } else { "127.0.0.1" }
        $port = if ($env:LLAMA_PORT) { $env:LLAMA_PORT } else { "8081" }
        "http://${hostAddress}:$port"
    } elseif ($env:POST_AI_MODERATION_BASE_URL) {
        $env:POST_AI_MODERATION_BASE_URL
    } else {
        "http://127.0.0.1:8081"
    }
}

$healthUrl = $BaseUrl.TrimEnd('/') + "/health"

try {
    $response = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri $healthUrl `
        -TimeoutSec 5
    Write-Output "llama-server is healthy: $($response.StatusCode) $healthUrl"
} catch {
    Write-Error "llama-server is unavailable at $healthUrl. $($_.Exception.Message)"
    exit 1
}
