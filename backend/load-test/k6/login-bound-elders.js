import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const VUS = Number(__ENV.VUS || 30);
const DURATION = __ENV.DURATION || '5m';
const PASSWORD = __ENV.PASSWORD || 'Test123456';

export const options = {
  scenarios: {
    login_and_read_bound_elders: {
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
  const childPhone = `138${suffix}`;
  const elderPhone = `139${suffix}`;

  const body = JSON.stringify({
    child: {
      name: `Perf Child ${suffix}`,
      nickname: `PerfChild${suffix}`,
      phone: childPhone,
      password: PASSWORD,
    },
    elders: [
      {
        name: `Perf Elder ${suffix}`,
        phone: elderPhone,
        relation: 'child',
      },
    ],
  });

  const res = http.post(`${BASE_URL}/v1/auth/register-child-with-elders`, body, jsonHeaders());
  check(res, {
    'setup child registration status is 200': (r) => r.status === 200,
    'setup child registration code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
  });

  return { childPhone, password: PASSWORD };
}

export default function (data) {
  const loginRes = http.post(
    `${BASE_URL}/v1/auth/login`,
    JSON.stringify({
      username: data.childPhone,
      password: data.password,
    }),
    jsonHeaders()
  );

  const loginOk = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
    'login returns token': (r) => {
      try {
        return Boolean(r.json('data.token'));
      } catch (_) {
        return false;
      }
    },
  });

  if (!loginOk) {
    sleep(1);
    return;
  }

  const token = loginRes.json('data.token');
  const boundRes = http.get(`${BASE_URL}/v1/child/bound-elders`, jsonHeaders(token));

  check(boundRes, {
    'bound elders status is 200': (r) => r.status === 200,
    'bound elders code is 0': (r) => {
      try {
        return r.json('code') === 0;
      } catch (_) {
        return false;
      }
    },
    'bound elders has data': (r) => {
      try {
        const elders = r.json('data');
        return Array.isArray(elders) && elders.length > 0;
      } catch (_) {
        return false;
      }
    },
  });

  sleep(1);
}
