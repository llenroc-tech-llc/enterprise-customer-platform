import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-foundation-page',
  template: `<header>
      <span>Module foundation</span>
      <h1>{{ title }}</h1>
      <p>This workspace is ready for the next backend-supported feature increment.</p>
    </header>
    <section>
      <div>◈</div>
      <h2>{{ title }} workspace</h2>
      <p>
        Navigation, permissions, responsive layout, and loading-state boundaries are prepared. No
        backend endpoint has been invented.
      </p>
      <button disabled>Coming in a future release</button>
    </section>`,
  styles: [
    `
      header span {
        color: #5b5fef;
        font-size: 0.72rem;
        font-weight: 800;
        text-transform: uppercase;
        letter-spacing: 0.12em;
      }
      h1 {
        font-size: clamp(2rem, 3vw, 3rem);
        margin: 0.4rem 0;
      }
      header p,
      section p {
        color: #667085;
      }
      section {
        max-width: 700px;
        margin-top: 2rem;
        padding: 3rem;
        text-align: center;
        border: 1px dashed #cbd4e1;
        border-radius: 16px;
        background: white;
      }
      section div {
        font-size: 2rem;
        color: #5b5fef;
      }
      button {
        padding: 0.7rem 1rem;
        border: 0;
        border-radius: 10px;
        color: #667085;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FoundationPageComponent {
  readonly title = inject(ActivatedRoute).snapshot.data['title'] as string;
}
