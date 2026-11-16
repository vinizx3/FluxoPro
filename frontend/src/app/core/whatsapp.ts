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

export function followUpUrl(phone: string | null | undefined, title: string): string | null {
  return whatsappUrl(phone, `Olá! Tudo bem? Passando para falar sobre "${title}".`);
}
