import { extractCount } from './notification';

describe('extractCount', () => {
  it('reads the first numeric field of the response object', () => {
    expect(extractCount({ unread: 3 })).toBe(3);
    expect(extractCount({ count: 0 })).toBe(0);
  });

  it('accepts a bare number', () => {
    expect(extractCount(5)).toBe(5);
  });

  it('falls back to zero', () => {
    expect(extractCount(null)).toBe(0);
    expect(extractCount({ message: 'x' })).toBe(0);
  });
});
