import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const VUS = Number(__ENV.VUS || 50);
const DURATION = __ENV.DURATION || '3m';

export const options = {
  scenarios: {
    health_smoke: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<200'],
  },
};

export default function () {
  const res = http.get(`${BASE_URL}/v1/health`);
  check(res, {
    'health status is 200': (r) => r.status === 200,
    'health api code is ok': (r) => {
      try {
        const body = r.json();
        return body.code === 0 || body.message === 'ok' || body.message === 'success';
      } catch (_) {
        return false;
      }
    },
  });
  sleep(1);
}
