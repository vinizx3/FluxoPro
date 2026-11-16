import { HttpErrorResponse } from '@angular/common/http';

export function errorMessage(
  error: unknown,
  overrides: Partial<Record<number, string>> = {},
): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.';
    }
    const custom = overrides[error.status];
    if (custom) {
      return custom;
    }
    switch (error.status) {
      case 400:
      case 422:
        return 'Confira os dados informados e tente novamente.';
      case 401:
        return 'Sua sessão expirou. Entre novamente.';
      case 403:
        return 'Você não tem permissão para fazer isso.';
      case 404:
        return 'Não encontramos o que você procurou.';
      case 409:
        return 'O registro foi alterado por outra pessoa. Recarregue e tente novamente.';
    }
  }
  return 'Algo deu errado. Tente novamente em instantes.';
}
