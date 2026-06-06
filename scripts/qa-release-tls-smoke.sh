#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_ARGS="${GRADLE_ARGS:---stacktrace --no-daemon}"
TEST_CLASS="${TEST_CLASS:-com.proxmoxmobile.presentation.QaReleaseTlsSmokeTest}"

./gradlew connectedQaReleaseAndroidTest \
  -Pandroid.testBuildType=qaRelease \
  -Pandroid.testInstrumentationRunnerArguments.class="$TEST_CLASS" \
  $GRADLE_ARGS
