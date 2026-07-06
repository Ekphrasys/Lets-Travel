import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cookie-consent',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (!accepted()) {
      <div class="cookie-banner" role="dialog" aria-labelledby="cookie-title">
        <div class="cookie-content">
          <h3 id="cookie-title">🍪 Confidentialité</h3>
          <p>
            Nous utilisons des cookies uniquement pour assurer le fonctionnement du service. Aucun cookie publicitaire ou de suivi tiers n'est déposé sans votre consentement.
            Vous pouvez modifier vos préférences à tout moment depuis la page <a routerLink="/privacy">Confidentialité</a>.
          </p>
          <button class="btn btn-primary" (click)="accept()">Accepter</button>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { display: block; position: fixed; inset: auto 0 0 0; z-index: 9999; }
    .cookie-banner {
      background: #1a365d;
      color: #fff;
      padding: 1.25rem;
      box-shadow: 0 -4px 24px rgba(0,0,0,0.15);
    }
    .cookie-content {
      max-width: 1100px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      gap: 1.25rem;
      flex-wrap: wrap;
      justify-content: space-between;
    }
    .cookie-content h3 { margin: 0; font-size: 1rem; }
    .cookie-content p { margin: 0; font-size: 0.9rem; opacity: 0.95; flex: 1; min-width: 220px; }
    .cookie-content .btn { padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; background: #3182ce; color: #fff; }
    .cookie-content a { color: #90cdf4; }
  `]
})
export class CookieConsentComponent {
  accepted = signal(localStorage.getItem('cookieConsent') === 'true');

  accept(): void {
    localStorage.setItem('cookieConsent', 'true');
    this.accepted.set(true);
  }
}
