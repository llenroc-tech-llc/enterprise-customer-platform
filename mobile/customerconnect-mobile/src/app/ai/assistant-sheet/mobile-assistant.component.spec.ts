import { TestBed } from '@angular/core/testing';
import { MobileAssistantComponent } from './mobile-assistant.component';

describe('MobileAssistantComponent', () => {
  it('starts empty and shows the unavailable state without transmitting data', async () => {
    await TestBed.configureTestingModule({
      imports: [MobileAssistantComponent],
    }).compileComponents();
    const component = TestBed.createComponent(MobileAssistantComponent).componentInstance;
    expect(component.messages()).toEqual([]);
    component.send('Summarize overdue invoices');
    expect(component.messages()).toEqual(['Summarize overdue invoices']);
    expect(component.unavailable()).toBeTrue();
  });
});
