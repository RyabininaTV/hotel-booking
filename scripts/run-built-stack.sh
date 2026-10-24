#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

export JWT_SECRET="${JWT_SECRET:-${SESSION_SECRET:-local-development-only-secret-change-me-2026}}"
export EUREKA_PORT="${EUREKA_PORT:-8761}"
export HOTEL_PORT="${HOTEL_PORT:-8081}"
export BOOKING_PORT="${BOOKING_PORT:-8082}"
export PORT="${PORT:-8080}"

for jar in \
  hotel-booking/discovery-server/target/discovery-server-1.0.0.jar \
  hotel-booking/hotel-service/target/hotel-service-1.0.0.jar \
  hotel-booking/booking-service/target/booking-service-1.0.0.jar \
  hotel-booking/api-gateway/target/api-gateway-1.0.0.jar; do
  if [[ ! -f "$jar" ]]; then
    echo "Missing packaged service: $jar. Run ./hotel-booking/scripts/dev-stack.sh first." >&2
    exit 1
  fi
done

mkdir -p hotel-booking/.logs
pids=()
stop_services() {
  for pid in "${pids[@]:-}"; do kill "$pid" 2>/dev/null || true; done
  for pid in "${pids[@]:-}"; do wait "$pid" 2>/dev/null || true; done
}
trap stop_services EXIT INT TERM

java -jar hotel-booking/discovery-server/target/discovery-server-1.0.0.jar \
  >hotel-booking/.logs/discovery.log 2>&1 &
pids+=("$!")

for _ in $(seq 1 60); do
  if curl -fsS "http://localhost:${EUREKA_PORT}/actuator/health" >/dev/null 2>&1; then break; fi
  sleep 1
done
if ! curl -fsS "http://localhost:${EUREKA_PORT}/actuator/health" >/dev/null 2>&1; then
  echo "Eureka did not become healthy; see hotel-booking/.logs/discovery.log" >&2
  exit 1
fi

java -jar hotel-booking/hotel-service/target/hotel-service-1.0.0.jar \
  >hotel-booking/.logs/hotel-service.log 2>&1 &
pids+=("$!")
java -jar hotel-booking/booking-service/target/booking-service-1.0.0.jar \
  >hotel-booking/.logs/booking-service.log 2>&1 &
pids+=("$!")

wait_for_url() {
  local url="$1"
  local label="$2"
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then return 0; fi
    sleep 1
  done
  echo "$label did not become healthy; inspect hotel-booking/.logs/" >&2
  return 1
}

wait_for_url "http://localhost:${HOTEL_PORT}/actuator/health" "Hotel Service"
wait_for_url "http://localhost:${BOOKING_PORT}/actuator/health" "Booking Service"
wait_for_url "http://localhost:${EUREKA_PORT}/eureka/apps/HOTEL-SERVICE" "Hotel Service registration"
wait_for_url "http://localhost:${EUREKA_PORT}/eureka/apps/BOOKING-SERVICE" "Booking Service registration"
sleep 6

echo "Starting API Gateway on port ${PORT}; service logs are in hotel-booking/.logs/"
java -jar hotel-booking/api-gateway/target/api-gateway-1.0.0.jar
