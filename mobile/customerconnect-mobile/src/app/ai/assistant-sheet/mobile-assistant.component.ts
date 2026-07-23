import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IonContent, IonInput } from '@ionic/angular/standalone';

@Component({
  selector: 'app-mobile-assistant',
  imports: [FormsModule, IonContent, IonInput],
  templateUrl: './mobile-assistant.component.html',
  styleUrl: './mobile-assistant.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileAssistantComponent {
  readonly messages = signal<string[]>([]);
  readonly unavailable = signal(false);
  prompt = '';
  readonly suggestions = [
    'Summarize overdue invoices',
    'Which customers need attention?',
    'Show my unsubmitted timesheets',
    'Find unusual invoice activity',
  ];
  send(value = this.prompt): void {
    const prompt = value.trim();
    if (!prompt) return;
    this.messages.update((messages) => [...messages, prompt]);
    this.unavailable.set(true);
    this.prompt = '';
  }
}
