import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { AssistantPanelComponent } from '../../ai/assistant-panel/assistant-panel.component';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AssistantPanelComponent],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShellComponent {
  private readonly router = inject(Router);
  readonly session = inject(AuthSessionService);
  readonly compact = signal(false);
  readonly drawerOpen = signal(false);
  readonly assistantOpen = signal(false);

  readonly navigation = [
    ['dashboard', 'Overview', '⌂'],
    ['customers', 'Customers', '◉'],
    ['invoices', 'Invoices', '▤'],
    ['employees', 'Employees', '♙'],
    ['timesheets', 'Timesheets', '◷'],
    ['reports', 'Reports', '↗'],
    ['administration', 'Administration', '⚙'],
  ] as const;

  logout(): void {
    this.session.clear();
    void this.router.navigate(['/auth/login']);
  }
}
