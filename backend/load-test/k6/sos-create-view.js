import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  BASE_URL,
  PASSWORD,
  accountForVu,
  apiCodeIsOk,
  createChildAndClaimedElder,
  jsonHeaders,
  jsonValue,
} from './lib/common.js';

const VUS = Number(__ENV.VUS || 10);
const DURATION = __ENV.DURATION || '3m';
const SETUP_ACCOUNTS = Number(__ENV.SETUP_ACCOUNTS || VUS);

export const options = {
  scenarios: {
    sos_create_view: {
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

let alertId = null;
let sent = false;

export function setup() {
  const accounts = [];
  for (let i = 0; i < SETUP_ACCOUNTS; i += 1) {
    accounts.push(createChildAndClaimedElder(i + 2000, PASSWORD));
  }
  return { accounts };
}

export default function (data) {
  const account = accountForVu(data.accounts);

  if (!alertId) {
    const createRes = http.post(
      `${BASE_URL}/v1/elder/emergency-alerts`,
      JSON.stringify({
        alertType: 'sos',
        triggerMode: 'button',
        remark: `k6 sos vu ${__VU}`,
      }),
      jsonHeaders(account.elderToken)
    );

    check(createRes, {
      'sos create status is 200': (r) => r.status === 200,
      'sos create code is 0': apiCodeIsOk,
      'sos create returns alertId': (r) => Boolean(jsonValue(r, 'data.alertId')),
      'sos create status is pending': (r) => jsonValue(r, 'data.status') === 'pending_revoke',
    });

    alertId = jsonValue(createRes, 'data.alertId', null);
  }

  if (alertId && !sent) {
    const sendRes = http.post(
      `${BASE_URL}/v1/elder/emergency-alerts/${alertId}/send-now`,
      null,
      jsonHeaders(account.elderToken)
    );

    check(sendRes, {
      'sos send-now status is 200': (r) => r.status === 200,
      'sos send-now code is 0': apiCodeIsOk,
      'sos send-now status is sent': (r) => jsonValue(r, 'data.status') === 'sent',
    });

    sent = apiCodeIsOk(sendRes);
  }

  if (alertId) {
    const elderDetailRes = http.get(
      `${BASE_URL}/v1/elder/emergency-alerts/${alertId}`,
      jsonHeaders(account.elderToken)
    );
    check(elderDetailRes, {
      'sos elder detail status is 200': (r) => r.status === 200,
      'sos elder detail code is 0': apiCodeIsOk,
      'sos elder detail returns alertId': (r) => jsonValue(r, 'data.alertId') === alertId,
    });
  }

  const childListRes = http.get(
    `${BASE_URL}/v1/child/emergency-alerts?status=sent&page=1&pageSize=20`,
    jsonHeaders(account.childToken)
  );
  check(childListRes, {
    'sos child list status is 200': (r) => r.status === 200,
    'sos child list code is 0': apiCodeIsOk,
    'sos child list returns list': (r) => Array.isArray(jsonValue(r, 'data.list', null)),
  });

  sleep(1);
}
