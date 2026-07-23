import { Injectable, signal } from '@angular/core';
import { AssistantMessage, AssistantState } from '../models/assistant.models';

@Injectable({ providedIn: 'root' })
export class AssistantService {
  readonly state = signal<AssistantState>('empty');
  readonly messages = signal<AssistantMessage[]>([]);

  send(prompt: string): void {
    this.messages.update((messages) => [...messages, { role: 'user', text: prompt }]);
    this.state.set('unavailable');
  }
}
