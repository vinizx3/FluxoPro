import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/entrar']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/funil']) : true;
};

export const ownerGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isOwner() ? true : inject(Router).createUrlTree(['/funil']);
};
