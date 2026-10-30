import { Routes } from '@angular/router';
import { authGuard, guestGuard, ownerGuard } from './core/auth.guards';

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
        path: 'alertas',
        loadComponent: () => import('./features/alerts/alerts-page').then((m) => m.AlertsPage),
      },
      {
        path: 'clientes',
        loadComponent: () => import('./features/customers/customers-page').then((m) => m.CustomersPage),
      },
      {
        path: 'projetos',
        loadComponent: () => import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'projetos/:id',
        loadComponent: () =>
          import('./features/projects/project-detail-page').then((m) => m.ProjectDetailPage),
      },
      {
        path: 'historico',
        canActivate: [ownerGuard],
        loadComponent: () => import('./features/audit/audit-page').then((m) => m.AuditPage),
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
