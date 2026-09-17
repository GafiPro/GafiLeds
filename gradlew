#!/usr/bin/env sh
set -eu
GRADLE_VERSION="9.6.1"
CACHE_DIR="${HOME}/.gradle/gafi-wrapper/${GRADLE_VERSION}"
DIST_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"
ZIP_PATH="${CACHE_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
if [ ! -x "${DIST_DIR}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  if [ ! -f "${ZIP_PATH}" ]; then
    echo "GafiLeds: Gradle ${GRADLE_VERSION} não encontrado localmente. A descarregar..."
    if command -v curl >/dev/null 2>&1; then curl -fL "${URL}" -o "${ZIP_PATH}";
    elif command -v wget >/dev/null 2>&1; then wget -O "${ZIP_PATH}" "${URL}";
    else echo "Necessário curl ou wget para descarregar Gradle." >&2; exit 1; fi
  fi
  rm -rf "${DIST_DIR}"
  if command -v unzip >/dev/null 2>&1; then unzip -q "${ZIP_PATH}" -d "${CACHE_DIR}";
  else echo "Necessário unzip para extrair Gradle." >&2; exit 1; fi
fi
exec "${DIST_DIR}/bin/gradle" "$@"
