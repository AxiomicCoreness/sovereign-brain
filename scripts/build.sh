#!/usr/bin/env bash
# Build the Sovereign Brain artifact.
#
# Modes:
#   (default)      interactive  — human at the terminal, verbose output on stdout
#   --autonomous   headless build — no prompts, self-terminating, artifact verified
#   --automated    CI build      — strict, structured output, reproducible, fast-fail
#
# autonomous ≠ automated:
#   autonomous — the build drives itself once started (no prompts, no TTY).
#   automated  — the build is driven by a machine (CI, cron); nothing on the
#                invocation site is a human, so exit codes and log format are
#                the entire contract.
set -euo pipefail

# ── Resolve script and repo root ─────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

# ── Mode detection ───────────────────────────────────────────────────
MODE="interactive"
for arg in "$@"; do
    case "${arg}" in
        --autonomous) MODE="autonomous" ;;
        --automated)  MODE="automated"  ;;
        *) ;;
    esac
done

case "${MODE}" in
    autonomous)
        # No TTY, no prompts, no interactive Maven/compiler output.
        MAVEN_ARGS=(-B -ntp)
        exec </dev/null
        ;;
    automated)
        # Strict CI build: batch mode, no transfer progress, fail on warnings
        # where the project tolerates it, structured output on stdout.
        MAVEN_ARGS=(-B -ntp -Dstyle.color=never)
        set -o pipefail
        exec </dev/null
        ;;
    interactive)
        MAVEN_ARGS=()
        ;;
esac

# ── Toolchain checks ────────────────────────────────────────────────
# Fail fast with a legible message in autonomous/automated modes; in
# interactive mode, print the same message and exit with the same code.
require() {
    local cmd="$1" hint="$2"
    if ! command -v "${cmd}" >/dev/null 2>&1; then
        echo "❌ Missing required tool: ${cmd}" >&2
        echo "   ${hint}" >&2
        exit 127
    fi
}
require java  "Install a JDK (17+ recommended): sudo apt install openjdk-17-jdk"
require mvn   "Install Maven: sudo apt install maven  (or use ./mvnw if present)"

# ── Prefer the Maven wrapper if the project ships one ───────────────
if [[ -x "./mvnw" ]]; then
    MVN="./mvnw"
else
    MVN="mvn"
fi

# ── Mode banner (stderr in non-interactive modes) ───────────────────
if [[ "${MODE}" == "interactive" ]]; then
    echo "🔨 Building sovereign-brain (${MODE})..."
else
    echo "🔨 Building sovereign-brain (${MODE})..." >&2
fi

# ── Clean in automated mode for reproducibility ─────────────────────
if [[ "${MODE}" == "automated" ]]; then
    "${MVN}" "${MAVEN_ARGS[@]}" clean
fi

# ── Build ───────────────────────────────────────────────────────────
"${MVN}" "${MAVEN_ARGS[@]}" -DskipTests=false package

# ── Artifact verification ───────────────────────────────────────────
JAR="target/sovereign-brain-1.0.0-SOVEREIGN.jar"
if [[ ! -f "${JAR}" ]]; then
    echo "❌ Expected artifact not produced: ${JAR}" >&2
    exit 1
fi

if [[ "${MODE}" == "interactive" ]]; then
    echo "✅ Build complete: ${JAR}"
else
    echo "✅ Build complete: ${JAR}" >&2
fi

# ── Optional: surface SHA-256 for automated consumers ───────────────
if [[ "${MODE}" == "automated" ]]; then
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "${JAR}"
    fi
fi
