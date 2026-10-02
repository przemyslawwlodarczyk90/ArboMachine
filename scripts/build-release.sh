#!/usr/bin/env bash
# Build wydania ArboMachine (Linux / macOS / Git Bash): testy frontendu i backendu, build frontendu do zasobow backendu,
# jeden plik JAR z aplikacja. Uzycie (z katalogu repozytorium):  bash scripts/build-release.sh
# Wymaga: Node.js 20+, JDK 25 (JAVA_HOME albo ~/.jdks/openjdk-25.0.2). Szczegoly: docs/WYDANIE.md
set -euo pipefail
repository="$(cd "$(dirname "$0")/.." && pwd)"
if [ -z "${JAVA_HOME:-}" ]; then
  if [ -d "$HOME/.jdks/openjdk-25.0.2" ]; then export JAVA_HOME="$HOME/.jdks/openjdk-25.0.2"; else echo "Ustaw JAVA_HOME na JDK 25." >&2; exit 1; fi
fi

echo "== Frontend: zaleznosci, testy, build =="
cd "$repository/frontend"
[ -d node_modules ] || npm ci
npm test
npm run build

echo "== Backend: czysty build z testami i JAR =="
cd "$repository"
# "clean" usuwa stare paczki frontendu z target/classes/static (inaczej trafilyby do JAR).
./mvnw -q clean verify
jar="$(ls target/arbomachine-*.jar | grep -v plain | head -n 1)"
echo
echo "Gotowe: $repository/$jar ($(du -h "$jar" | cut -f1))"
echo "SHA-256: $(sha256sum "$jar" | cut -d' ' -f1)"
echo "Uruchomienie: java -jar $jar  (port: ARBOMACHINE_PORT, baza: patrz docs/WYDANIE.md)"
