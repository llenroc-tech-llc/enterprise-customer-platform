import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  inject,
  Input,
  Output,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssistantService } from '../services/assistant.service';

@Component({
  selector: 'app-assistant-panel',
  imports: [FormsModule],
  templateUrl: './assistant-panel.component.html',
  styleUrl: './assistant-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssistantPanelComponent {
  @Input() open = false;
  @Output() readonly closed = new EventEmitter<void>();
  readonly assistant = inject(AssistantService);
  prompt = '';
  readonly suggestions = [
    'Summarize overdue invoices',
    'Which customers need attention?',
    'Draft a payment reminder',
    'Show my unsubmitted timesheets',
  ];

  send(value = this.prompt): void {
    const prompt = value.trim();
    if (!prompt) return;
    this.assistant.send(prompt);
    this.prompt = '';
  }
}
