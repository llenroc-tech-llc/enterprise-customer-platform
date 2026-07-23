export interface AssistantMessage {
  role: 'user' | 'assistant';
  text: string;
  sources?: string[];
}

export type AssistantState = 'empty' | 'loading' | 'unavailable' | 'offline';
