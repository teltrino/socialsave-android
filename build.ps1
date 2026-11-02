param(
    [ValidateSet('Debug', 'Release')][string]$Configuration = 'Debug',
    [string]$SigningProperties
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$localJava = Join-Path $PSScriptRoot '.tools/java'
if (Test-Path -LiteralPath $localJava) {
    $jdk = Get-ChildItem -LiteralPath $localJava -Directory | Select-Object -First 1
    $env:JAVA_HOME = $jdk.FullName
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.tools/gradle-home'
}
$localSdk = Join-Path $PSScriptRoot '.tools/android-sdk'
if (Test-Path -LiteralPath $localSdk) { $env:ANDROID_HOME = $localSdk }
$gradleArgs = @("assemble$Configuration", 'lintDebug', 'testDebugUnitTest', '--console=plain')
if ($Configuration -eq 'Release') {
    if (-not $SigningProperties) { $SigningProperties = Join-Path $PSScriptRoot '.tools/signing.properties' }
    if (-not (Test-Path -LiteralPath $SigningProperties)) { throw 'Release signing is not configured. See docs/BUILDING.md. Use -Configuration Debug to build without private keys.' }
    $resolvedSigning = (Resolve-Path -LiteralPath $SigningProperties).Path.Replace('\', '/')
    $gradleArgs += "-PsigningProperties=$resolvedSigning"
}
& .\gradlew.bat @gradleArgs
if ($LASTEXITCODE -ne 0) { throw 'Android build or verification failed.' }
$version = [regex]::Match((Get-Content -Raw app/build.gradle.kts), 'versionName\s*=\s*"([^"]+)"').Groups[1].Value
if (-not $version) { throw 'Cannot determine app version.' }
$buildType = $Configuration.ToLowerInvariant()
$suffix = if ($Configuration -eq 'Debug') { '-debug' } else { '' }
New-Item -ItemType Directory -Force -Path 'dist' | Out-Null
$output = "dist/Tellawy-downloader-$version$suffix.apk"
Copy-Item -LiteralPath "app/build/outputs/apk/$buildType/app-$buildType.apk" -Destination $output
Write-Host "Built $output"
