import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { IonContent, IonRefresher, IonRefresherContent } from '@ionic/angular/standalone';

@Component({
  selector: 'app-mobile-dashboard',
  imports: [IonContent, IonRefresher, IonRefresherContent],
  templateUrl: './mobile-dashboard.component.html',
  styleUrl: './mobile-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileDashboardComponent {
  readonly metrics = signal([
    ['Outstanding', '$128.4K', '8 invoices need attention'],
    ['Customers', '248', '12 added this month'],
    ['Timesheets', '17', 'Need action'],
    ['Approvals', '9', '3 high priority'],
  ]);
  refresh(event: CustomEvent): void {
    window.setTimeout(() => (event.target as HTMLIonRefresherElement).complete(), 400);
  }
}
