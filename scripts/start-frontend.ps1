$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$env:npm_config_cache = Join-Path $projectRoot '.cache\npm'

Push-Location (Join-Path $projectRoot 'frontend')
try {
    if (-not (Test-Path -LiteralPath 'node_modules')) {
        npm.cmd ci
    }
    npm.cmd run dev
}
finally {
    Pop-Location
}
