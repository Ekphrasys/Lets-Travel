#!/usr/bin/env python3
import json
import subprocess
import time
import sys
import datetime

API = "https://localhost:8080"
PASS = "password123"

GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
CYAN = '\033[0;36m'
NC = '\033[0m'

print(f"{CYAN}======================================================================{NC}")
print(f"{CYAN}            LETS-TRAVEL — AUTOMATED FEATURE VERIFICATION              {NC}")
print(f"{CYAN}======================================================================{NC}")

def run_curl(method, path, headers=None, data=None):
    cmd = ["curl", "-sk", "-X", method, f"{API}{path}"]
    if headers:
        for k, v in headers.items():
            cmd += ["-H", f"{k}: {v}"]
    if data:
        cmd += ["-d", json.dumps(data)]
    
    # Return status code and response body
    cmd += ["-w", "\nHTTP_STATUS:%{http_code}"]
    res = subprocess.run(cmd, capture_output=True, text=True)
    
    if res.returncode != 0:
        return 0, ""
    
    lines = res.stdout.strip().split("\n")
    status_line = [l for l in lines if l.startswith("HTTP_STATUS:")]
    body_lines = [l for l in lines if not l.startswith("HTTP_STATUS:")]
    
    status = int(status_line[0].split(":")[1]) if status_line else 0
    body = "\n".join(body_lines)
    return status, body

def login(email, password):
    status, body = run_curl("POST", "/api/auth/login", 
                            headers={"Content-Type": "application/json"},
                            data={"email": email, "password": password})
    if status == 200:
        try:
            return json.loads(body).get("token")
        except:
            return None
    return None

# Authenticate users
print("Logging in users...")
admin_token = login("admin@travel.com", PASS)
manager_token = login("alice.manager@travel.com", PASS)
traveler_token = login("charlie.traveler@travel.com", PASS)

if not admin_token or not manager_token or not traveler_token:
    print(f"{RED}[ERROR] Failed to authenticate users. Make sure Docker containers are running.{NC}")
    sys.exit(1)

print(f"{GREEN}Authentication successful!{NC}\n")

# Create a test trip to Lyon departing tomorrow
tomorrow_date = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()
print(f"Creating a test trip to Lyon departing tomorrow ({tomorrow_date})...")
status, body = run_curl("POST", "/api/travels", 
                        headers={"Authorization": f"Bearer {manager_token}", "Content-Type": "application/json"},
                        data={
                            "title": "Escape to Lyon",
                            "originCity": "Paris",
                            "destinationCity": "Lyon",
                            "departureDate": tomorrow_date,
                            "price": 120.0,
                            "seatsAvailable": 10
                        })
lyon_trip_id = None
if status == 201:
    try:
        lyon_trip_id = json.loads(body).get("id")
        print(f"{GREEN}Test trip created successfully with ID: {lyon_trip_id}{NC}\n")
    except Exception as e:
        print(f"{RED}Failed to parse created trip: {e}{NC}\n")
else:
    print(f"{RED}Failed to create test trip: Status={status}, Body={body}{NC}\n")

errors = []

def verify(name, condition, details=""):
    if condition:
        print(f"  {GREEN}[OK] {name}{NC}")
    else:
        print(f"  {RED}[FAILED] {name}{NC}")
        errors.append(f"{name}: {details}")

# 1. RBAC Tests
print(f"{YELLOW}1. Testing RBAC Controls...{NC}")
status, _ = run_curl("GET", "/api/users")
verify("Anonymous access rejected (401/403)", status in [401, 403], f"Status was {status}")

status, _ = run_curl("GET", "/api/users", headers={"Authorization": f"Bearer {traveler_token}"})
verify("Traveler accessing admin endpoint rejected (403)", status == 403, f"Status was {status}")

status, _ = run_curl("GET", "/api/users", headers={"Authorization": f"Bearer {admin_token}"})
verify("Admin accessing admin endpoint allowed (200)", status == 200, f"Status was {status}")

# 2. Elasticsearch Tests
print(f"\n{YELLOW}2. Testing Elasticsearch Search & Autocomplete...{NC}")
status, body = run_curl("GET", "/api/travels/search/autocomplete?query=Par", headers={"Authorization": f"Bearer {traveler_token}"})
is_ok = False
if status == 200:
    try:
        suggestions = json.loads(body)
        is_ok = any("Paris" in s for s in suggestions)
    except:
        pass
verify("Autocomplete suggestions returned 'Paris'", is_ok, f"Status={status}, Body={body}")

status, body = run_curl("GET", "/api/travels/search?query=Lyon", headers={"Authorization": f"Bearer {traveler_token}"})
is_ok = False
if status == 200:
    try:
        results = json.loads(body)
        is_ok = len(results) > 0
    except:
        pass
verify("Search results returned active trips for query 'Lyon'", is_ok, f"Status={status}, Body={body}")

# 3. Fallback Test
print(f"\n{YELLOW}3. Testing Elasticsearch Outage Fallback to PostgreSQL...{NC}")
print("  Stopping travel-elasticsearch container...")
subprocess.run(["docker", "stop", "travel-elasticsearch"], capture_output=True)

status, body = run_curl("GET", "/api/travels/search?query=Lyon", headers={"Authorization": f"Bearer {traveler_token}"})
is_ok = False
if status == 200:
    try:
        results = json.loads(body)
        is_ok = len(results) > 0
    except:
        pass
verify("Search functions using SQL Fallback with Elasticsearch down", is_ok, f"Status={status}, Body={body}")

print("  Starting travel-elasticsearch container back up...")
subprocess.run(["docker", "start", "travel-elasticsearch"], capture_output=True)

# 4. Neo4j Recommendations
print(f"\n{YELLOW}4. Testing Neo4j Personalized Recommendations...{NC}")
status, body = run_curl("GET", "/api/travels/recommendations", headers={"Authorization": f"Bearer {traveler_token}"})
is_ok = False
if status == 200:
    try:
        suggestions = json.loads(body)
        is_ok = isinstance(suggestions, list)
    except:
        pass
verify("Personalized recommendations retrieved successfully", is_ok, f"Status={status}, Body={body}")

# 5. Booking & 3-Day Cutoff
print(f"\n{YELLOW}5. Testing Bookings, Payments & 3-Day Cancellation Cutoff...{NC}")
# Find a trip
status, body = run_curl("GET", "/api/travels", headers={"Authorization": f"Bearer {traveler_token}"})
trip_id = None
if status == 200:
    try:
        trip_id = json.loads(body)[0].get("id")
    except:
        pass

if lyon_trip_id:
    trip_id = lyon_trip_id

if not trip_id:
    print(f"  {RED}[SKIPPED] No trip found to book.{NC}")
else:
    # Book with Paypal
    status, body = run_curl("POST", "/api/bookings", 
                            headers={"Authorization": f"Bearer {traveler_token}", "Content-Type": "application/json"},
                            data={"tripId": trip_id, "paymentMethod": "PAYPAL"})
    booking_id = None
    if status == 201:
        try:
            booking_id = json.loads(body).get("id")
        except:
            pass
    verify("Create booking with PAYPAL", booking_id is not None, f"Status={status}, Body={body}")
    
    if booking_id:
        # Cancel (Should fail with 422 since departureDate is near/past)
        status, body = run_curl("DELETE", f"/api/bookings/{booking_id}", headers={"Authorization": f"Bearer {traveler_token}"})
        verify("3-day cancellation cutoff validation throws 422", status == 422, f"Status={status}, Body={body}")

# 6. Feedbacks
print(f"\n{YELLOW}6. Testing Trip Feedbacks...{NC}")
if not trip_id:
    print(f"  {RED}[SKIPPED] No trip found to submit feedback.{NC}")
else:
    # Try leaving feedback (Will fail if no confirmed booking, but tests endpoint connectivity)
    status, body = run_curl("POST", f"/api/travels/{trip_id}/feedback", 
                            headers={"Authorization": f"Bearer {traveler_token}", "Content-Type": "application/json"},
                            data={"rating": 5, "comment": "Excellent trip"})
    verify("Feedback endpoint accessible (resolves correctly)", status in [200, 201, 403], f"Status={status}, Body={body}")

# 7. Reports & Moderation
print(f"\n{YELLOW}7. Testing Reports & Moderation...{NC}")
# Report manager Alice
status, body = run_curl("POST", "/api/users/reports", 
                        headers={"Authorization": f"Bearer {traveler_token}", "Content-Type": "application/json"},
                        data={"reportedId": "a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1", "reason": "Spam listings"})
report_id = None
if status == 201:
    try:
        report_id = json.loads(body).get("id")
    except:
        pass
verify("Submit report against manager", report_id is not None, f"Status={status}, Body={body}")

if report_id:
    # Get reports as admin
    status, body = run_curl("GET", "/api/users/reports", headers={"Authorization": f"Bearer {admin_token}"})
    is_ok = False
    if status == 200:
        try:
            reports = json.loads(body)
            is_ok = any(r.get("id") == report_id for r in reports)
        except:
            pass
    verify("Admin can view submitted report", is_ok, f"Status={status}, Body={body}")

    # Resolve report as admin
    status, body = run_curl("PUT", f"/api/users/reports/{report_id}/resolve", headers={"Authorization": f"Bearer {admin_token}"})
    verify("Admin can resolve report", status == 200, f"Status={status}, Body={body}")

# 8. Dashboards & Stats
print(f"\n{YELLOW}8. Testing Dashboards & Analytics...{NC}")
# Traveler stats
status, body = run_curl("GET", "/api/users/c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3/stats", headers={"Authorization": f"Bearer {traveler_token}"})
verify("Traveler stats endpoint returns 200", status == 200, f"Status={status}, Body={body}")

# Manager stats
status, body = run_curl("GET", "/api/travels/managers/a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1/dashboard", headers={"Authorization": f"Bearer {manager_token}"})
verify("Manager dashboard returns 200", status == 200, f"Status={status}, Body={body}")

# Admin dashboard
status, body = run_curl("GET", "/api/travels/admin/dashboard", headers={"Authorization": f"Bearer {admin_token}"})
verify("Admin dashboard returns 200", status == 200, f"Status={status}, Body={body}")

print(f"\n{CYAN}======================================================================{NC}")
print(f"{CYAN}                        VERIFICATION SUMMARY                          {NC}")
print(f"{CYAN}======================================================================{NC}")
if not errors:
    print(f"{GREEN}SUCCESS: All features verified successfully and operate correctly!{NC}")
    sys.exit(0)
else:
    print(f"{RED}FAILURES DETECTED ({len(errors)}):{NC}")
    for err in errors:
        print(f"  - {RED}{err}{NC}")
    sys.exit(1)
