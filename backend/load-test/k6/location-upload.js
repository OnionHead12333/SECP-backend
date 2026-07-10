import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const VUS = Number(__ENV.VUS || 30);
const DURATION = __ENV.DURATION || '5m';
const PASSWORD = __ENV.PASSWORD || 'Test123456';

export const options = {
  scenarios: {
    elder_location_upload: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
  },
};

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

export function setup() {
  const suffix = String(Date.now()).slice(-8);
  const elderUsername = `elder_perf_${suffix}`;
  const elderPhone = `139${suffix}`;

  const registerRes = http.post(
    `${BASE_URL}/v1/auth/register`,
    JSON.stringify({
      username: elderUsername,
      password: PASSWORD,
      role: 'elder',
      phone: elderPhone,
      name: `Perf Elder ${suffix}`,
    }),
    jsonHeaders()
  );

  check(registerRes, {
    'setup elder registration status is 200': (r) => r.status === 200,
    'setup elder registration code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
  });

  const loginRes = http.post(
    `${BASE_URL}/v1/auth/login`,
    JSON.stringify({
      username: elderUsername,
      password: PASSWORD,
    }),
    jsonHeaders()
  );

  check(loginRes, {
    'setup elder login status is 200': (r) => r.status === 200,
    'setup elder login returns token': (r) => {
      try {
        return Boolean(r.json('data.token'));
      } catch (_) {
        return false;
      }
    },
  });

  const token = loginRes.json('data.token');
  const warmupRes = http.post(
    `${BASE_URL}/v1/elder/location-tracks`,
    JSON.stringify({
      latitude: 39.9042,
      longitude: 116.4074,
      locationType: 'outdoor',
      source: 'gps',
      recordedAt: new Date().toISOString(),
    }),
    jsonHeaders(token)
  );

  check(warmupRes, {
    'setup location warmup status is 200': (r) => r.status === 200,
    'setup location warmup code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
    'setup location warmup returns id': (r) => {
      try {
        return Boolean(r.json('data.locationId'));
      } catch (_) {
        return false;
      }
    },
  });

  return { token };
}

export default function (data) {
  const latOffset = Math.random() / 1000;
  const lngOffset = Math.random() / 1000;
  const body = JSON.stringify({
    latitude: 39.9042 + latOffset,
    longitude: 116.4074 + lngOffset,
    locationType: 'outdoor',
    source: 'gps',
    recordedAt: new Date().toISOString(),
  });

  const res = http.post(`${BASE_URL}/v1/elder/location-tracks`, body, jsonHeaders(data.token));

  check(res, {
    'location upload status is 200': (r) => r.status === 200,
    'location upload code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
    'location upload returns id': (r) => {
      try {
        return Boolean(r.json('data.locationId'));
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
