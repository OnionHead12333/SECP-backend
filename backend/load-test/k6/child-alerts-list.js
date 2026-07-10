import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import {
  BASE_URL,
  PASSWORD,
  accountForVu,
  apiCodeIsOk,
  createChildAndClaimedElder,
  jsonHeaders,
  jsonValue,
} from './lib/common.js';

const VUS = Number(__ENV.VUS || 20);
const DURATION = __ENV.DURATION || '3m';
const SETUP_ACCOUNTS = Number(__ENV.SETUP_ACCOUNTS || VUS);

export const options = {
  scenarios: {
    child_alerts_list: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const accounts = [];
  for (let i = 0; i < SETUP_ACCOUNTS; i += 1) {
    const account = createChildAndClaimedElder(i + 3000, PASSWORD);
    const createRes = http.post(
      `${BASE_URL}/v1/elder/emergency-alerts`,
      JSON.stringify({
        alertType: 'sos',
        triggerMode: 'button',
        remark: `k6 seed alert ${account.suffix}`,
      }),
      jsonHeaders(account.elderToken)
    );
    const alertId = jsonValue(createRes, 'data.alertId');
    check(createRes, {
      'setup alert create status is 200': (r) => r.status === 200,
      'setup alert create code is 0': apiCodeIsOk,
      'setup alert create returns alertId': () => Boolean(alertId),
    });
    if (!alertId) {
      fail(`setup alert create failed: ${createRes.status} ${createRes.body}`);
    }

    const sendRes = http.post(
      `${BASE_URL}/v1/elder/emergency-alerts/${alertId}/send-now`,
      null,
      jsonHeaders(account.elderToken)
    );
    check(sendRes, {
      'setup alert send status is 200': (r) => r.status === 200,
      'setup alert send code is 0': apiCodeIsOk,
    });
    if (sendRes.status !== 200 || !apiCodeIsOk(sendRes)) {
      fail(`setup alert send failed: ${sendRes.status} ${sendRes.body}`);
    }

    accounts.push({ ...account, alertId });
  }
  return { accounts };
}

export default function (data) {
  const account = accountForVu(data.accounts);
  const res = http.get(
    `${BASE_URL}/v1/child/emergency-alerts?status=sent&page=1&pageSize=20`,
    jsonHeaders(account.childToken)
  );

  check(res, {
    'child alerts list status is 200': (r) => r.status === 200,
    'child alerts list code is 0': apiCodeIsOk,
    'child alerts list returns list': (r) => Array.isArray(jsonValue(r, 'data.list', null)),
    'child alerts list has seeded alert': (r) => {
      const list = jsonValue(r, 'data.list', []);
      return Array.isArray(list) && list.some((item) => item.alertId === account.alertId);
    },
  });

  sleep(1);
}
