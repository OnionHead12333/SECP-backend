import http from 'k6/http';
import { check, fail } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
export const PASSWORD = __ENV.PASSWORD || 'Test123456';

export function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

export function apiCodeIsOk(res) {
  try {
    return res.json('code') === 0;
  } catch (_) {
    return false;
  }
}

export function jsonValue(res, path, fallback = undefined) {
  try {
    const value = res.json(path);
    return value === undefined ? fallback : value;
  } catch (_) {
    return fallback;
  }
}

export function phonePair(index) {
  const base = Number(String(Date.now()).slice(-8));
  const tail = String((base + index) % 100000000).padStart(8, '0');
  return {
    childPhone: `138${tail}`,
    elderPhone: `139${tail}`,
    suffix: tail,
  };
}

export function registerChildWithElder(index, password = PASSWORD) {
  const { childPhone, elderPhone, suffix } = phonePair(index);
  const res = http.post(
    `${BASE_URL}/v1/auth/register-child-with-elders`,
    JSON.stringify({
      child: {
        name: `Perf Child ${suffix}`,
        nickname: `PerfChild${suffix}`,
        phone: childPhone,
        password,
      },
      elders: [
        {
          name: `Perf Elder ${suffix}`,
          phone: elderPhone,
          relation: 'child',
        },
      ],
    }),
    jsonHeaders()
  );

  check(res, {
    'setup child registration status is 200': (r) => r.status === 200,
    'setup child registration code is 0': apiCodeIsOk,
  });

  if (res.status !== 200 || !apiCodeIsOk(res)) {
    fail(`setup child registration failed: ${res.status} ${res.body}`);
  }

  return { childPhone, elderPhone, suffix, password };
}

export function registerElderUser(elderPhone, suffix, password = PASSWORD) {
  const username = `elder_perf_${suffix}`;
  const res = http.post(
    `${BASE_URL}/v1/auth/register`,
    JSON.stringify({
      username,
      password,
      role: 'elder',
      phone: elderPhone,
      name: `Perf Elder ${suffix}`,
    }),
    jsonHeaders()
  );

  check(res, {
    'setup elder registration status is 200': (r) => r.status === 200,
    'setup elder registration code is 0': apiCodeIsOk,
  });

  if (res.status !== 200 || !apiCodeIsOk(res)) {
    fail(`setup elder registration failed: ${res.status} ${res.body}`);
  }

  return { username, userId: jsonValue(res, 'data.userId') };
}

export function login(username, password = PASSWORD, label = 'login') {
  const res = http.post(
    `${BASE_URL}/v1/auth/login`,
    JSON.stringify({ username, password }),
    jsonHeaders()
  );

  check(res, {
    [`${label} status is 200`]: (r) => r.status === 200,
    [`${label} code is 0`]: apiCodeIsOk,
    [`${label} returns token`]: (r) => Boolean(jsonValue(r, 'data.token')),
  });

  const token = jsonValue(res, 'data.token');
  if (res.status !== 200 || !apiCodeIsOk(res) || !token) {
    fail(`${label} failed: ${res.status} ${res.body}`);
  }

  return {
    token,
    userId: jsonValue(res, 'data.userId'),
    role: jsonValue(res, 'data.role'),
  };
}

export function firstBoundElder(childToken) {
  const res = http.get(`${BASE_URL}/v1/child/bound-elders`, jsonHeaders(childToken));

  check(res, {
    'setup bound elders status is 200': (r) => r.status === 200,
    'setup bound elders code is 0': apiCodeIsOk,
    'setup bound elders has data': (r) => {
      const elders = jsonValue(r, 'data', []);
      return Array.isArray(elders) && elders.length > 0;
    },
  });

  const elders = jsonValue(res, 'data', []);
  if (res.status !== 200 || !apiCodeIsOk(res) || !Array.isArray(elders) || elders.length === 0) {
    fail(`setup bound elders lookup failed: ${res.status} ${res.body}`);
  }

  return elders[0];
}

export function createChildAndBoundElder(index, password = PASSWORD) {
  const family = registerChildWithElder(index, password);
  const childLogin = login(family.childPhone, password, 'setup child login');
  const elder = firstBoundElder(childLogin.token);
  return {
    ...family,
    childToken: childLogin.token,
    childUserId: childLogin.userId,
    elderProfileId: elder.elderProfileId,
  };
}

export function createChildAndClaimedElder(index, password = PASSWORD) {
  const family = createChildAndBoundElder(index, password);
  const elderUser = registerElderUser(family.elderPhone, family.suffix, password);
  const elderLogin = login(elderUser.username, password, 'setup elder login');
  return {
    ...family,
    elderUsername: elderUser.username,
    elderUserId: elderLogin.userId,
    elderToken: elderLogin.token,
  };
}

export function accountForVu(accounts) {
  return accounts[(__VU - 1) % accounts.length];
}

export function futureIso(minutesFromNow = 5) {
  return new Date(Date.now() + minutesFromNow * 60 * 1000).toISOString();
}
