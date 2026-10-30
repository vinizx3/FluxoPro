import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guards';

export const routes: Routes = [
  {
    path: 'entrar',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      {
        path: 'funil',
        loadComponent: () => import('./features/funnel/funnel-page').then((m) => m.FunnelPage),
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/customers/customers-page').then((m) => m.CustomersPage),
      },
      {
        path: 'negociacoes/:id',
        loadComponent: () =>
          import('./features/opportunity/opportunity-detail-page').then((m) => m.OpportunityDetailPage),
      },
      { path: '', pathMatch: 'full', redirectTo: 'funil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
