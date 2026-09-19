#!/bin/bash
# PASA SessionStart hook — prepares the workspace so tests/checks can run in
# Claude Code on the web. Idempotent and non-interactive.
set -uo pipefail

# Only run in the remote (web) environment.
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

ROOT="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# 1. Node control-plane dependencies (pasa-server).
if [ -f "$ROOT/pasa-server/package.json" ]; then
  echo "[session-start] Installing pasa-server npm dependencies..."
  (cd "$ROOT/pasa-server" && npm install --no-audit --no-fund) \
    && echo "[session-start] pasa-server deps ready." \
    || echo "[session-start] WARN: npm install failed (check network)."
fi

# 2. Android toolchain status (informational).
#    The Android SDK is required to compile/test the app module. We do not
#    auto-install it here (multi-GB, license prompts); report status instead.
if [ -n "${ANDROID_HOME:-}" ] || [ -n "${ANDROID_SDK_ROOT:-}" ] || [ -f "$ROOT/local.properties" ]; then
  echo "[session-start] Android SDK location configured."
else
  echo "[session-start] NOTE: Android SDK not found (ANDROID_HOME unset, no local.properties)."
  echo "[session-start]       :app Gradle tasks (compile/test/assemble) will not run until an SDK is available."
fi

# 3. Confirm a JDK is present for Gradle.
if command -v java >/dev/null 2>&1; then
  echo "[session-start] JDK: $(java -version 2>&1 | head -n1)"
else
  echo "[session-start] NOTE: no 'java' on PATH; Gradle builds need JDK 17+."
fi

exit 0
