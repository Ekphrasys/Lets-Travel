import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { TravelerService } from '../../services/traveler.service';
import { AuthService } from '../../services/auth.service';
import type { TravelerProfile } from '../../models/travel.models';

@Component({
  selector: 'app-traveler-profile',
  imports: [DatePipe, CurrencyPipe],
  templateUrl: './traveler-profile.html',
  styleUrl: './traveler-profile.css'
})
export class TravelerProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private travelerService = inject(TravelerService);
  auth = inject(AuthService);

  profile = signal<TravelerProfile | null>(null);
  loading = signal(true);
  error = signal('');

  isOwnProfile = false;

  ngOnInit(): void {
    const travelerId = this.route.snapshot.paramMap.get('travelerId') ?? '';
    this.isOwnProfile = this.auth.currentUser()?.sub === travelerId;

    this.travelerService.getProfile(travelerId).subscribe({
      next: p => { this.profile.set(p); this.loading.set(false); },
      error: () => { this.error.set('Voyageur introuvable.'); this.loading.set(false); }
    });
  }

  stars(rating: number): string {
    const full = Math.round(rating);
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }
}
