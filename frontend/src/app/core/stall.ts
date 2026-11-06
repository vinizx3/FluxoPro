/** A partir de quantos dias sem mexer a negociação é considerada esfriando (igual ao padrão do backend). */
export const STALL_DAYS = 7;
export const TICKS = 10;

const DAY_MS = 86_400_000;

export function daysSince(iso: string, now: number): number {
  const time = Date.parse(iso);
  if (Number.isNaN(time)) {
    return 0;
  }
  return Math.max(0, Math.floor((now - time) / DAY_MS));
}

export function isCold(days: number): boolean {
  return days >= STALL_DAYS;
}

/** Quantos dos 10 tracinhos do termômetro continuam "quentes": um a menos por dia parado. */
export function activeTicks(days: number): number {
  return Math.min(TICKS, Math.max(0, TICKS - days));
}

/** Soma, em reais, o valor das negociações informadas (as sem valor contam como zero). */
export function sumAmounts(items: { amount: number | null }[]): number {
  return items.reduce((sum, item) => sum + Number(item.amount ?? 0), 0);
}
