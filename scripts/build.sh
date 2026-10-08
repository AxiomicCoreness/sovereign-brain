#!/usr/bin/env bash
# Build the Sovereign Brain.
set -euo pipefail

cd "$(dirname "$0")/.."
mvn -q clean package
echo "Build complete: target/sovereign-brain-1.0.0-SOVEREIGN.jar"
