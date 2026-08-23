[CmdletBinding()]
param(
    [string]$ProjectRoot = "",
    [ValidateSet("x64", "arm64")]
    [string]$Architecture = "x64",
    [string]$LlamaCppRef = "latest",
    [switch]$Force,
    [switch]$ForceModels,
    [switch]$SkipStart
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
} else {
    $ProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
}

$modelsDirectory = Join-Path $ProjectRoot "ai-models"
$runtimeDirectory = Join-Path $modelsDirectory "llama-server"
$serverPath = Join-Path $runtimeDirectory "llama-server.exe"
$modelPath = Join-Path $modelsDirectory "SmolVLM2-2.2B-Instruct-Q4_K_M.gguf"
$mmprojPath = Join-Path $modelsDirectory "mmproj-SmolVLM2-2.2B-Instruct-f16.gguf"
$modelUrl = if ($env:LLAMA_MODEL_URL) {
    $env:LLAMA_MODEL_URL
} else {
    "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf?download=true"
}
$mmprojUrl = if ($env:LLAMA_MMPROJ_URL) {
    $env:LLAMA_MMPROJ_URL
} else {
    "https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-2.2B-Instruct-f16.gguf?download=true"
}
$modelSha256 = "0cf76814555b8665149075b74ab6b5c1d428ea1d3d01c1918c12012e8d7c9f58"
$mmprojSha256 = "db9a3a1648cab1ebc3af4a2b0c8145dd8faebf6f7dd7b16e7dc1842229f14ac4"
$modelSize = [int64]1112602656
$mmprojSize = [int64]872303680
$llamaEnvPath = Join-Path $ProjectRoot "llama-server.env"
$appEnvPath = Join-Path $ProjectRoot ".env"
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)

function Set-EnvValue {
    param(
        [string]$Path,
        [string]$Name,
        [string]$Value
    )

    $lines = [System.Collections.Generic.List[string]]::new()
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $lines.AddRange([System.IO.File]::ReadAllLines($Path))
    }
    $pattern = "^\s*" + [regex]::Escape($Name) + "="
    $updated = $false

    for ($index = 0; $index -lt $lines.Count; $index += 1) {
        if ($lines[$index] -match $pattern) {
            $lines[$index] = "$Name=$Value"
            $updated = $true
            break
        }
    }
    if (-not $updated) {
        $lines.Add("$Name=$Value")
    }

    [System.IO.File]::WriteAllLines($Path, $lines, $utf8WithoutBom)
}

function Import-EnvFile {
    param([string]$Path)

    foreach ($line in [System.IO.File]::ReadAllLines($Path)) {
        if ($line -match "^\s*#" -or [string]::IsNullOrWhiteSpace($line)) {
            continue
        }
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            [Environment]::SetEnvironmentVariable(
                $parts[0].Trim(),
                $parts[1].Trim().Trim('"'),
                "Process")
        }
    }
}

function Get-LlamaRelease {
    $headers = @{
        Accept = "application/vnd.github+json"
        "User-Agent" = "minimal-spring-app-ai-setup"
    }
    if ($LlamaCppRef -eq "latest") {
        return Invoke-RestMethod `
            -Headers $headers `
            -Uri "https://api.github.com/repos/ggml-org/llama.cpp/releases/latest"
    }

    return Invoke-RestMethod `
        -Headers $headers `
        -Uri ("https://api.github.com/repos/ggml-org/llama.cpp/releases/tags/" `
                + [uri]::EscapeDataString($LlamaCppRef))
}

function Test-ModelFile {
    param(
        [string]$Path,
        [int64]$ExpectedSize,
        [string]$ExpectedSha256,
        [switch]$VerifyHash
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }
    if ((Get-Item -LiteralPath $Path).Length -ne $ExpectedSize) {
        return $false
    }
    if (-not $VerifyHash) {
        return $true
    }

    return (Get-FileHash -Algorithm SHA256 $Path).Hash -eq $ExpectedSha256
}

function Install-ModelFile {
    param(
        [string]$Name,
        [string]$DestinationPath,
        [string]$Url,
        [int64]$ExpectedSize,
        [string]$ExpectedSha256
    )

    if (-not $ForceModels -and (Test-ModelFile `
            -Path $DestinationPath `
            -ExpectedSize $ExpectedSize `
            -ExpectedSha256 $ExpectedSha256 `
            -VerifyHash)) {
        Write-Output "$Name is already installed."
        return
    }

    $curl = Get-Command "curl.exe" -ErrorAction SilentlyContinue
    if (-not $curl) {
        throw "curl.exe is required to download GGUF models."
    }

    New-Item -ItemType Directory -Force -Path $modelsDirectory | Out-Null
    $temporaryPath = "$DestinationPath.download"
    if ((Test-Path -LiteralPath $temporaryPath -PathType Leaf) `
            -and (Get-Item -LiteralPath $temporaryPath).Length -gt $ExpectedSize) {
        Remove-Item -LiteralPath $temporaryPath -Force
    }

    if (-not (Test-ModelFile `
            -Path $temporaryPath `
            -ExpectedSize $ExpectedSize `
            -ExpectedSha256 $ExpectedSha256 `
            -VerifyHash)) {
        Write-Output "Downloading $Name ($ExpectedSize bytes)..."
        & $curl.Source `
            --location `
            --fail `
            --retry 5 `
            --retry-delay 3 `
            --continue-at - `
            --output $temporaryPath `
            $Url
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to download $Name. curl exited with code $LASTEXITCODE."
        }
    }

    if (-not (Test-ModelFile `
            -Path $temporaryPath `
            -ExpectedSize $ExpectedSize `
            -ExpectedSha256 $ExpectedSha256 `
            -VerifyHash)) {
        throw "Downloaded $Name failed size or SHA-256 verification."
    }

    Move-Item -LiteralPath $temporaryPath -Destination $DestinationPath -Force
    Write-Output "Installed $Name at $DestinationPath."
}

function Install-LlamaRuntime {
    if ((Test-Path -LiteralPath $serverPath -PathType Leaf) -and -not $Force) {
        Write-Output "llama-server.exe is already installed at $serverPath."
        return
    }

    if ($Force) {
        & (Join-Path $PSScriptRoot "stop-llama.ps1") `
            -ProjectRoot $ProjectRoot
    }

    $release = Get-LlamaRelease
    $assetPattern = "(?i)bin-win-cpu-$([regex]::Escape($Architecture))\.zip$"
    $asset = @($release.assets) `
        | Where-Object { $_.name -match $assetPattern } `
        | Select-Object -First 1
    if (-not $asset) {
        throw "No Windows CPU $Architecture asset was found in llama.cpp release $($release.tag_name)."
    }

    $temporaryDirectory = Join-Path `
        ([System.IO.Path]::GetTempPath()) `
        ("minimal-spring-app-llama-" + [guid]::NewGuid())
    $archivePath = Join-Path $temporaryDirectory $asset.name
    $extractDirectory = Join-Path $temporaryDirectory "extracted"

    New-Item -ItemType Directory -Force -Path $temporaryDirectory | Out-Null
    try {
        Write-Output "Downloading llama.cpp $($release.tag_name) for Windows $Architecture..."
        Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $archivePath

        if ($asset.digest -and $asset.digest.StartsWith("sha256:")) {
            $expectedHash = $asset.digest.Substring(7)
            $actualHash = (Get-FileHash -Algorithm SHA256 $archivePath).Hash
            if ($actualHash -ne $expectedHash) {
                throw "Downloaded llama.cpp archive failed SHA-256 verification."
            }
        }

        Expand-Archive -LiteralPath $archivePath -DestinationPath $extractDirectory
        $downloadedServer = Get-ChildItem `
            -LiteralPath $extractDirectory `
            -Filter "llama-server.exe" `
            -File `
            -Recurse `
            | Select-Object -First 1
        if (-not $downloadedServer) {
            throw "The llama.cpp archive does not contain llama-server.exe."
        }

        New-Item -ItemType Directory -Force -Path $runtimeDirectory | Out-Null
        Copy-Item `
            -Path (Join-Path $downloadedServer.Directory.FullName "*") `
            -Destination $runtimeDirectory `
            -Recurse `
            -Force
        Write-Output "Installed llama-server.exe at $serverPath."
    } finally {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force `
            -ErrorAction SilentlyContinue
    }
}

Install-ModelFile `
    -Name "SmolVLM2 Q4_K_M model" `
    -DestinationPath $modelPath `
    -Url $modelUrl `
    -ExpectedSize $modelSize `
    -ExpectedSha256 $modelSha256
Install-ModelFile `
    -Name "SmolVLM2 multimodal projector" `
    -DestinationPath $mmprojPath `
    -Url $mmprojUrl `
    -ExpectedSize $mmprojSize `
    -ExpectedSha256 $mmprojSha256

Install-LlamaRuntime

Set-EnvValue $llamaEnvPath "LLAMA_SERVER_BIN" `
    "ai-models/llama-server/llama-server.exe"
Set-EnvValue $llamaEnvPath "LLAMA_MODEL" `
    "ai-models/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf"
Set-EnvValue $llamaEnvPath "LLAMA_MMPROJ" `
    "ai-models/mmproj-SmolVLM2-2.2B-Instruct-f16.gguf"
Set-EnvValue $llamaEnvPath "LLAMA_ALIAS" "local-aimoderation"
Set-EnvValue $llamaEnvPath "LLAMA_HOST" "127.0.0.1"
Set-EnvValue $llamaEnvPath "LLAMA_PORT" "8081"
Set-EnvValue $llamaEnvPath "LLAMA_THREADS" "4"
Set-EnvValue $llamaEnvPath "LLAMA_CTX_SIZE" "4096"
Set-EnvValue $llamaEnvPath "LLAMA_PARALLEL" "1"
Set-EnvValue $llamaEnvPath "LLAMA_HEALTH_URL" `
    "http://127.0.0.1:8081/health"
Set-EnvValue $llamaEnvPath "LLAMA_STARTUP_TIMEOUT_SECONDS" "180"
Set-EnvValue $appEnvPath "POST_AI_MODERATION_ENABLED" "true"

Write-Output "AI configuration is ready."
if (-not $SkipStart) {
    Import-EnvFile $llamaEnvPath
    & (Join-Path $PSScriptRoot "start-llama.ps1") `
        -ProjectRoot $ProjectRoot
}
