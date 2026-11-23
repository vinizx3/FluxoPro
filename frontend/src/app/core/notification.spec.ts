import { AppNotification, extractCount, notificationSummary } from './notification';

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

describe('notificationSummary', () => {
  const base: AppNotification = {
    id: '1',
    type: 'STALLED_OPPORTUNITY',
    opportunityId: 'o1',
    opportunityTitle: 'Proposta',
    customerName: 'Hotel Planalto',
    stalledSince: '2026-11-01T10:00:00Z',
    daysStalled: 5,
    createdAt: '2026-11-06T08:00:00Z',
    readAt: null,
  };

  it('describes a stalled opportunity with the number of days', () => {
    expect(notificationSummary(base)).toBe('Hotel Planalto · parada há 5 dias');
    expect(notificationSummary({ ...base, daysStalled: 1 })).toBe('Hotel Planalto · parada há 1 dia');
  });

  it('describes a won opportunity without mentioning days', () => {
    const won = notificationSummary({ ...base, type: 'OPPORTUNITY_WON', stalledSince: null, daysStalled: 0 });
    expect(won).toBe('Hotel Planalto · negociação ganha');
  });

  it('describes a lost opportunity', () => {
    const lost = notificationSummary({ ...base, type: 'OPPORTUNITY_LOST', stalledSince: null, daysStalled: 0 });
    expect(lost).toBe('Hotel Planalto · negociação perdida');
  });
});
