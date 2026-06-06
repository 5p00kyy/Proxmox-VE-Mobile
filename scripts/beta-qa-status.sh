#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

QA_DOC="${1:-docs/beta-smoke-qa.md}"
REQUIRE_COMPLETE="${REQUIRE_COMPLETE:-false}"
SCOPE="${BETA_QA_STATUS_SCOPE:-release}"

if [[ "${1:-}" == "--require-complete" ]]; then
  QA_DOC="${2:-docs/beta-smoke-qa.md}"
  REQUIRE_COMPLETE=true
elif [[ "${1:-}" == "--all" ]]; then
  QA_DOC="${2:-docs/beta-smoke-qa.md}"
  SCOPE="all"
fi

if [[ ! -f "$QA_DOC" ]]; then
  echo "QA document not found: $QA_DOC"
  exit 1
fi

if [[ "$SCOPE" == "release" ]]; then
  status_source="$(
    awk '
      /^## First Beta Release Readiness$/ { in_section=1; next }
      /^## / && in_section { exit }
      in_section { print }
    ' "$QA_DOC"
  )"
  if [[ -z "$status_source" ]]; then
    echo "Release readiness section not found in $QA_DOC"
    exit 1
  fi
else
  status_source="$(cat "$QA_DOC")"
fi

pending_count="$(grep -cE '^[|].*[|][[:space:]]*Pending[[:space:]]*[|]' <<< "$status_source" || true)"
pass_count="$(grep -cE '^[|].*[|][[:space:]]*Pass[[:space:]]*[|]' <<< "$status_source" || true)"
fail_count="$(grep -cE '^[|].*[|][[:space:]]*Fail[[:space:]]*[|]' <<< "$status_source" || true)"
blocked_count="$(grep -cE '^[|].*[|][[:space:]]*Blocked[[:space:]]*[|]' <<< "$status_source" || true)"

echo "Beta QA status from $QA_DOC"
echo "Scope: $SCOPE"
echo "Pending: $pending_count"
echo "Pass: $pass_count"
echo "Fail: $fail_count"
echo "Blocked: $blocked_count"

if (( fail_count > 0 || blocked_count > 0 )); then
  echo "QA evidence contains failed or blocked release items."
  exit 1
fi

if [[ "$REQUIRE_COMPLETE" == "true" && "$pending_count" -gt 0 ]]; then
  echo "QA evidence still has pending release items."
  exit 1
fi
