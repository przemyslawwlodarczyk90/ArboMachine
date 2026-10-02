# Build wydania ArboMachine (Windows / PowerShell): testy frontendu i backendu, build frontendu do zasobow backendu,
# jeden plik JAR z aplikacja. Uzycie (z katalogu repozytorium):  powershell -File scripts/build-release.ps1
# Wymaga: Node.js 20+, JDK 25 (JAVA_HOME albo ~/.jdks/openjdk-25.0.2). Szczegoly: docs/WYDANIE.md
$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME) {
    $defaultJdk = Join-Path $HOME '.jdks/openjdk-25.0.2'
    if (Test-Path $defaultJdk) { $env:JAVA_HOME = $defaultJdk } else { throw 'Ustaw JAVA_HOME na JDK 25.' }
}

Write-Host '== Frontend: zaleznosci, testy, build ==' -ForegroundColor Green
Push-Location (Join-Path $repository 'frontend')
try {
    if (-not (Test-Path 'node_modules')) { npm ci; if ($LASTEXITCODE -ne 0) { throw 'npm ci nie powiodlo sie' } }
    npm test; if ($LASTEXITCODE -ne 0) { throw 'Testy frontendu nie przeszly' }
    npm run build; if ($LASTEXITCODE -ne 0) { throw 'Build frontendu nie powiodl sie' }
} finally { Pop-Location }

Write-Host '== Backend: czysty build z testami i JAR ==' -ForegroundColor Green
Push-Location $repository
try {
    # "clean" usuwa stare paczki frontendu z target/classes/static (inaczej trafilyby do JAR).
    & .\mvnw.cmd -q clean verify; if ($LASTEXITCODE -ne 0) { throw 'Testy / build backendu nie przeszly' }
    $jar = Get-ChildItem target -Filter 'arbomachine-*.jar' | Where-Object { $_.Name -notlike '*plain*' } | Select-Object -First 1
    $hash = (Get-FileHash $jar.FullName -Algorithm SHA256).Hash
    Write-Host ''
    Write-Host "Gotowe: $($jar.FullName) ($([math]::Round($jar.Length / 1MB, 1)) MB)" -ForegroundColor Green
    Write-Host "SHA-256: $hash"
    Write-Host "Uruchomienie: java -jar $($jar.Name)  (port: ARBOMACHINE_PORT, baza: patrz docs/WYDANIE.md)"
} finally { Pop-Location }
