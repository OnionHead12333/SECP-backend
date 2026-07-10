import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import {
  BASE_URL,
  PASSWORD,
  accountForVu,
  apiCodeIsOk,
  createChildAndBoundElder,
  jsonHeaders,
  jsonValue,
} from './lib/common.js';

const VUS = Number(__ENV.VUS || 15);
const DURATION = __ENV.DURATION || '3m';
const SETUP_ACCOUNTS = Number(__ENV.SETUP_ACCOUNTS || VUS);

export const options = {
  scenarios: {
    medical_archive_read: {
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
    const account = createChildAndBoundElder(i + 4000, PASSWORD);
    const folderRes = http.post(
      `${BASE_URL}/v1/medical/folders`,
      JSON.stringify({
        elderProfileId: account.elderProfileId,
        name: `k6-folder-${account.suffix}`,
      }),
      jsonHeaders(account.childToken)
    );
    check(folderRes, {
      'setup medical folder status is 200': (r) => r.status === 200,
      'setup medical folder code is 0': apiCodeIsOk,
      'setup medical folder returns id': (r) => Boolean(jsonValue(r, 'data.id')),
    });
    if (folderRes.status !== 200 || !apiCodeIsOk(folderRes) || !jsonValue(folderRes, 'data.id')) {
      fail(`setup medical folder failed: ${folderRes.status} ${folderRes.body}`);
    }

    accounts.push({
      ...account,
      folderId: jsonValue(folderRes, 'data.id', null),
    });
  }
  return { accounts };
}

export default function (data) {
  const account = accountForVu(data.accounts);

  const foldersRes = http.get(
    `${BASE_URL}/v1/medical/folders?elderProfileId=${account.elderProfileId}`,
    jsonHeaders(account.childToken)
  );
  check(foldersRes, {
    'medical folders status is 200': (r) => r.status === 200,
    'medical folders code is 0': apiCodeIsOk,
    'medical folders returns array': (r) => Array.isArray(jsonValue(r, 'data', null)),
    'medical folders has seeded folder': (r) => {
      const folders = jsonValue(r, 'data', []);
      return Array.isArray(folders) && folders.some((item) => item.id === account.folderId);
    },
  });

  const docsRes = http.get(
    `${BASE_URL}/v1/medical/documents?elderProfileId=${account.elderProfileId}`,
    jsonHeaders(account.childToken)
  );
  check(docsRes, {
    'medical documents status is 200': (r) => r.status === 200,
    'medical documents code is 0': apiCodeIsOk,
    'medical documents returns array': (r) => Array.isArray(jsonValue(r, 'data', null)),
  });

  sleep(1);
}
