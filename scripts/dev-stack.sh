#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

export JWT_SECRET="${JWT_SECRET:-${SESSION_SECRET:-local-development-only-secret-change-me-2026}}"
export EUREKA_PORT="${EUREKA_PORT:-8761}"
export HOTEL_PORT="${HOTEL_PORT:-8081}"
export BOOKING_PORT="${BOOKING_PORT:-8082}"
export PORT="${PORT:-8080}"
export APP_SEED_ENABLED="${APP_SEED_ENABLED:-true}"

echo "Building the hotel booking services..."
mvn -f hotel-booking/pom.xml -DskipTests package
exec bash hotel-booking/scripts/run-built-stack.sh
