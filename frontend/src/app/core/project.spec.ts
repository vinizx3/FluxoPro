import { allowedProjectTargets, isOverdue, isProjectOpen, todayIso } from './project';

describe('project helpers', () => {
  it('mirrors the backend transitions', () => {
    expect(allowedProjectTargets('PLANNING')).toEqual(['IN_PROGRESS', 'CANCELLED']);
    expect(allowedProjectTargets('IN_PROGRESS')).toEqual(['DELIVERED', 'CANCELLED']);
    expect(allowedProjectTargets('DELIVERED')).toEqual([]);
    expect(allowedProjectTargets('CANCELLED')).toEqual([]);
  });

  it('knows which statuses are open', () => {
    expect(isProjectOpen('PLANNING')).toBe(true);
    expect(isProjectOpen('IN_PROGRESS')).toBe(true);
    expect(isProjectOpen('DELIVERED')).toBe(false);
  });

  it('formats today as yyyy-MM-dd in local time', () => {
    expect(todayIso(new Date(2026, 9, 5))).toBe('2026-10-05');
  });

  it('flags only open projects with a past delivery date as overdue', () => {
    expect(isOverdue('IN_PROGRESS', '2026-10-01', '2026-10-30')).toBe(true);
    expect(isOverdue('IN_PROGRESS', '2026-10-30', '2026-10-30')).toBe(false);
    expect(isOverdue('DELIVERED', '2026-10-01', '2026-10-30')).toBe(false);
    expect(isOverdue('PLANNING', null, '2026-10-30')).toBe(false);
  });
});
