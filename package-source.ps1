param([string]$Ref = 'HEAD')
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
& git rev-parse --verify "$Ref^{commit}" *> $null
if ($LASTEXITCODE -ne 0) { throw 'Source packaging requires a committed Git revision. Commit reviewed source files first.' }
$version = [regex]::Match((Get-Content -Raw app/build.gradle.kts), 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
New-Item -ItemType Directory -Force -Path 'dist' | Out-Null
$output = "dist/SocialSave-android-source-$version.zip"
& git archive --format=zip "--output=$output" $Ref
if ($LASTEXITCODE -ne 0) { throw 'Source archive creation failed.' }
Write-Host "Packaged committed source: $output"
