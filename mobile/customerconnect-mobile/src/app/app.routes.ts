import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: ':mode',
        loadComponent: () =>
          import('./features/authentication/mobile-auth.component').then(
            (component) => component.MobileAuthComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'login' },
    ],
  },
  {
    path: 'tabs',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/app-tabs/app-tabs.component').then(
        (component) => component.AppTabsComponent,
      ),
    children: [
      {
        path: 'home',
        loadComponent: () =>
          import('./features/dashboard/mobile-dashboard.component').then(
            (component) => component.MobileDashboardComponent,
          ),
      },
      {
        path: 'customers',
        loadComponent: () =>
          import('./features/foundation/mobile-foundation.component').then(
            (component) => component.MobileFoundationComponent,
          ),
        data: { title: 'Customers' },
      },
      {
        path: 'work',
        loadComponent: () =>
          import('./features/foundation/mobile-foundation.component').then(
            (component) => component.MobileFoundationComponent,
          ),
        data: { title: 'Work' },
      },
      {
        path: 'ai',
        loadComponent: () =>
          import('./ai/assistant-sheet/mobile-assistant.component').then(
            (component) => component.MobileAssistantComponent,
          ),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/mobile-profile.component').then(
            (component) => component.MobileProfileComponent,
          ),
      },
      { path: '', pathMatch: 'full', redirectTo: 'home' },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'tabs/home' },
  { path: '**', redirectTo: 'tabs/home' },
];
