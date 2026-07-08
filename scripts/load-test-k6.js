// Business-flow load test: login -> search -> get trip -> book -> cancel.
// Verifies core actions stay under 5s at increasing concurrency.
//
// Install: https://k6.io/docs/get-started/installation/
// Run:     k6 run scripts/load-test-k6.js
// Target:  API_URL=https://localhost:8080 k6 run scripts/load-test-k6.js
//
// Requires the docker-compose stack up and seeded (infrastructure/postgres/init/01-init.sql).

import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.API_URL || 'https://localhost:8080';
const RESPONSE_BUDGET_MS = 5000;

const TEST_USERS = [
  { email: 'charlie.traveler@travel.com', password: 'password123' },
  { email: 'david.traveler@travel.com', password: 'password123' },
];

const TRIP_IDS = [
  '11111111-1111-1111-1111-111111111111',
  '22222222-2222-2222-2222-222222222222',
  '33333333-3333-3333-3333-333333333333',
];

export const options = {
  insecureSkipTLSVerify: true, // self-signed travel.p12 on the gateway
  scenarios: {
    business_flow: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 50 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_duration{name:login}': [`p(95)<${RESPONSE_BUDGET_MS}`],
    'http_req_duration{name:search_trips}': [`p(95)<${RESPONSE_BUDGET_MS}`],
    'http_req_duration{name:get_trip}': [`p(95)<${RESPONSE_BUDGET_MS}`],
    'http_req_duration{name:create_booking}': [`p(95)<${RESPONSE_BUDGET_MS}`],
    'http_req_duration{name:cancel_booking}': [`p(95)<${RESPONSE_BUDGET_MS}`],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const user = TEST_USERS[__VU % TEST_USERS.length];
  const tripId = TRIP_IDS[__VU % TRIP_IDS.length];
  let token;

  group('login', () => {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ email: user.email, password: user.password }),
      { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
    );
    check(res, {
      'login succeeded': (r) => r.status === 200,
      'login under budget': (r) => r.timings.duration < RESPONSE_BUDGET_MS,
    });
    if (res.status === 200 && res.body) {
      token = res.json('token');
    }
  });

  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  group('search_trips', () => {
    const res = http.get(`${BASE_URL}/api/travels/search?q=Paris`, { tags: { name: 'search_trips' } });
    check(res, {
      'search succeeded': (r) => r.status === 200,
      'search under budget': (r) => r.timings.duration < RESPONSE_BUDGET_MS,
    });
  });

  group('get_trip', () => {
    const res = http.get(`${BASE_URL}/api/travels/${tripId}`, { tags: { name: 'get_trip' } });
    check(res, {
      'get trip succeeded': (r) => r.status === 200,
      'get trip under budget': (r) => r.timings.duration < RESPONSE_BUDGET_MS,
    });
  });

  let bookingId;
  group('create_booking', () => {
    const res = http.post(
      `${BASE_URL}/api/bookings`,
      JSON.stringify({ tripId, paymentMethod: 'CREDIT_CARD' }),
      { headers: authHeaders, tags: { name: 'create_booking' } }
    );
    // 201 = booked, 400/409 = cleanly rejected (e.g. trip full) - both are a healthy response, not a hang.
    check(res, {
      'booking handled cleanly': (r) => [201, 400, 409].includes(r.status),
      'booking under budget': (r) => r.timings.duration < RESPONSE_BUDGET_MS,
    });
    if (res.status === 201 && res.body) {
      bookingId = res.json('id');
    }
  });

  if (bookingId) {
    group('cancel_booking', () => {
      const res = http.del(`${BASE_URL}/api/bookings/${bookingId}`, null, {
        headers: authHeaders,
        tags: { name: 'cancel_booking' },
      });
      check(res, {
        'cancel succeeded': (r) => r.status === 200,
        'cancel under budget': (r) => r.timings.duration < RESPONSE_BUDGET_MS,
      });
    });
  }

  sleep(1);
}
