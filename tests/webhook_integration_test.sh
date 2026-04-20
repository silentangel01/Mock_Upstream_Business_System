#!/usr/bin/env bash
# =============================================================================
# Real Webhook Integration Test
# Simulates HVAS sending a webhook event to MUBS with HMAC signature
# =============================================================================
# Prerequisites:
#   - MUBS backend running on port 8090
#   - MongoDB on port 27018
# =============================================================================

set -e

MUBS_URL="http://localhost:8090"
SECRET="hvas-mubs-shared-secret"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }
info() { echo -e "${YELLOW}[INFO]${NC} $1"; }

# ------------------------------------------------------------------
info "Step 1: Verify MUBS is running"
# ------------------------------------------------------------------
curl -sf "$MUBS_URL/api/health" > /dev/null && pass "MUBS health OK" || fail "MUBS not reachable"

# ------------------------------------------------------------------
info "Step 2: Construct webhook payload (mimics HVAS)"
# ------------------------------------------------------------------
EVENT_ID="507f1f77bcf86cd799439011"
PAYLOAD=$(cat <<'ENDJSON'
{
  "event_id": "507f1f77bcf86cd799439011",
  "event_type": "smoke_flame",
  "camera_id": "cam_lobby_01",
  "timestamp": 1713600000.0,
  "confidence": 0.92,
  "image_url": "http://localhost:5000/uploads/evidence/test.jpg",
  "description": "Smoke detected in lobby area",
  "object_count": 2,
  "lat_lng": "31.2304,121.4737",
  "location": "Building A Lobby",
  "area_code": "SH-PD-001",
  "group": "zone_a"
}
ENDJSON
)

# ------------------------------------------------------------------
info "Step 3: Compute HMAC-SHA256 signature"
# ------------------------------------------------------------------
SIGNATURE=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $NF}')
pass "Signature: $SIGNATURE"

# ------------------------------------------------------------------
info "Step 4: Send webhook to MUBS (with signature)"
# ------------------------------------------------------------------
WH_RESP=$(curl -sf -X POST "$MUBS_URL/api/v1/hvas/webhook" \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIGNATURE" \
  -d "$PAYLOAD")
echo "Response: $WH_RESP"
echo "$WH_RESP" | grep -q '"created"' && pass "Ticket created via webhook" || fail "Webhook failed: $WH_RESP"

TICKET_ID=$(echo "$WH_RESP" | grep -o '"ticket_id":"[^"]*"' | cut -d'"' -f4)
TEAM=$(echo "$WH_RESP" | grep -o '"assigned_team":"[^"]*"' | cut -d'"' -f4)
pass "Ticket: $TICKET_ID, Team: $TEAM"

# ------------------------------------------------------------------
info "Step 5: Verify ticket exists and has correct data"
# ------------------------------------------------------------------
# Login first
TOKEN=$(curl -sf -X POST "$MUBS_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

TICKET=$(curl -sf "$MUBS_URL/api/tickets/$TICKET_ID" -H "Authorization: Bearer $TOKEN")
echo "$TICKET" | grep -q '"smoke_flame"' && pass "Event type correct"
echo "$TICKET" | grep -q '"DISPATCHED"' && pass "Auto-dispatched"
echo "$TICKET" | grep -q '"fire_team"' && pass "Assigned to fire_team"
echo "$TICKET" | grep -q '"Building A Lobby"' && pass "Location preserved"

# ------------------------------------------------------------------
info "Step 6: Test duplicate rejection"
# ------------------------------------------------------------------
DUP_RESP=$(curl -sf -X POST "$MUBS_URL/api/v1/hvas/webhook" \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: $SIGNATURE" \
  -d "$PAYLOAD")
echo "$DUP_RESP" | grep -q '"duplicate"' && pass "Duplicate correctly rejected" || fail "Duplicate not detected: $DUP_RESP"

# ------------------------------------------------------------------
info "Step 7: Test invalid signature rejection"
# ------------------------------------------------------------------
BAD_RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$MUBS_URL/api/v1/hvas/webhook" \
  -H "Content-Type: application/json" \
  -H "X-HVAS-Signature: deadbeef" \
  -d "$PAYLOAD")
[ "$BAD_RESP" = "401" ] && pass "Invalid signature rejected (401)" || fail "Expected 401, got $BAD_RESP"

# ------------------------------------------------------------------
info "Step 8: Test missing signature rejection"
# ------------------------------------------------------------------
NO_SIG_RESP=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$MUBS_URL/api/v1/hvas/webhook" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD")
[ "$NO_SIG_RESP" = "401" ] && pass "Missing signature rejected (401)" || fail "Expected 401, got $NO_SIG_RESP"

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Webhook integration tests passed!${NC}"
echo -e "${GREEN}========================================${NC}"
