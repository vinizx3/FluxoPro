import { Routes } from '@angular/router';
import { authGuard, guestGuard, ownerGuard } from './core/auth.guards';

export const routes: Routes = [
  {
    path: 'entrar',
    title: 'Entrar · FluxoPro',
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
        title: 'Funil · FluxoPro',
        loadComponent: () => import('./features/funnel/funnel-page').then((m) => m.FunnelPage),
      },
      {
        path: 'alertas',
        title: 'Alertas · FluxoPro',
        loadComponent: () => import('./features/alerts/alerts-page').then((m) => m.AlertsPage),
      },
      {
        path: 'clientes',
        title: 'Clientes · FluxoPro',
        loadComponent: () =>
          import('./features/customers/customers-page').then((m) => m.CustomersPage),
      },
      {
        path: 'projetos',
        title: 'Projetos · FluxoPro',
        loadComponent: () =>
          import('./features/projects/projects-page').then((m) => m.ProjectsPage),
      },
      {
        path: 'projetos/:id',
        title: 'Projeto · FluxoPro',
        loadComponent: () =>
          import('./features/projects/project-detail-page').then((m) => m.ProjectDetailPage),
      },
      {
        path: 'conta',
        title: 'Minha conta · FluxoPro',
        loadComponent: () => import('./features/account/account-page').then((m) => m.AccountPage),
      },
      {
        path: 'painel',
        title: 'Painel · FluxoPro',
        canActivate: [ownerGuard],
        loadComponent: () =>
          import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
      },
      {
        path: 'equipe',
        title: 'Equipe · FluxoPro',
        canActivate: [ownerGuard],
        loadComponent: () => import('./features/team/team-page').then((m) => m.TeamPage),
      },
      {
        path: 'historico',
        title: 'Histórico · FluxoPro',
        canActivate: [ownerGuard],
        loadComponent: () => import('./features/audit/audit-page').then((m) => m.AuditPage),
      },
      {
        path: 'negociacoes/:id',
        title: 'Negociação · FluxoPro',
        loadComponent: () =>
          import('./features/opportunity/opportunity-detail-page').then(
            (m) => m.OpportunityDetailPage,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'funil' },
    ],
  },
  { path: '**', redirectTo: '' },
];
