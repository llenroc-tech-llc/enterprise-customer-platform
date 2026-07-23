import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  // Presentation-only mock. Replace with an API adapter when dashboard endpoints exist.
  readonly snapshot = signal({
    metrics: [
      {
        label: 'Outstanding invoices',
        value: '$128.4K',
        change: '8 need attention',
        tone: 'indigo',
      },
      { label: 'Active customers', value: '248', change: '12 added this month', tone: 'emerald' },
      { label: 'Timesheets needing action', value: '17', change: 'Due by Friday', tone: 'warning' },
      { label: 'Pending approvals', value: '9', change: '3 high priority', tone: 'violet' },
    ],
    activity: [
      ['Invoice #1048 approved', 'Finance workflow', '12 min ago'],
      ['Acme account updated', 'Customer operations', '42 min ago'],
      ['Timesheet submitted', 'People operations', '1 hr ago'],
    ],
  });
}
