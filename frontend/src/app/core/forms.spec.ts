import { blankToNull } from './forms';

describe('blankToNull', () => {
  it('turns blank values into null', () => {
    expect(blankToNull('')).toBeNull();
    expect(blankToNull('   ')).toBeNull();
    expect(blankToNull(null)).toBeNull();
    expect(blankToNull(undefined)).toBeNull();
  });

  it('trims real values', () => {
    expect(blankToNull('  a@b.com ')).toBe('a@b.com');
  });
});
