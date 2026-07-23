import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IonButton, IonContent } from '@ionic/angular/standalone';
import { MobileSessionService } from '../../core/auth/mobile-session.service';

@Component({
  selector: 'app-mobile-profile',
  imports: [IonContent, IonButton],
  template: `<ion-content [fullscreen]="true"
    ><main>
      <span>Account</span>
      <h1>Profile</h1>
      <section>
        <div class="avatar">{{ (session.user()?.firstName || 'U')[0] }}</div>
        <h2>{{ session.user()?.firstName }} {{ session.user()?.lastName }}</h2>
        <p>{{ session.user()?.email }}</p>
        <b>Protected account</b>
      </section>
      <article>
        <strong>Notifications</strong><span>Notification preferences foundation</span>
      </article>
      <ion-button expand="block" fill="outline" (click)="logout()">Sign out</ion-button>
    </main></ion-content
  >`,
  styles: [
    `
      ion-content {
        --background: #f5f7fb;
      }
      main {
        padding: calc(env(safe-area-inset-top) + 1.5rem) 1rem;
        max-width: 680px;
        margin: auto;
      }
      main > span {
        color: #5b5fef;
        text-transform: uppercase;
        font-size: 0.68rem;
        font-weight: 800;
      }
      h1 {
        font-size: 2rem;
        margin: 0.4rem 0 1.5rem;
      }
      section,
      article {
        padding: 1.5rem;
        text-align: center;
        border: 1px solid #dde3ec;
        border-radius: 17px;
        background: white;
      }
      .avatar {
        display: grid;
        place-items: center;
        width: 4.2rem;
        aspect-ratio: 1;
        margin: auto;
        border-radius: 50%;
        color: white;
        background: #0b1739;
        font-size: 1.5rem;
        font-weight: 800;
      }
      section p,
      article span {
        color: #667085;
      }
      section b {
        color: #047857;
        font-size: 0.75rem;
      }
      article {
        display: grid;
        text-align: left;
        gap: 0.3rem;
        margin: 1rem 0;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobileProfileComponent {
  readonly session = inject(MobileSessionService);
  private readonly router = inject(Router);
  logout(): void {
    this.session.clear();
    void this.router.navigate(['/auth/login']);
  }
}
