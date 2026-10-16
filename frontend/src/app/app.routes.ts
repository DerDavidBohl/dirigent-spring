import {Routes} from '@angular/router';
import {authGuard, guestGuard} from './core/auth/auth.guard';

// COD-009: every feature area is its own lazily loaded chunk.
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Sign in · Dirigent',
    loadComponent: () => import('./features/login/login').then(m => m.Login)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: [
      {path: '', pathMatch: 'full', title: 'Dashboard · Dirigent', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard)},
      {path: 'deployments', title: 'Deployments · Dirigent', loadComponent: () => import('./features/deployments/deployments').then(m => m.Deployments)},
      {path: 'updates', title: 'Updates · Dirigent', loadComponent: () => import('./features/updates/updates').then(m => m.Updates)},
      {path: 'secrets', title: 'Secrets · Dirigent', loadComponent: () => import('./features/secrets/secrets').then(m => m.Secrets)},
      {path: 'settings/tokens', title: 'Machine tokens · Dirigent', loadComponent: () => import('./features/tokens/tokens').then(m => m.Tokens)}
    ]
  },
  {path: '**', redirectTo: ''}
];
