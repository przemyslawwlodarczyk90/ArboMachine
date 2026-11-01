#!/usr/bin/env bash
# Buduje LOKALNIE obraz linux/amd64 z jednoznacznym tagiem arbomachine:<wersja-pom>-<commit> i zapisuje archiwum
# do wyslania na serwer (docker load). Rollback = uruchomienie poprzedniego tagu (deploy/README.md).
# Uzycie (Git Bash, Docker Desktop):  bash deploy/build-image.sh
#   OUTPUT_DIR=/sciezka  - gdzie zapisac arbomachine-<tag>.tar.gz (domyslnie deploy/out, poza Gitem)
#   SKIP_TESTS=1         - pomin ./mvnw verify (NIE do wydania)
set -euo pipefail
repository="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repository"
docker info >/dev/null 2>&1 || { echo "Docker nie odpowiada - uruchom Docker Desktop." >&2; exit 1; }

app_version="$(awk '/<artifactId>arbomachine<\/artifactId>/{getline; gsub(/.*<version>|<\/version>.*/,""); print; exit}' pom.xml)"
git_commit="$(git rev-parse --short=12 HEAD)"
if [ -n "$(git status --porcelain -- src frontend pom.xml deploy)" ]; then
  echo "UWAGA: niezacommitowane zmiany w src/frontend/pom/deploy - tag dostanie przyrostek -dirty." >&2
  git_commit="${git_commit}-dirty"
fi
image_tag="arbomachine:${app_version}-${git_commit}"

if [ "${SKIP_TESTS:-0}" != "1" ]; then
  echo "== Testy backendu (./mvnw verify) =="
  ./mvnw -q -B verify
fi

echo "== Budowanie ${image_tag} (linux/amd64) =="
docker build --platform linux/amd64 -f deploy/Dockerfile \
  --build-arg APP_VERSION="$app_version" --build-arg GIT_COMMIT="$git_commit" -t "$image_tag" .

output_dir="${OUTPUT_DIR:-$repository/deploy/out}"
mkdir -p "$output_dir"
archive="$output_dir/arbomachine-${app_version}-${git_commit}.tar.gz"
docker save "$image_tag" | gzip > "$archive"
docker image ls "$image_tag"
ls -lh "$archive"
echo "Gotowe: obraz ${image_tag}, archiwum ${archive}"
