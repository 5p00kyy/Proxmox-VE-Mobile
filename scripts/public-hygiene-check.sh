#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DOC_AND_WORKFLOW_PATHS=(
  README.md
  CHANGELOG.md
  CONTRIBUTING.md
  SECURITY.md
  docs
  .github
  scripts
)

SOURCE_PATHS=(
  app/src/main
)

TEST_PATHS=(
  app/src/test
  app/src/androidTest
)

home_path_pattern='/ho'"me/[^[:space:]]+"
android_home_pattern='ANDROID_HOME=/ho'"me|ANDROID_SDK_ROOT=/ho"'me'
private_ip_pattern='10[.][0-9]{1,3}[.]|172[.](1[6-9]|2[0-9]|3[0-1])[.]|192[.]168[.]'
secret_assignment_pattern='(password|token|secret|ticket|cookie)[[:space:]]*[:=][[:space:]]*[^[:space:]"]{6,}'
private_principal_pattern='pve[.]local|root@pam|admin@pve'

public_doc_pattern="(${home_path_pattern}|${android_home_pattern}|${private_ip_pattern}|${secret_assignment_pattern})"
if git grep -InE "$public_doc_pattern" -- "${DOC_AND_WORKFLOW_PATHS[@]}"; then
  echo "Public docs or workflow files contain local environment details or inline secret-looking values."
  exit 1
fi

source_local_pattern="(${home_path_pattern}|${android_home_pattern})"
if git grep -InE "$source_local_pattern" -- "${SOURCE_PATHS[@]}"; then
  echo "Application source contains local environment details."
  exit 1
fi

test_fixture_pattern="(${home_path_pattern}|${android_home_pattern}|${private_ip_pattern}|${private_principal_pattern})"
if git grep -InE "$test_fixture_pattern" -- "${TEST_PATHS[@]}"; then
  echo "Checked-in test fixtures contain local environment details or private-looking lab identifiers."
  exit 1
fi

if [[ -n "${EXTRA_PUBLIC_HYGIENE_PATTERN:-}" ]]; then
  if git grep -InE "$EXTRA_PUBLIC_HYGIENE_PATTERN" -- "${DOC_AND_WORKFLOW_PATHS[@]}" "${SOURCE_PATHS[@]}" "${TEST_PATHS[@]}"; then
    echo "Tracked public files matched EXTRA_PUBLIC_HYGIENE_PATTERN."
    exit 1
  fi
fi
