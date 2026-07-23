import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  {
    path: ':mode',
    loadComponent: () =>
      import('./auth-page.component').then((component) => component.AuthPageComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
];
