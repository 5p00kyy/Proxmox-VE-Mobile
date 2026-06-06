#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

QA_DOC="${1:-docs/beta-smoke-qa.md}"
REQUIRE_COMPLETE="${REQUIRE_COMPLETE:-false}"

if [[ "${1:-}" == "--require-complete" ]]; then
  QA_DOC="${2:-docs/beta-smoke-qa.md}"
  REQUIRE_COMPLETE=true
fi

if [[ ! -f "$QA_DOC" ]]; then
  echo "QA document not found: $QA_DOC"
  exit 1
fi

pending_count="$(grep -cE '^[|].*[|][[:space:]]*Pending[[:space:]]*[|]' "$QA_DOC" || true)"
pass_count="$(grep -cE '^[|].*[|][[:space:]]*Pass[[:space:]]*[|]' "$QA_DOC" || true)"
fail_count="$(grep -cE '^[|].*[|][[:space:]]*Fail[[:space:]]*[|]' "$QA_DOC" || true)"
blocked_count="$(grep -cE '^[|].*[|][[:space:]]*Blocked[[:space:]]*[|]' "$QA_DOC" || true)"

echo "Beta QA status from $QA_DOC"
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
