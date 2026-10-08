#!/usr/bin/env bash
# Run the Sovereign Brain simulation.
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f target/sovereign-brain-1.0.0-SOVEREIGN.jar ]]; then
    echo "JAR not found, running build first..."
    ./scripts/build.sh
fi

java -jar target/sovereign-brain-1.0.0-SOVEREIGN.jar "$@"
