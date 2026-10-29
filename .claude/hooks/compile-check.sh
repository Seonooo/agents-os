#!/usr/bin/env bash
# Stop hook: fast compile-only check before Claude declares done.
# Runs: ./gradlew compileJava compileTestJava --console=plain --no-daemon
# - exit 0 + no output on success → stop proceeds
# - exit 0 + JSON {decision:block, reason:...} on failure → Claude is prompted to fix
#
# Why compile-only and not test:
#   Full test = Testcontainers spin-up ~30-60s, too slow for every turn.
#   The /harness slash command is the explicit trigger for full ./gradlew test.

set -uo pipefail

out=$(./gradlew compileJava compileTestJava --console=plain --no-daemon 2>&1)
rc=$?

if [ "$rc" -eq 0 ]; then
  exit 0
fi

python -c "
import json, sys
out = sys.stdin.read()
print(json.dumps({
    'decision': 'block',
    'reason': 'Compile failed before stop — fix import/syntax errors and retry.\n\nGradle output:\n' + out
}))
" <<< "$out"
