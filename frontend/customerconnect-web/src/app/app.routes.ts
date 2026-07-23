import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () =>
      import('./features/authentication/auth.routes').then((routes) => routes.AUTH_ROUTES),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/app-shell/app-shell.component').then(
        (component) => component.AppShellComponent,
      ),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (component) => component.DashboardComponent,
          ),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(
            (component) => component.ProfileComponent,
          ),
      },
      ...['customers', 'invoices', 'employees', 'timesheets', 'reports', 'administration'].map(
        (path) => ({
          path,
          loadComponent: () =>
            import('./features/foundation/foundation-page.component').then(
              (component) => component.FoundationPageComponent,
            ),
          data: { title: path[0].toUpperCase() + path.slice(1) },
        }),
      ),
      { path: '', pathMatch: 'full' as const, redirectTo: 'dashboard' },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
