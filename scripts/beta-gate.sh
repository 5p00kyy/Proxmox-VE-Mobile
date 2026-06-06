#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BETA_TAG="${1:-${BETA_TAG:-v0.1.0-beta.1}}"
GRADLE_ARGS="${GRADLE_ARGS:---stacktrace --no-daemon}"

if [[ ! "$BETA_TAG" =~ ^v[0-9]+[.][0-9]+[.][0-9]+-beta[.][0-9]+$ ]]; then
  echo "Beta tag must look like v0.1.0-beta.1; got '$BETA_TAG'."
  exit 1
fi

gradle_version="$(
  sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle.kts |
    head -n 1
)"

if [[ -z "$gradle_version" ]]; then
  echo "Could not read versionName from app/build.gradle.kts."
  exit 1
fi

if [[ "$BETA_TAG" != "v$gradle_version" ]]; then
  echo "Beta tag $BETA_TAG does not match app versionName $gradle_version."
  exit 1
fi

git diff --check

./scripts/public-hygiene-check.sh

./gradlew test lint assembleDebug assembleRelease $GRADLE_ARGS
