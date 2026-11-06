import { activeTicks, daysSince, isCold, sumAmounts } from './stall';
import {
  CONTACT_KINDS,
  CONTACT_LABEL,
  activityDate,
  allowedTargets,
  nextStepOverdue,
} from './opportunity';

describe('stall helpers', () => {
  const now = Date.parse('2026-10-27T12:00:00Z');

  it('counts whole days since the last status change', () => {
    expect(daysSince('2026-10-20T11:00:00Z', now)).toBe(7);
    expect(daysSince('2026-10-27T11:00:00Z', now)).toBe(0);
  });

  it('never returns negative days or NaN', () => {
    expect(daysSince('2026-10-30T00:00:00Z', now)).toBe(0);
    expect(daysSince('invalid', now)).toBe(0);
  });

  it('marks cold from 7 days', () => {
    expect(isCold(6)).toBe(false);
    expect(isCold(7)).toBe(true);
  });

  it('loses one tick per day, between 0 and 10', () => {
    expect(activeTicks(0)).toBe(10);
    expect(activeTicks(3)).toBe(7);
    expect(activeTicks(25)).toBe(0);
  });
});

describe('allowedTargets', () => {
  it('mirrors the backend transitions', () => {
    expect(allowedTargets('LEAD')).toEqual(['QUALIFICATION', 'LOST']);
    expect(allowedTargets('NEGOTIATION')).toEqual(['PROPOSAL', 'WON', 'LOST']);
    expect(allowedTargets('WON')).toEqual([]);
  });
});

describe('sumAmounts', () => {
  it('adds the amounts and treats missing values as zero', () => {
    expect(sumAmounts([{ amount: 67000 }, { amount: null }, { amount: 120000 }])).toBe(187000);
  });

  it('is zero for an empty list', () => {
    expect(sumAmounts([])).toBe(0);
  });
});

describe('contact helpers', () => {
  it('uses the last activity when present and falls back to the status change', () => {
    expect(
      activityDate({
        lastActivityAt: '2026-11-05T10:00:00Z',
        statusChangedAt: '2026-10-01T10:00:00Z',
      }),
    ).toBe('2026-11-05T10:00:00Z');
    expect(activityDate({ statusChangedAt: '2026-10-01T10:00:00Z' })).toBe('2026-10-01T10:00:00Z');
  });

  it('marks a next step as overdue only when its date has passed', () => {
    expect(nextStepOverdue('2026-11-05', '2026-11-06')).toBe(true);
    expect(nextStepOverdue('2026-11-06', '2026-11-06')).toBe(false);
    expect(nextStepOverdue(null, '2026-11-06')).toBe(false);
    expect(nextStepOverdue(undefined, '2026-11-06')).toBe(false);
  });

  it('has a Portuguese label for every contact kind', () => {
    for (const kind of CONTACT_KINDS) {
      expect(CONTACT_LABEL[kind].length).toBeGreaterThan(0);
    }
  });
});
