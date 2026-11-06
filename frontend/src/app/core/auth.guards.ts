import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Só deixa passar quem está logado. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/entrar']);
};

/** Telas de entrada: quem já está logado vai direto para o funil. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/funil']) : true;
};

/**
 * Rotas só do dono (por exemplo, o histórico de auditoria).
 * Isto evita uma tela vazia ou um erro feio; a proteção real é o 403 do backend.
 */
export const ownerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isOwner() ? true : inject(Router).createUrlTree(['/funil']);
};
