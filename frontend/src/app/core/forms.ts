/** Campo opcional em branco vira null, para o backend não receber "" (texto vazio). */
export function blankToNull(value: string | null | undefined): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
