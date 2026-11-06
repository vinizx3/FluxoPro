import { fieldLabel, formatValue } from './audit';

describe('audit formatting', () => {
  it('translates statuses from both opportunities and projects', () => {
    expect(formatValue('status', 'QUALIFICATION')).toBe('Qualificação');
    expect(formatValue('status', 'IN_PROGRESS')).toBe('Em andamento');
    expect(formatValue('status', 'SOMETHING_NEW')).toBe('SOMETHING_NEW');
  });

  it('formats money, dates and empty values', () => {
    expect(formatValue('amount', '48000.00')).toBe('R$ 48.000,00');
    expect(formatValue('expectedDeliveryDate', '2026-10-30')).toBe('30/10/2026');
    expect(formatValue('title', null)).toBe('—');
    expect(formatValue('title', '')).toBe('—');
  });

  it('never prints raw ids', () => {
    expect(formatValue('customerId', 'e889dab5-0ad0-460d-899f-cfd36db5dc49')).toBe('vinculado');
  });

  it('falls back to the raw field name for unknown fields', () => {
    expect(fieldLabel('title')).toBe('Título');
    expect(fieldLabel('novoCampo')).toBe('novoCampo');
  });
});
