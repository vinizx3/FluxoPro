import { whatsappUrl } from './whatsapp';

describe('whatsappUrl', () => {
  it('adds the country code to a local number', () => {
    expect(whatsappUrl('(61) 99999-0000')).toBe('https://wa.me/5561999990000');
    expect(whatsappUrl('6133334444')).toBe('https://wa.me/556133334444');
  });

  it('keeps a number that already has the country code', () => {
    expect(whatsappUrl('+55 61 99999-0000')).toBe('https://wa.me/5561999990000');
  });

  it('encodes an optional message', () => {
    expect(whatsappUrl('61999990000', 'Olá, tudo bem?')).toBe(
      'https://wa.me/5561999990000?text=Ol%C3%A1%2C%20tudo%20bem%3F',
    );
  });

  it('returns null for missing or invalid numbers', () => {
    expect(whatsappUrl(null)).toBeNull();
    expect(whatsappUrl('')).toBeNull();
    expect(whatsappUrl('12345')).toBeNull();
  });
});
