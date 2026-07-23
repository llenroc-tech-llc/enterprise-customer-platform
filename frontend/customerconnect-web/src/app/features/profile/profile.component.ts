import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthSessionService } from '../../core/auth/auth-session.service';

@Component({
  selector: 'app-profile',
  template: `<header class="page">
      <span>Account</span>
      <h1>Your profile</h1>
      <p>Review your workspace identity and security posture.</p>
    </header>
    <section class="profile-card">
      <div class="avatar">{{ (session.user()?.firstName || 'U')[0] }}</div>
      <div>
        <h2>{{ session.user()?.firstName }} {{ session.user()?.lastName }}</h2>
        <p>{{ session.user()?.email }}</p>
        <span>Protected account</span>
      </div>
    </section>`,
  styles: [
    `
      .page span {
        color: #5b5fef;
        font-size: 0.75rem;
        font-weight: 800;
        text-transform: uppercase;
      }
      .page h1 {
        font-size: 2.5rem;
        margin: 0.4rem 0;
      }
      .page p,
      .profile-card p {
        color: #667085;
      }
      .profile-card {
        display: flex;
        gap: 1rem;
        align-items: center;
        margin-top: 2rem;
        padding: 1.5rem;
        background: white;
        border: 1px solid #dde3ec;
        border-radius: 16px;
      }
      .avatar {
        display: grid;
        place-items: center;
        width: 4rem;
        aspect-ratio: 1;
        border-radius: 50%;
        background: #0b1739;
        color: white;
        font-size: 1.5rem;
        font-weight: 800;
      }
      .profile-card h2 {
        margin: 0;
      }
      .profile-card span {
        color: #047857;
        font-size: 0.75rem;
        font-weight: 700;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent {
  readonly session = inject(AuthSessionService);
}
