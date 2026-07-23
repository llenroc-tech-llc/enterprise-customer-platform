import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { IonContent } from '@ionic/angular/standalone';

@Component({
  selector: 'app-mobile-foundation',
  imports: [IonContent],
  template: `<ion-content [fullscreen]="true"
    ><main>
      <span>Module foundation</span>
      <h1>{{ title }}</h1>
      <section>
        <div>◈</div>
        <h2>{{ title }} workspace</h2>
        <p>
          This native-oriented workspace is ready for the next backend-supported feature. No API
          endpoint has been invented.
        </p>
        <button disabled>Coming soon</button>
      </section>
    </main></ion-content
  >`,
  styles: [
    `
      ion-content {
        --background: #f5f7fb;
      }
      main {
        padding: calc(env(safe-area-inset-top) + 1.5rem) 1rem;
        max-width: 720px;
        margin: auto;
      }
      main > span {
        color: #5b5fef;
        text-transform: uppercase;
        letter-spacing: 0.12em;
        font-size: 0.68rem;
        font-weight: 800;
      }
      h1 {
        font-size: 2.1rem;
        margin: 0.4rem 0 1.5rem;
      }
      section {
        text-align: center;
        padding: 2.5rem 1.2rem;
        border: 1px dashed #cbd4e1;
        border-radius: 18px;
        background: white;
      }
      section div {
        font-size: 2rem;
        color: #5b5fef;
      }
      section p {
        color: #667085;
        line-height: 1.55;
      }
      button {
        padding: 0.7rem 1rem;
        border: 0;
        border-radius: 10px;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileFoundationComponent {
  readonly title = inject(ActivatedRoute).snapshot.data['title'] as string;
}
