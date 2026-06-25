#!/bin/bash

set -u
set -o pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_EMAIL="${API_EMAIL:-Timothee.Greswell@BuzzwordSolutions.com}"
API_PASSWORD="${API_PASSWORD:-Password123!}"
LOG_DIR="${LOG_DIR:-./api-validation-logs}"
READ_ONLY_ONLY=false
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-20}"
TEST_LOCATION_ID="${TEST_LOCATION_ID:-3}"
TEST_BUILDING_ID="${TEST_BUILDING_ID:-3}"
TEST_ROOM_TYPE_ID="${TEST_ROOM_TYPE_ID:-1}"
TEST_TIME_ZONE_ID="${TEST_TIME_ZONE_ID:-3}"
TEST_ORGANIZER_ID="${TEST_ORGANIZER_ID:-31}"
TEST_EMPLOYEE_ID="${TEST_EMPLOYEE_ID:-14}"
TEST_APPROVER_ID="${TEST_APPROVER_ID:-58}"
TEST_ROOM_ID="${TEST_ROOM_ID:-103}"
TEST_ASSIGNMENT_ID="${TEST_ASSIGNMENT_ID:-1001}"
TEST_ROOM_ASSIGNMENT_ID="${TEST_ROOM_ASSIGNMENT_ID:-2001}"
TEST_VIDEO_RESERVATION_ID="${TEST_VIDEO_RESERVATION_ID:-4001}"

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Validates BuzzMeet API endpoints against a local backend.

Options:
  --base-url URL         API base URL. Default: ${BASE_URL}
  --email EMAIL          Login email. Default: ${API_EMAIL}
  --password PASSWORD    Login password. Default: value from API_PASSWORD or seeded default
  --log-dir DIR          Directory for detailed logs. Default: ${LOG_DIR}
  --read-only-only       Test only login and GET endpoints. By default all documented endpoints are tested.
  --include-planned      Deprecated no-op. All documented endpoints are tested by default.
  -h, --help             Show this help text.

Environment variables:
  BASE_URL, API_EMAIL, API_PASSWORD, LOG_DIR, TIMEOUT_SECONDS
  TEST_LOCATION_ID, TEST_BUILDING_ID, TEST_ROOM_TYPE_ID, TEST_TIME_ZONE_ID
  TEST_ORGANIZER_ID, TEST_EMPLOYEE_ID, TEST_APPROVER_ID
  TEST_ROOM_ID, TEST_ASSIGNMENT_ID, TEST_ROOM_ASSIGNMENT_ID, TEST_VIDEO_RESERVATION_ID

Warning:
  By default this script calls POST, PUT, and DELETE endpoints with sample payloads.
  Run it against a disposable local database or use --read-only-only.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --email)
      API_EMAIL="${2:-}"
      shift 2
      ;;
    --password)
      API_PASSWORD="${2:-}"
      shift 2
      ;;
    --log-dir)
      LOG_DIR="${2:-}"
      shift 2
      ;;
    --read-only-only|--readonly-only)
      READ_ONLY_ONLY=true
      shift
      ;;
    --include-planned)
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

mkdir -p "$LOG_DIR"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
LOG_FILE="${LOG_DIR}/api-validation-${RUN_ID}.log"
SUMMARY_FILE="${LOG_DIR}/api-validation-${RUN_ID}.summary"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

ACCESS_TOKEN=""
TOTAL=0
PASS=0
FAIL=0
SKIP=0

log() {
  printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" | tee -a "$LOG_FILE"
}

mask_sensitive() {
  sed \
    -e 's/\("password"[[:space:]]*:[[:space:]]*"\)[^"]*\(")/\1***\2/g' \
    -e 's/\("accessToken"[[:space:]]*:[[:space:]]*"\)[^"]*\(")/\1***\2/g' \
    -e 's/\(Authorization: Bearer \).*/\1***/g'
}

extract_access_token() {
  local body_file="$1"

  if command -v jq >/dev/null 2>&1; then
    jq -r '.accessToken // empty' "$body_file" 2>/dev/null
    return
  fi

  sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$body_file" | head -n 1
}

status_matches() {
  local actual="$1"
  local expected_csv="$2"
  local expected

  IFS=',' read -ra expected_statuses <<< "$expected_csv"
  for expected in "${expected_statuses[@]}"; do
    [[ "$actual" == "$expected" ]] && return 0
  done

  return 1
}

record_summary() {
  local result="$1"
  local method="$2"
  local path="$3"
  local status="$4"
  local expected="$5"
  local note="$6"

  printf '%-7s %-6s %-55s status=%-4s expected=%-12s %s\n' \
    "$result" "$method" "$path" "$status" "$expected" "$note" | tee -a "$SUMMARY_FILE"
}

request() {
  local name="$1"
  local method="$2"
  local path="$3"
  local expected_statuses="$4"
  local auth_mode="${5:-auth}"
  local body="${6:-}"

  TOTAL=$((TOTAL + 1))

  local safe_name body_file header_file meta_file curl_status http_status elapsed content_type url
  safe_name="$(printf '%s' "$name" | tr -cs 'A-Za-z0-9._-' '_' | sed 's/^_//; s/_$//')"
  body_file="${TMP_DIR}/${safe_name}.body"
  header_file="${TMP_DIR}/${safe_name}.headers"
  meta_file="${TMP_DIR}/${safe_name}.meta"
  url="${BASE_URL}${path}"

  log ""
  log "================================================================================"
  log "Endpoint: ${name}"
  log "Request: ${method} ${url}"
  log "Expected HTTP status: ${expected_statuses}"
  log "Auth mode: ${auth_mode}"

  local curl_args=(
    --silent
    --show-error
    --location
    --max-time "$TIMEOUT_SECONDS"
    --request "$method"
    --dump-header "$header_file"
    --output "$body_file"
    --write-out 'http_status=%{http_code}\ntime_total=%{time_total}\ncontent_type=%{content_type}\n'
  )

  if [[ "$auth_mode" == "auth" ]]; then
    if [[ -z "$ACCESS_TOKEN" ]]; then
      log "Result: SKIP - no access token is available for protected endpoint"
      record_summary "SKIP" "$method" "$path" "n/a" "$expected_statuses" "No access token"
      SKIP=$((SKIP + 1))
      return 0
    fi
    curl_args+=(-H "Authorization: Bearer ${ACCESS_TOKEN}")
  fi

  if [[ -n "$body" ]]; then
    curl_args+=(-H "Content-Type: application/json" --data "$body")
    log "Request body:"
    printf '%s\n' "$body" | mask_sensitive | tee -a "$LOG_FILE" >/dev/null
  fi

  curl "${curl_args[@]}" "$url" > "$meta_file" 2>"${TMP_DIR}/${safe_name}.stderr"
  curl_status=$?

  if [[ -s "${TMP_DIR}/${safe_name}.stderr" ]]; then
    log "curl stderr:"
    sed 's/^/  /' "${TMP_DIR}/${safe_name}.stderr" | tee -a "$LOG_FILE" >/dev/null
  fi

  http_status="$(sed -n 's/^http_status=//p' "$meta_file" | tail -n 1)"
  elapsed="$(sed -n 's/^time_total=//p' "$meta_file" | tail -n 1)"
  content_type="$(sed -n 's/^content_type=//p' "$meta_file" | tail -n 1)"

  log "Response status: ${http_status:-000}"
  log "Response time: ${elapsed:-n/a}s"
  log "Response content type: ${content_type:-n/a}"
  log "Response headers:"
  mask_sensitive < "$header_file" | sed 's/^/  /' | tee -a "$LOG_FILE" >/dev/null
  log "Response body:"
  if [[ -s "$body_file" ]]; then
    mask_sensitive < "$body_file" | sed 's/^/  /' | tee -a "$LOG_FILE" >/dev/null
  else
    log "  <empty>"
  fi

  if [[ "$curl_status" -ne 0 ]]; then
    log "Result: FAIL - curl exited with status ${curl_status}"
    record_summary "FAIL" "$method" "$path" "${http_status:-000}" "$expected_statuses" "curl status ${curl_status}"
    FAIL=$((FAIL + 1))
    return 1
  fi

  if status_matches "$http_status" "$expected_statuses"; then
    log "Result: PASS"
    record_summary "PASS" "$method" "$path" "$http_status" "$expected_statuses" "$name"
    PASS=$((PASS + 1))
    return 0
  fi

  log "Result: FAIL - expected ${expected_statuses}, got ${http_status}"
  record_summary "FAIL" "$method" "$path" "$http_status" "$expected_statuses" "$name"
  FAIL=$((FAIL + 1))
  return 1
}

response_body_file_for() {
  local name="$1"
  local safe_name

  safe_name="$(printf '%s' "$name" | tr -cs 'A-Za-z0-9._-' '_' | sed 's/^_//; s/_$//')"
  printf '%s/%s.body\n' "$TMP_DIR" "$safe_name"
}

login() {
  local login_body login_body_file
  login_body="$(printf '{"email":"%s","password":"%s"}' "$API_EMAIL" "$API_PASSWORD")"
  login_body_file="${TMP_DIR}/login.body"

  request "POST /api/auth/login" "POST" "/api/auth/login" "200" "none" "$login_body" || return 1

  cp "$(response_body_file_for "POST /api/auth/login")" "$login_body_file" 2>/dev/null || true
  if [[ ! -s "$login_body_file" ]]; then
    log "Unable to find login response body for token extraction."
    return 1
  fi

  ACCESS_TOKEN="$(extract_access_token "$login_body_file")"
  if [[ -z "$ACCESS_TOKEN" ]]; then
    log "Login returned HTTP 200 but no accessToken was found in the response body."
    return 1
  fi

  log "Access token captured for protected endpoint checks."
  return 0
}

print_startup() {
  : > "$SUMMARY_FILE"
  log "BuzzMeet API endpoint validation"
  log "Base URL: ${BASE_URL}"
  log "Log file: ${LOG_FILE}"
  log "Summary file: ${SUMMARY_FILE}"
  log "Read-only only: ${READ_ONLY_ONLY}"
  log "Login email: ${API_EMAIL}"
  log "Test IDs: location=${TEST_LOCATION_ID}, building=${TEST_BUILDING_ID}, roomType=${TEST_ROOM_TYPE_ID}, timeZone=${TEST_TIME_ZONE_ID}, organizer=${TEST_ORGANIZER_ID}, employee=${TEST_EMPLOYEE_ID}, approver=${TEST_APPROVER_ID}, room=${TEST_ROOM_ID}, assignment=${TEST_ASSIGNMENT_ID}, roomAssignment=${TEST_ROOM_ASSIGNMENT_ID}, videoReservation=${TEST_VIDEO_RESERVATION_ID}"
}

run_implemented_endpoints() {
  request "GET /api/auth/me" "GET" "/api/auth/me" "200"
  request "GET /api/locations" "GET" "/api/locations" "200"
  request "GET /api/buildings" "GET" "/api/buildings" "200"
  request "GET /api/buildings?locationId=${TEST_LOCATION_ID}" "GET" "/api/buildings?locationId=${TEST_LOCATION_ID}" "200"
  request "GET /api/room-types" "GET" "/api/room-types" "200"
  request "GET /api/time-zones" "GET" "/api/time-zones" "200"
  request "GET /api/employees" "GET" "/api/employees" "200"
  request "GET /api/employees?locationId=${TEST_LOCATION_ID}&title=Manager" "GET" "/api/employees?locationId=${TEST_LOCATION_ID}&title=Manager" "200"
}

run_documented_get_endpoints() {
  log ""
  log "Running remaining documented GET endpoints."

  request "GET /api/rooms" "GET" "/api/rooms" "200"
  request "GET /api/rooms/{roomId}" "GET" "/api/rooms/${TEST_ROOM_ID}" "200"
  request "GET /api/rooms/{roomId}/availability" "GET" "/api/rooms/${TEST_ROOM_ID}/availability?startUtc=2026-07-01T13:00:00Z&endUtc=2026-07-01T14:30:00Z" "200"
  request "GET /api/assignments" "GET" "/api/assignments" "200"
  request "GET /api/assignments/{assignmentId}" "GET" "/api/assignments/${TEST_ASSIGNMENT_ID}" "200"
  request "GET /api/room-assignments" "GET" "/api/room-assignments" "200"
  request "GET /api/assignments/{assignmentId}/room-assignments" "GET" "/api/assignments/${TEST_ASSIGNMENT_ID}/room-assignments" "200"
  request "GET /api/assignments/{assignmentId}/video-reservations" "GET" "/api/assignments/${TEST_ASSIGNMENT_ID}/video-reservations" "200"
  request "GET /api/video-reservations/{videoReservationId}" "GET" "/api/video-reservations/${TEST_VIDEO_RESERVATION_ID}" "200"
  request "GET /api/notifications" "GET" "/api/notifications" "200"
  request "GET /api/audit-logs" "GET" "/api/audit-logs" "200"
}

run_documented_mutating_endpoints() {
  local room_body assignment_body assignment_update_body room_assignment_body room_assignment_update_body
  local video_reservation_body video_reservation_update_body cancel_body override_body

  log ""
  log "Running documented POST, PUT, and DELETE endpoints."
  log "These calls can mutate data when the endpoints are implemented."

  room_body="$(cat <<JSON
{
  "buildingId": ${TEST_BUILDING_ID},
  "roomTypeId": ${TEST_ROOM_TYPE_ID},
  "roomCode": "API-VALIDATION-${RUN_ID}",
  "roomName": "API Validation Room ${RUN_ID}",
  "capacity": 12,
  "floor": 4,
  "isVideoRoom": "Y",
  "dialInInfo": "Validation bridge ext. 9410",
  "status": "ACTIVE",
  "notes": "Created by validate_api_endpoints.sh"
}
JSON
)"

  assignment_body="$(cat <<JSON
{
  "organizerId": ${TEST_ORGANIZER_ID},
  "meetingTitle": "API Validation Assignment ${RUN_ID}",
  "description": "Created by validate_api_endpoints.sh",
  "startUtc": "2026-07-01T13:00:00Z",
  "endUtc": "2026-07-01T14:30:00Z",
  "secondaryTimeZoneId": ${TEST_TIME_ZONE_ID},
  "priority": "HIGH",
  "isRecurring": "N",
  "recurrencePattern": null,
  "participants": [
    {
      "employeeId": ${TEST_EMPLOYEE_ID},
      "status": "ATTENDEE",
      "responsibility": "Validation attendee"
    },
    {
      "employeeId": ${TEST_APPROVER_ID},
      "status": "APPROVER",
      "responsibility": "Validation approver"
    }
  ],
  "roomAssignments": [
    {
      "roomId": ${TEST_ROOM_ID},
      "isPrimaryRoom": "Y"
    }
  ],
  "videoReservations": [
    {
      "meetingAssignmentId": ${TEST_ROOM_ASSIGNMENT_ID},
      "locationId": ${TEST_LOCATION_ID},
      "timeZoneId": ${TEST_TIME_ZONE_ID},
      "videoTitle": "API Validation Bridge ${RUN_ID}",
      "isPrimaryLocation": "Y",
      "isVideoEnabled": "Y",
      "connectionLink": "https://meet.buzzmeet.example/api-validation-${RUN_ID}",
      "dialInInfo": "+1-214-555-0101,,991001#"
    }
  ]
}
JSON
)"

  assignment_update_body="$(cat <<JSON
{
  "meetingTitle": "API Validation Assignment Updated ${RUN_ID}",
  "description": "Updated by validate_api_endpoints.sh",
  "startUtc": "2026-07-01T15:00:00Z",
  "endUtc": "2026-07-01T16:00:00Z",
  "participants": [
    {
      "employeeId": ${TEST_EMPLOYEE_ID},
      "status": "ATTENDEE",
      "responsibility": "Validation attendee"
    }
  ],
  "roomAssignments": [
    {
      "roomId": ${TEST_ROOM_ID},
      "isPrimaryRoom": "Y"
    }
  ],
  "videoReservations": [
    {
      "meetingAssignmentId": ${TEST_ROOM_ASSIGNMENT_ID},
      "locationId": ${TEST_LOCATION_ID},
      "timeZoneId": ${TEST_TIME_ZONE_ID},
      "videoTitle": "API Validation Bridge Updated ${RUN_ID}",
      "isPrimaryLocation": "Y",
      "isVideoEnabled": "Y",
      "connectionLink": "https://meet.buzzmeet.example/api-validation-updated-${RUN_ID}",
      "dialInInfo": "+1-214-555-0101,,991002#"
    }
  ]
}
JSON
)"

  room_assignment_body="$(cat <<JSON
{
  "roomId": ${TEST_ROOM_ID},
  "isPrimaryRoom": "N"
}
JSON
)"

  room_assignment_update_body="$(cat <<JSON
{
  "status": "RELEASED"
}
JSON
)"

  video_reservation_body="$(cat <<JSON
{
  "meetingAssignmentId": ${TEST_ROOM_ASSIGNMENT_ID},
  "locationId": ${TEST_LOCATION_ID},
  "timeZoneId": ${TEST_TIME_ZONE_ID},
  "videoTitle": "API Validation Video Reservation ${RUN_ID}",
  "isPrimaryLocation": "N",
  "isVideoEnabled": "Y",
  "connectionLink": "https://meet.buzzmeet.example/api-validation-video-${RUN_ID}",
  "dialInInfo": "+49-30-555-0106,,991002#"
}
JSON
)"

  video_reservation_update_body="$(cat <<JSON
{
  "videoTitle": "API Validation Video Reservation Updated ${RUN_ID}",
  "isPrimaryLocation": "N",
  "isVideoEnabled": "Y",
  "connectionLink": "https://meet.buzzmeet.example/api-validation-video-updated-${RUN_ID}",
  "dialInInfo": "+49-30-555-0106,,991003#",
  "status": "ACTIVE"
}
JSON
)"

  cancel_body="$(cat <<JSON
{
  "reason": "API validation cancellation"
}
JSON
)"

  override_body="$(cat <<JSON
{
  "reason": "API validation override",
  "newStartUtc": "2026-07-01T15:00:00Z",
  "newEndUtc": "2026-07-01T16:00:00Z",
  "newRoomId": ${TEST_ROOM_ID}
}
JSON
)"

  request "POST /api/rooms" "POST" "/api/rooms" "200,201" "auth" "$room_body"
  request "PUT /api/rooms/{roomId}" "PUT" "/api/rooms/${TEST_ROOM_ID}" "200,204" "auth" "$room_body"
  request "DELETE /api/rooms/{roomId}" "DELETE" "/api/rooms/${TEST_ROOM_ID}" "200,204"

  request "POST /api/assignments" "POST" "/api/assignments" "200,201" "auth" "$assignment_body"
  request "PUT /api/assignments/{assignmentId}" "PUT" "/api/assignments/${TEST_ASSIGNMENT_ID}" "200,204" "auth" "$assignment_update_body"

  request "POST /api/assignments/{assignmentId}/room-assignments" "POST" "/api/assignments/${TEST_ASSIGNMENT_ID}/room-assignments" "200,201" "auth" "$room_assignment_body"
  request "PUT /api/room-assignments/{meetingAssignmentId}" "PUT" "/api/room-assignments/${TEST_ROOM_ASSIGNMENT_ID}" "200,204" "auth" "$room_assignment_update_body"
  request "DELETE /api/assignments/{assignmentId}/room-assignments/{meetingAssignmentId}" "DELETE" "/api/assignments/${TEST_ASSIGNMENT_ID}/room-assignments/${TEST_ROOM_ASSIGNMENT_ID}" "200,204"

  request "POST /api/assignments/{assignmentId}/video-reservations" "POST" "/api/assignments/${TEST_ASSIGNMENT_ID}/video-reservations" "200,201" "auth" "$video_reservation_body"
  request "PUT /api/video-reservations/{videoReservationId}" "PUT" "/api/video-reservations/${TEST_VIDEO_RESERVATION_ID}" "200,204" "auth" "$video_reservation_update_body"
  request "DELETE /api/video-reservations/{videoReservationId}" "DELETE" "/api/video-reservations/${TEST_VIDEO_RESERVATION_ID}" "200,204"

  request "POST /api/assignments/{assignmentId}/cancel" "POST" "/api/assignments/${TEST_ASSIGNMENT_ID}/cancel" "200,201,204" "auth" "$cancel_body"
  request "POST /api/assignments/{assignmentId}/override" "POST" "/api/assignments/${TEST_ASSIGNMENT_ID}/override" "200,201,204" "auth" "$override_body"
}

print_final_summary() {
  log ""
  log "================================================================================"
  log "Validation summary"
  log "Total: ${TOTAL}, Passed: ${PASS}, Failed: ${FAIL}, Skipped: ${SKIP}"
  log "Detailed log: ${LOG_FILE}"
  log "Summary: ${SUMMARY_FILE}"
  log ""

  cat "$SUMMARY_FILE"
}

print_startup

if ! command -v curl >/dev/null 2>&1; then
  log "curl is required but was not found in PATH."
  exit 2
fi

login
run_implemented_endpoints
run_documented_get_endpoints

if [[ "$READ_ONLY_ONLY" == "false" ]]; then
  run_documented_mutating_endpoints
else
  log ""
  log "Skipping documented POST, PUT, and DELETE endpoints because --read-only-only was provided."
fi

print_final_summary

if [[ "$FAIL" -gt 0 || "$SKIP" -gt 0 ]]; then
  exit 1
fi

exit 0
