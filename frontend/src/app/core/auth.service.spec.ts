import { parseToken } from './auth.service';

function makeToken(payload: object): string {
  const encode = (o: object) =>
    btoa(JSON.stringify(o)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.sig`;
}

describe('parseToken', () => {
  const now = 1_000_000_000_000;
  const valid = { sub: 'u1', companyId: 'c1', role: 'OWNER', exp: now / 1000 + 3600 };

  it('reads role and company from a valid token', () => {
    expect(parseToken(makeToken(valid), now)).toEqual({
      userId: 'u1',
      companyId: 'c1',
      role: 'OWNER',
      expiresAt: (now / 1000 + 3600) * 1000,
    });
  });

  it('rejects an expired token', () => {
    expect(parseToken(makeToken({ ...valid, exp: now / 1000 - 1 }), now)).toBeNull();
  });

  it('rejects an unknown role', () => {
    expect(parseToken(makeToken({ ...valid, role: 'ADMIN' }), now)).toBeNull();
  });

  it('rejects garbage and null', () => {
    expect(parseToken('abc', now)).toBeNull();
    expect(parseToken(null, now)).toBeNull();
  });
});
