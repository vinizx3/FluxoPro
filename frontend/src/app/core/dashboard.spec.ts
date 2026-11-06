import { barWidth, conversionNote, formatDays } from './dashboard';

describe('dashboard helpers', () => {
  it('scales bars against the largest value, with a visible minimum', () => {
    expect(barWidth(10, 10)).toBe(100);
    expect(barWidth(5, 10)).toBe(50);
    expect(barWidth(0, 10)).toBe(0);
    expect(barWidth(1, 1000)).toBe(4);
    expect(barWidth(3, 0)).toBe(0);
  });

  it('formats days with comma and singular', () => {
    expect(formatDays(1)).toBe('1 dia');
    expect(formatDays(12.46)).toBe('12,5 dias');
    expect(formatDays(0)).toBe('0 dias');
  });

  it('explains the conversion rate', () => {
    expect(conversionNote(0, 0)).toBe('Nenhuma negociação fechada no período');
    expect(conversionNote(1, 0)).toBe('1 ganha de 1 fechada');
    expect(conversionNote(2, 1)).toBe('2 ganhas de 3 fechadas');
  });
});
