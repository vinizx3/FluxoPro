/**
 * Monta o link do WhatsApp (wa.me) a partir de um telefone digitado do jeito brasileiro.
 * Aceita "(61) 99999-0000", "61999990000" ou "+55 61 99999-0000".
 * Devolve null quando o número não parece um telefone brasileiro válido.
 */
export function whatsappUrl(phone: string | null | undefined, message?: string): string | null {
  if (!phone) {
    return null;
  }
  let digits = phone.replace(/\D/g, '');
  if (digits.startsWith('0055')) {
    digits = digits.slice(2);
  }
  if (digits.length === 10 || digits.length === 11) {
    digits = `55${digits}`;
  }
  if (!/^55\d{10,11}$/.test(digits)) {
    return null;
  }
  const base = `https://wa.me/${digits}`;
  return message ? `${base}?text=${encodeURIComponent(message)}` : base;
}
