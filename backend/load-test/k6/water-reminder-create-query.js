import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL,
  PASSWORD,
  accountForVu,
  apiCodeIsOk,
  createChildAndBoundElder,
  futureIso,
  jsonHeaders,
  jsonValue,
} from './lib/common.js';

const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '3m';
const SETUP_ACCOUNTS = Number(__ENV.SETUP_ACCOUNTS || VUS);

export const options = {
  scenarios: {
    water_reminder_create_query: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.98'],
  },
};

let reminderId = null;

export function setup() {
  const accounts = [];
  for (let i = 0; i < SETUP_ACCOUNTS; i += 1) {
    accounts.push(createChildAndBoundElder(i + 1000, PASSWORD));
  }
  return { accounts };
}

export default function (data) {
  const account = accountForVu(data.accounts);

  if (!reminderId) {
    const createRes = http.post(
      `${BASE_URL}/v1/child/water-reminders`,
      JSON.stringify({
        elderProfileId: account.elderProfileId,
        title: `k6 water ${account.suffix} vu ${__VU}`,
        dailyTargetMl: 1600,
        intervalMinutes: 120,
        startTime: '08:00:00',
        endTime: '20:00:00',
        remindTime: futureIso(10),
        sourceType: 'manual',
        status: 'active',
        createdBy: 'k6',
      }),
      jsonHeaders(account.childToken)
    );

    check(createRes, {
      'water create status is 200': (r) => r.status === 200,
      'water create code is 0': apiCodeIsOk,
      'water create returns id': (r) => Boolean(jsonValue(r, 'data.id')),
    });

    reminderId = jsonValue(createRes, 'data.id', null);
  }

  const listRes = http.get(
    `${BASE_URL}/v1/child/water-reminders?elderProfileId=${account.elderProfileId}`,
    jsonHeaders(account.childToken)
  );

  check(listRes, {
    'water list status is 200': (r) => r.status === 200,
    'water list code is 0': apiCodeIsOk,
    'water list returns array': (r) => Array.isArray(jsonValue(r, 'data', null)),
    'water list contains reminders': (r) => {
      const items = jsonValue(r, 'data', []);
      return Array.isArray(items) && items.length > 0;
    },
  });

  sleep(1);
}
