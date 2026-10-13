# Run from the extracted name-garden folder before building.
$ErrorActionPreference = "Stop"
$required = @("Dockerfile", "compose.yaml", "docker-entrypoint.sh", "name-suggestion.jar")
$missing = @($required | Where-Object { -not (Test-Path -LiteralPath (Join-Path $PSScriptRoot $_) -PathType Leaf) })
if ($missing.Count -gt 0) {
  Write-Host "Missing Docker build input(s): $($missing -join ', ')" -ForegroundColor Red
  Write-Host "Re-extract the ZIP and run Docker from the extracted name-garden folder."
  exit 1
}
$jar = Get-Item -LiteralPath (Join-Path $PSScriptRoot "name-suggestion.jar")
if ($jar.Length -lt 10000000) {
  Write-Host "JAR looks incomplete: $($jar.Length) bytes" -ForegroundColor Red
  exit 1
}
Write-Host "Docker inputs verified: JAR $([math]::Round($jar.Length / 1MB, 1)) MB" -ForegroundColor Green
Write-Host "cd `"$PSScriptRoot`""
Write-Host "docker compose build --no-cache"
Write-Host "docker compose up -d --force-recreate"
