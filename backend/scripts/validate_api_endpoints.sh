#!/usr/bin/env bash
# validate_api_endpoints.sh — Full BuzzMeet API validation script.
# Covers all 46 endpoints. Mutating tests create resources dynamically,
# capture returned IDs, and clean up after themselves.
#
# Usage:
#   ./validate_api_endpoints.sh                      # read-only
#   ./validate_api_endpoints.sh --mutate             # read + write
#   ./validate_api_endpoints.sh --mutate --base-url http://host:8080

set -Eeuo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_EMAIL="${API_EMAIL:-Timothee.Greswell@BuzzwordSolutions.com}"
API_PASSWORD="${API_PASSWORD:-Password123!}"
LOG_DIR="${LOG_DIR:-./api-validation-logs}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-20}"
READ_ONLY=true

# Pre-seeded read-only fixture IDs — override via environment variables
TEST_LOCATION_ID="${TEST_LOCATION_ID:-3}"
TEST_BUILDING_ID="${TEST_BUILDING_ID:-1}"
TEST_ROOM_ID="${TEST_ROOM_ID:-103}"
TEST_ROOM_TYPE_ID="${TEST_ROOM_TYPE_ID:-1}"
TEST_ASSIGNMENT_ID="${TEST_ASSIGNMENT_ID:-1001}"
TEST_EMPLOYEE_ID="${TEST_EMPLOYEE_ID:-46}"
TEST_VIDEO_RESERVATION_ID="${TEST_VIDEO_RESERVATION_ID:-4001}"

# ── Argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)  BASE_URL="$2";    shift 2 ;;
    --email)     API_EMAIL="$2";   shift 2 ;;
    --password)  API_PASSWORD="$2"; shift 2 ;;
    --log-dir)   LOG_DIR="$2";     shift 2 ;;
    --mutate)    READ_ONLY=false;  shift ;;
    -h|--help)
      cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Options:
  --mutate            Enable write/mutating tests (default: read-only)
  --base-url  URL     API base URL            (default: http://localhost:8080)
  --email     EMAIL   Login e-mail
  --password  PASS    Login password
  --log-dir   DIR     Log output directory    (default: ./api-validation-logs)

Fixture IDs (override via env):
  TEST_LOCATION_ID  TEST_BUILDING_ID  TEST_ROOM_ID  TEST_ROOM_TYPE_ID
  TEST_ASSIGNMENT_ID  TEST_EMPLOYEE_ID  TEST_VIDEO_RESERVATION_ID
EOF
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done

# ── Setup ─────────────────────────────────────────────────────────────────────
mkdir -p "$LOG_DIR"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
SUMMARY_FILE="${LOG_DIR}/api-validation-${RUN_ID}.summary"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

TOTAL=0; PASS=0; FAIL=0; SKIP=0
TOKEN=""
LAST_BODY=""

# ── Helpers ───────────────────────────────────────────────────────────────────
line()    { printf '%s\n' "$*" | tee -a "$SUMMARY_FILE"; }
section() { line ""; line "──── $* ────"; }
contains_status() { [[ ",$2," == *",$1,"* ]]; }

extract_json_field() {
  local file="$1" field="$2"
  if command -v jq >/dev/null 2>&1; then
    jq -r ".${field} // empty" "$file" 2>/dev/null
    return
  fi
  sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "$file" | head -n1
}

extract_json_int() {
  local file="$1" field="$2"
  if command -v jq >/dev/null 2>&1; then
    jq -r ".${field} // empty" "$file" 2>/dev/null
    return
  fi
  sed -n "s/.*\"${field}\"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p" "$file" | head -n1
}

# call_api METHOD PATH EXPECTED_CODES [auth=yes] [body=""] [label=""]
call_api() {
  local method="$1" path="$2" expected="$3"
  local auth="${4:-yes}"
  local body="${5:-}"
  local label="${6:-${method} ${path}}"

  local slug
  slug="$(printf '%s_%s_%s' "$method" "$path" "$RANDOM" | tr -cs 'A-Za-z0-9' '_' | cut -c1-80)"
  local out="${TMP_DIR}/${slug}.json"
  local code="000"

  local -a args=(-sS -m "$TIMEOUT_SECONDS" -X "$method" -o "$out" -w "%{http_code}" "${BASE_URL}${path}")
  [[ "$auth" == "yes" ]] && args=(-H "Authorization: Bearer $TOKEN" "${args[@]}")
  [[ -n "$body" ]]       && args=(-H "Content-Type: application/json" -d "$body" "${args[@]}")

  code="$(curl "${args[@]}" 2>/dev/null || true)"
  TOTAL=$((TOTAL + 1))
  LAST_BODY="$out"

  local verdict
  if contains_status "$code" "$expected"; then
    PASS=$((PASS + 1)); verdict="PASS"
  else
    FAIL=$((FAIL + 1)); verdict="FAIL"
    [[ -s "$out" ]] && line "       Response: $(head -c 300 "$out")"
  fi

  printf "%-4s  %-6s  %-70s  status=%-3s  expected=%-12s  %s\n" \
    "$verdict" "$method" "$path" "$code" "$expected" "$label" \
    | tee -a "$SUMMARY_FILE"
}

# ── Login ─────────────────────────────────────────────────────────────────────
login() {
  section "Authentication — Login"
  call_api POST "/api/auth/login" "200" "no" \
    "{\"email\":\"${API_EMAIL}\",\"password\":\"${API_PASSWORD}\"}" \
    "POST /api/auth/login"
  TOKEN="$(extract_json_field "$LAST_BODY" "accessToken")"
  if [[ -z "$TOKEN" ]]; then
    line "FATAL  Could not extract access token — aborting."
    exit 1
  fi
  line "       Token acquired successfully."
}

# ── Read-only tests ───────────────────────────────────────────────────────────
run_read_only() {

  # Current user profile
  section "Auth — Current User"
  call_api GET "/api/auth/me" "200" "yes" "" "GET /api/auth/me"

  # Lookup / reference data
  section "Lookup — Reference Data"
  call_api GET "/api/locations"                                                 "200" "yes" "" "GET /api/locations"
  call_api GET "/api/buildings"                                                 "200" "yes" "" "GET /api/buildings (all)"
  call_api GET "/api/buildings?locationId=${TEST_LOCATION_ID}"                 "200" "yes" "" "GET /api/buildings?locationId"
  call_api GET "/api/room-types"                                                "200" "yes" "" "GET /api/room-types"
  call_api GET "/api/time-zones"                                                "200" "yes" "" "GET /api/time-zones"
  call_api GET "/api/employees"                                                 "200" "yes" "" "GET /api/employees (all)"
  call_api GET "/api/employees?locationId=${TEST_LOCATION_ID}"                 "200" "yes" "" "GET /api/employees?locationId"
  call_api GET "/api/employees?title=Manager"                                  "200" "yes" "" "GET /api/employees?title"
  call_api GET "/api/employees?locationId=${TEST_LOCATION_ID}&title=Manager"   "200" "yes" "" "GET /api/employees?locationId&title"

  # Rooms — read
  section "Rooms — Read"
  call_api GET "/api/rooms"                                                     "200" "yes" "" "GET /api/rooms (all)"
  call_api GET "/api/rooms?locationId=${TEST_LOCATION_ID}"                     "200" "yes" "" "GET /api/rooms?locationId"
  call_api GET "/api/rooms?buildingId=${TEST_BUILDING_ID}"                     "200" "yes" "" "GET /api/rooms?buildingId"
  call_api GET "/api/rooms?roomTypeId=${TEST_ROOM_TYPE_ID}"                    "200" "yes" "" "GET /api/rooms?roomTypeId"
  call_api GET "/api/rooms?status=ACTIVE"                                      "200" "yes" "" "GET /api/rooms?status"
  call_api GET "/api/rooms?isVideoRoom=true"                                   "200" "yes" "" "GET /api/rooms?isVideoRoom"
  call_api GET "/api/rooms?capacity=10"                                        "200" "yes" "" "GET /api/rooms?capacity"
  call_api GET "/api/rooms/${TEST_ROOM_ID}"                                    "200" "yes" "" "GET /api/rooms/{roomId}"
  call_api GET "/api/rooms/${TEST_ROOM_ID}/availability?startUtc=2026-07-01T13:00:00Z&endUtc=2026-07-01T14:30:00Z" \
    "200" "yes" "" "GET /api/rooms/{roomId}/availability"

  # Assignments — read
  section "Assignments — Read"
  call_api GET "/api/assignments"                                               "200" "yes" "" "GET /api/assignments (all)"
  call_api GET "/api/assignments?organizerId=${TEST_EMPLOYEE_ID}"              "200" "yes" "" "GET /api/assignments?organizerId"
  call_api GET "/api/assignments?participantEmployeeId=${TEST_EMPLOYEE_ID}"    "200" "yes" "" "GET /api/assignments?participantEmployeeId"
  call_api GET "/api/assignments?status=CONFIRMED"                             "200" "yes" "" "GET /api/assignments?status"
  call_api GET "/api/assignments?locationId=${TEST_LOCATION_ID}"               "200" "yes" "" "GET /api/assignments?locationId"
  call_api GET "/api/assignments?roomId=${TEST_ROOM_ID}"                       "200" "yes" "" "GET /api/assignments?roomId"
  call_api GET "/api/assignments?fromUtc=2026-07-01T00:00:00Z&toUtc=2026-07-31T23:59:59Z" \
    "200" "yes" "" "GET /api/assignments?fromUtc&toUtc"
  call_api GET "/api/assignments?priority=NORMAL"                              "200" "yes" "" "GET /api/assignments?priority"
  call_api GET "/api/assignments/${TEST_ASSIGNMENT_ID}"                        "200" "yes" "" "GET /api/assignments/{assignmentId}"

  # Room Assignments — read
  section "Room Assignments — Read"
  call_api GET "/api/room-assignments"                                          "200" "yes" "" "GET /api/room-assignments (all)"
  call_api GET "/api/room-assignments?roomId=${TEST_ROOM_ID}"                  "200" "yes" "" "GET /api/room-assignments?roomId"
  call_api GET "/api/room-assignments?locationId=${TEST_LOCATION_ID}"          "200" "yes" "" "GET /api/room-assignments?locationId"
  call_api GET "/api/room-assignments?status=CONFIRMED"                        "200" "yes" "" "GET /api/room-assignments?status"
  call_api GET "/api/room-assignments?fromUtc=2026-07-01T00:00:00Z&toUtc=2026-07-31T23:59:59Z" \
    "200" "yes" "" "GET /api/room-assignments?fromUtc&toUtc"
  call_api GET "/api/assignments/${TEST_ASSIGNMENT_ID}/room-assignments"       "200" "yes" "" "GET /api/assignments/{id}/room-assignments"

  # Video Reservations — read
  section "Video Reservations — Read"
  call_api GET "/api/assignments/${TEST_ASSIGNMENT_ID}/video-reservations"     "200" "yes" "" "GET /api/assignments/{id}/video-reservations"
  call_api GET "/api/video-reservations/${TEST_VIDEO_RESERVATION_ID}"          "200,404" "yes" "" "GET /api/video-reservations/{videoReservationId}"

  # Participants — read
  section "Participants — Read"
  call_api GET "/api/assignments/${TEST_ASSIGNMENT_ID}/participants"           "200" "yes" "" "GET /api/assignments/{id}/participants"

  # Notifications
  section "Notifications"
  call_api GET "/api/notifications"                                            "200" "yes" "" "GET /api/notifications (all)"
  call_api GET "/api/notifications?employeeId=${TEST_EMPLOYEE_ID}"            "200" "yes" "" "GET /api/notifications?employeeId"

  # Audit Logs
  section "Audit Logs"
  call_api GET "/api/audit-logs"                                               "200,403" "yes" "" "GET /api/audit-logs (all)"
  call_api GET "/api/audit-logs?entityType=ASSIGNMENT"                        "200,403" "yes" "" "GET /api/audit-logs?entityType"
  call_api GET "/api/audit-logs?entityType=ASSIGNMENT&entityId=${TEST_ASSIGNMENT_ID}" \
    "200,403" "yes" "" "GET /api/audit-logs?entityType&entityId"

  # Admin — read
  section "Admin — Read (200 or 403 if insufficient role)"
  call_api GET "/api/admin/users"                                              "200,403" "yes" "" "GET /api/admin/users"
  call_api GET "/api/admin/users?activeOnly=true"                             "200,403" "yes" "" "GET /api/admin/users?activeOnly=true"
  call_api GET "/api/admin/users?roleName=MANAGER"                           "200,403" "yes" "" "GET /api/admin/users?roleName"
  call_api GET "/api/admin/equipment"                                         "200,403" "yes" "" "GET /api/admin/equipment"
  call_api GET "/api/admin/equipment?status=ACTIVE"                          "200,403" "yes" "" "GET /api/admin/equipment?status"
}

# ── Mutating tests ────────────────────────────────────────────────────────────
run_mutating() {
  local signup_email="api.signup.${RUN_ID}@buzzmeet.local"
  local new_user_email="api.user.${RUN_ID}@buzzmeet.local"

  # ── Signup (public endpoint) ────────────────────────────────────────────────
  section "Signup — Public"
  call_api POST "/api/auth/signup" "200,201,409" "no" \
    "{\"firstName\":\"Api\",\"lastName\":\"Tester\",\"email\":\"${signup_email}\",\"password\":\"Password123!\",\"locationId\":${TEST_LOCATION_ID},\"role\":\"EMPLOYEE\",\"title\":\"QA\",\"country\":\"US\",\"city\":\"Dallas\"}" \
    "POST /api/auth/signup"

  # ── Rooms — CRUD ────────────────────────────────────────────────────────────
  section "Rooms — CRUD"

  call_api POST "/api/rooms" "200,201,403" "yes" \
    "{\"name\":\"API-Room-${RUN_ID}\",\"buildingId\":${TEST_BUILDING_ID},\"capacity\":10,\"roomTypeId\":${TEST_ROOM_TYPE_ID},\"status\":\"ACTIVE\",\"isVideoRoom\":false,\"floor\":\"2\"}" \
    "POST /api/rooms (create)"
  local new_room_id
  new_room_id="$(extract_json_int "$LAST_BODY" "roomId")"

  if [[ -n "$new_room_id" ]]; then
    call_api GET "/api/rooms/${new_room_id}" "200" "yes" "" \
      "GET /api/rooms/{new roomId}"
    call_api PUT "/api/rooms/${new_room_id}" "200,204" "yes" \
      "{\"name\":\"API-Room-${RUN_ID}-updated\",\"capacity\":12,\"status\":\"ACTIVE\",\"isVideoRoom\":true}" \
      "PUT /api/rooms/{roomId}"
  else
    line "SKIP   GET/PUT/DELETE /api/rooms/{id} — room creation failed or 403, no ID"
    SKIP=$((SKIP + 3))
  fi

  # ── Assignments — CRUD ──────────────────────────────────────────────────────
  section "Assignments — CRUD"

  call_api POST "/api/assignments" "200,201" "yes" \
    "{\"meetingTitle\":\"API Validation ${RUN_ID}\",\"description\":\"Created by API validator\",\"organizerId\":${TEST_EMPLOYEE_ID},\"locationId\":${TEST_LOCATION_ID},\"startUtc\":\"2027-03-10T10:00:00Z\",\"endUtc\":\"2027-03-10T11:00:00Z\",\"priority\":\"NORMAL\",\"status\":\"PENDING\"}" \
    "POST /api/assignments (create)"
  local new_assignment_id
  new_assignment_id="$(extract_json_int "$LAST_BODY" "assignmentId")"

  if [[ -n "$new_assignment_id" ]]; then
    call_api GET "/api/assignments/${new_assignment_id}" "200" "yes" "" \
      "GET /api/assignments/{new id}"
    call_api PUT "/api/assignments/${new_assignment_id}" "200,204" "yes" \
      "{\"meetingTitle\":\"API Validation ${RUN_ID} (updated)\",\"priority\":\"HIGH\"}" \
      "PUT /api/assignments/{assignmentId}"
  else
    line "SKIP   GET/PUT /api/assignments/{id} — creation failed, no ID"
    SKIP=$((SKIP + 2))
  fi

  local target_assignment="${new_assignment_id:-$TEST_ASSIGNMENT_ID}"

  # ── Room Assignments — CRUD ─────────────────────────────────────────────────
  section "Room Assignments — CRUD"

  call_api POST "/api/assignments/${target_assignment}/room-assignments" "200,201" "yes" \
    "{\"roomId\":${TEST_ROOM_ID},\"startUtc\":\"2027-03-10T10:00:00Z\",\"endUtc\":\"2027-03-10T11:00:00Z\"}" \
    "POST /api/assignments/{id}/room-assignments (book)"
  local new_room_assignment_id
  new_room_assignment_id="$(extract_json_int "$LAST_BODY" "meetingAssignmentId")"

  if [[ -n "$new_room_assignment_id" ]]; then
    call_api PUT "/api/room-assignments/${new_room_assignment_id}" "200,204" "yes" \
      "{\"startUtc\":\"2027-03-10T10:00:00Z\",\"endUtc\":\"2027-03-10T11:30:00Z\"}" \
      "PUT /api/room-assignments/{meetingAssignmentId}"
    call_api DELETE "/api/assignments/${target_assignment}/room-assignments/${new_room_assignment_id}" \
      "200,204" "yes" "" \
      "DELETE /api/assignments/{id}/room-assignments/{meetingAssignmentId}"
  else
    line "SKIP   PUT/DELETE room-assignment — booking failed, no ID"
    SKIP=$((SKIP + 2))
  fi

  # ── Video Reservations — CRUD ───────────────────────────────────────────────
  section "Video Reservations — CRUD"

  call_api POST "/api/assignments/${target_assignment}/video-reservations" "200,201" "yes" \
    "{\"codec\":\"H.264\",\"bandwidth\":\"2M\",\"bridgeAddress\":\"192.168.1.10\",\"startUtc\":\"2027-03-10T10:00:00Z\",\"endUtc\":\"2027-03-10T11:00:00Z\"}" \
    "POST /api/assignments/{id}/video-reservations"
  local new_video_id
  new_video_id="$(extract_json_int "$LAST_BODY" "videoReservationId")"

  if [[ -n "$new_video_id" ]]; then
    call_api GET "/api/video-reservations/${new_video_id}" "200" "yes" "" \
      "GET /api/video-reservations/{new id}"
    call_api PUT "/api/video-reservations/${new_video_id}" "200,204" "yes" \
      "{\"bandwidth\":\"4M\",\"codec\":\"H.265\"}" \
      "PUT /api/video-reservations/{videoReservationId}"
    call_api DELETE "/api/video-reservations/${new_video_id}" "200,204" "yes" "" \
      "DELETE /api/video-reservations/{videoReservationId}"
  else
    line "SKIP   GET/PUT/DELETE video-reservation — creation failed, no ID"
    SKIP=$((SKIP + 3))
  fi

  # ── Participants — Add / Remove ─────────────────────────────────────────────
  section "Participants — Add / Remove"

  call_api POST "/api/assignments/${target_assignment}/participants" "200,201" "yes" \
    "{\"employeeId\":${TEST_EMPLOYEE_ID},\"status\":\"ATTENDEE\",\"responseStatus\":\"PENDING\"}" \
    "POST /api/assignments/{id}/participants (add)"
  local new_participant_id
  new_participant_id="$(extract_json_int "$LAST_BODY" "participantId")"

  if [[ -n "$new_participant_id" ]]; then
    call_api DELETE "/api/assignments/${target_assignment}/participants/${new_participant_id}" \
      "200,204" "yes" "" \
      "DELETE /api/assignments/{id}/participants/{participantId}"
  else
    line "SKIP   DELETE participant — add failed, no ID"
    SKIP=$((SKIP + 1))
  fi

  # ── Cancel, Override, Delete assignment ────────────────────────────────────
  section "Assignment — Cancel / Override / Delete"

  if [[ -n "$new_assignment_id" ]]; then
    call_api POST "/api/assignments/${new_assignment_id}/override" "200,204,403" "yes" \
      "{\"justification\":\"API validation test\"}" \
      "POST /api/assignments/{id}/override"
    call_api POST "/api/assignments/${new_assignment_id}/cancel" "200,204" "yes" \
      "{\"reason\":\"API validation cleanup\"}" \
      "POST /api/assignments/{id}/cancel"
    call_api DELETE "/api/assignments/${new_assignment_id}" "200,204" "yes" "" \
      "DELETE /api/assignments/{assignmentId} (cleanup)"
  else
    line "SKIP   override/cancel/delete assignment — creation failed, no ID"
    SKIP=$((SKIP + 3))
  fi

  # ── Delete the room created above ──────────────────────────────────────────
  section "Rooms — Cleanup"
  if [[ -n "$new_room_id" ]]; then
    call_api DELETE "/api/rooms/${new_room_id}" "200,204" "yes" "" \
      "DELETE /api/rooms/{roomId} (cleanup)"
  fi

  # ── Admin — Users CRUD ──────────────────────────────────────────────────────
  section "Admin — Users CRUD"

  call_api POST "/api/admin/users" "200,201,403" "yes" \
    "{\"firstName\":\"Api\",\"lastName\":\"Validator\",\"title\":\"QA\",\"email\":\"${new_user_email}\",\"country\":\"US\",\"city\":\"Dallas\",\"locationId\":${TEST_LOCATION_ID},\"passwordHash\":\"{noop}Password123!\",\"roleName\":\"EMPLOYEE\"}" \
    "POST /api/admin/users (create)"
  local new_admin_emp_id
  new_admin_emp_id="$(extract_json_int "$LAST_BODY" "employeeId")"

  if [[ -n "$new_admin_emp_id" ]]; then
    call_api PUT "/api/admin/users/${new_admin_emp_id}" "200,204" "yes" \
      "{\"title\":\"Senior QA\"}" \
      "PUT /api/admin/users/{employeeId}"
    call_api POST "/api/admin/users/${new_admin_emp_id}/roles" "200,201,204" "yes" \
      "{\"roleName\":\"ORGANIZER\"}" \
      "POST /api/admin/users/{employeeId}/roles"
    call_api POST "/api/admin/users/${new_admin_emp_id}/deactivate" "200,204" "yes" "" \
      "POST /api/admin/users/{employeeId}/deactivate"
  else
    line "SKIP   admin user update/role/deactivate — creation failed or 403"
    SKIP=$((SKIP + 3))
  fi

  # ── Admin — Equipment CRUD ──────────────────────────────────────────────────
  section "Admin — Equipment CRUD"

  call_api POST "/api/admin/equipment" "200,201,403" "yes" \
    "{\"equipmentName\":\"API-EQUIP-${RUN_ID}\",\"category\":\"Validation\",\"description\":\"Created by API validator\",\"status\":\"ACTIVE\"}" \
    "POST /api/admin/equipment (create)"
  local new_equipment_id
  new_equipment_id="$(extract_json_int "$LAST_BODY" "equipmentId")"

  if [[ -n "$new_equipment_id" ]]; then
    call_api PUT "/api/admin/equipment/${new_equipment_id}" "200,204" "yes" \
      "{\"description\":\"Updated by API validator\"}" \
      "PUT /api/admin/equipment/{equipmentId}"
    call_api POST "/api/admin/equipment/${new_equipment_id}/assign-room" "200,201,204" "yes" \
      "{\"roomId\":${TEST_ROOM_ID},\"quantity\":1,\"notes\":\"validation\"}" \
      "POST /api/admin/equipment/{id}/assign-room"
    call_api POST "/api/admin/equipment/${new_equipment_id}/retire" "200,204" "yes" \
      "{\"reason\":\"API validation cleanup\"}" \
      "POST /api/admin/equipment/{id}/retire"
  else
    line "SKIP   equipment update/assign-room/retire — creation failed or 403"
    SKIP=$((SKIP + 3))
  fi
}

# ── Main ──────────────────────────────────────────────────────────────────────
line "═══════════════════════════════════════════════════════════════════════"
line "BuzzMeet API Validation"
line "Base URL : $BASE_URL"
line "Mode     : $([[ "$READ_ONLY" == "true" ]] && echo "read-only" || echo "read + mutate")"
line "Run ID   : $RUN_ID"
line "Log      : $SUMMARY_FILE"
line "═══════════════════════════════════════════════════════════════════════"

command -v curl >/dev/null 2>&1 || { echo "curl is required" >&2; exit 2; }

login
run_read_only
[[ "$READ_ONLY" == "false" ]] && run_mutating

line ""
line "═══════════════════════════════════════════════════════════════════════"
line "Results:  TOTAL=${TOTAL}  PASS=${PASS}  FAIL=${FAIL}  SKIP=${SKIP}"
line "═══════════════════════════════════════════════════════════════════════"

[[ "$FAIL" -eq 0 ]]
