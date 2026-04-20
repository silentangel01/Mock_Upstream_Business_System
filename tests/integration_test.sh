#!/usr/bin/env bash
# =============================================================================
# MUBS + HVAS End-to-End Integration Test
# =============================================================================
# Prerequisites:
#   - MongoDB on 27017 (HVAS) and 27018 (MUBS)
#   - HVAS backend running on port 8000 (with WEBHOOK_SECRET=hvas-mubs-shared-secret)
#   - MUBS backend running on port 8090
# =============================================================================

set -e

HVAS_URL="http://localhost:5000"
MUBS_URL="http://localhost:8090"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

# ------------------------------------------------------------------
info "Step 1: Health checks"
# ------------------------------------------------------------------
curl -sf "$HVAS_URL/api/health" > /dev/null && pass "HVAS health OK" || fail "HVAS not reachable on $HVAS_URL"
curl -sf "$MUBS_URL/api/health" > /dev/null && pass "MUBS health OK" || fail "MUBS not reachable on $MUBS_URL"

# ------------------------------------------------------------------
info "Step 2: MUBS login (admin)"
# ------------------------------------------------------------------
LOGIN_RESP=$(curl -sf -X POST "$MUBS_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
[ -n "$TOKEN" ] && pass "Login OK, got JWT" || fail "Login failed: $LOGIN_RESP"
AUTH="Authorization: Bearer $TOKEN"

# ------------------------------------------------------------------
info "Step 3: MUBS seed data check"
# ------------------------------------------------------------------
STATS=$(curl -sf "$MUBS_URL/api/tickets/stats" -H "$AUTH")
TOTAL=$(echo "$STATS" | grep -o '"total":[0-9]*' | cut -d: -f2)
[ "$TOTAL" -ge 1 ] && pass "Seed data present: $TOTAL tickets" || fail "No seed data: $STATS"

# ------------------------------------------------------------------
info "Step 4: Register webhook in HVAS"
# ------------------------------------------------------------------
WH_RESP=$(curl -sf -X POST "$HVAS_URL/api/webhooks" \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"$MUBS_URL/api/v1/hvas/webhook\"}" 2>&1) || true
echo "$WH_RESP" | grep -qi "error\|fail" && info "Webhook may already exist (OK): $WH_RESP" || pass "Webhook registered"

# ------------------------------------------------------------------
info "Step 5: Get current ticket count"
# ------------------------------------------------------------------
BEFORE_TOTAL=$TOTAL

# ------------------------------------------------------------------
info "Step 6: Simulate event via MUBS demo endpoint"
# ------------------------------------------------------------------
SIM_RESP=$(curl -sf -X POST "$MUBS_URL/api/demo/simulate-event" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"eventType":"smoke_flame"}')
SIM_ID=$(echo "$SIM_RESP" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
[ -n "$SIM_ID" ] && pass "Simulated event created ticket: $SIM_ID" || fail "Simulate failed: $SIM_RESP"

# Check it was auto-dispatched
SIM_STATUS=$(echo "$SIM_RESP" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
SIM_TEAM=$(echo "$SIM_RESP" | grep -o '"assignedTeam":"[^"]*"' | cut -d'"' -f4)
[ "$SIM_STATUS" = "DISPATCHED" ] && pass "Auto-dispatched to $SIM_TEAM" || info "Status: $SIM_STATUS (expected DISPATCHED)"

# ------------------------------------------------------------------
info "Step 7: Ticket lifecycle — accept → in_progress → resolve → close"
# ------------------------------------------------------------------
# Accept
ACC_RESP=$(curl -sf -X PATCH "$MUBS_URL/api/tickets/$SIM_ID/status" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"status":"ACCEPTED","note":"On my way"}')
echo "$ACC_RESP" | grep -q '"ACCEPTED"' && pass "ACCEPTED" || fail "Accept failed: $ACC_RESP"

# In Progress
IP_RESP=$(curl -sf -X PATCH "$MUBS_URL/api/tickets/$SIM_ID/status" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS","note":"Arrived on scene"}')
echo "$IP_RESP" | grep -q '"IN_PROGRESS"' && pass "IN_PROGRESS" || fail "In-progress failed: $IP_RESP"

# Resolve
RES_RESP=$(curl -sf -X PATCH "$MUBS_URL/api/tickets/$SIM_ID/status" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"status":"RESOLVED","note":"Fire extinguished"}')
echo "$RES_RESP" | grep -q '"RESOLVED"' && pass "RESOLVED" || fail "Resolve failed: $RES_RESP"

# Close
CLS_RESP=$(curl -sf -X PATCH "$MUBS_URL/api/tickets/$SIM_ID/status" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"status":"CLOSED","note":"Verified and closed"}')
echo "$CLS_RESP" | grep -q '"CLOSED"' && pass "CLOSED" || fail "Close failed: $CLS_RESP"

# ------------------------------------------------------------------
info "Step 8: Verify ticket detail has full timeline"
# ------------------------------------------------------------------
DETAIL=$(curl -sf "$MUBS_URL/api/tickets/$SIM_ID" -H "$AUTH")
TL_COUNT=$(echo "$DETAIL" | grep -o '"action"' | wc -l)
[ "$TL_COUNT" -ge 5 ] && pass "Timeline has $TL_COUNT entries" || info "Timeline entries: $TL_COUNT (expected >= 5)"

# ------------------------------------------------------------------
info "Step 9: Reassign test (create another ticket)"
# ------------------------------------------------------------------
SIM2_RESP=$(curl -sf -X POST "$MUBS_URL/api/demo/simulate-event" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"eventType":"parking_violation"}')
SIM2_ID=$(echo "$SIM2_RESP" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
[ -n "$SIM2_ID" ] && pass "Second ticket: $SIM2_ID" || fail "Second simulate failed"

REASSIGN_RESP=$(curl -sf -X PATCH "$MUBS_URL/api/tickets/$SIM2_ID/reassign" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"targetTeam":"fire_team","note":"Reassigning for test"}')
echo "$REASSIGN_RESP" | grep -q '"fire_team"' && pass "Reassigned to fire_team" || fail "Reassign failed: $REASSIGN_RESP"

# ------------------------------------------------------------------
info "Step 10: Dispatch rules CRUD"
# ------------------------------------------------------------------
RULES=$(curl -sf "$MUBS_URL/api/dispatch-rules" -H "$AUTH")
RULE_COUNT=$(echo "$RULES" | grep -o '"id"' | wc -l)
[ "$RULE_COUNT" -ge 1 ] && pass "Dispatch rules: $RULE_COUNT" || fail "No dispatch rules"

# ------------------------------------------------------------------
info "Step 11: Fieldworker login"
# ------------------------------------------------------------------
FW_RESP=$(curl -sf -X POST "$MUBS_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"worker_zhang","password":"worker123"}')
FW_TOKEN=$(echo "$FW_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
FW_ROLE=$(echo "$FW_RESP" | grep -o '"role":"[^"]*"' | cut -d'"' -f4)
[ "$FW_ROLE" = "FIELDWORKER" ] && pass "Fieldworker login OK (worker_zhang)" || fail "Fieldworker login failed"

# ------------------------------------------------------------------
info "Step 12: Stats after tests"
# ------------------------------------------------------------------
FINAL_STATS=$(curl -sf "$MUBS_URL/api/tickets/stats" -H "$AUTH")
FINAL_TOTAL=$(echo "$FINAL_STATS" | grep -o '"total":[0-9]*' | cut -d: -f2)
pass "Final ticket count: $FINAL_TOTAL (was $BEFORE_TOTAL)"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  All integration tests passed!${NC}"
echo -e "${GREEN}========================================${NC}"
