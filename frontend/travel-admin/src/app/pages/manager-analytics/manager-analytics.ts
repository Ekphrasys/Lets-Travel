import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { TripService } from '../../services/trip.service';
import type { TripAnalytics } from '../../models/travel.models';

type SortKey = 'averageRating' | 'confirmedBookings' | 'revenue' | 'occupancyRate';

@Component({
  selector: 'app-manager-analytics',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './manager-analytics.html',
  styleUrl: './manager-analytics.css'
})
export class ManagerAnalyticsComponent implements OnInit {
  private tripService = inject(TripService);

  trips = signal<TripAnalytics[]>([]);
  loading = signal(true);
  error = signal('');
  sortKey = signal<SortKey>('revenue');

  sortedTrips = computed(() => {
    const key = this.sortKey();
    return [...this.trips()].sort((a, b) => b[key] - a[key]);
  });

  highlights = computed(() => {
    const list = this.trips();
    if (!list.length) return null;
    const rated = list.filter(t => t.feedbackCount > 0);
    return {
      bestRated: rated.length ? rated.reduce((a, b) => b.averageRating > a.averageRating ? b : a) : null,
      mostBooked: list.reduce((a, b) => b.confirmedBookings > a.confirmedBookings ? b : a),
      topRevenue: list.reduce((a, b) => b.revenue > a.revenue ? b : a),
    };
  });

  insights = computed((): string[] => {
    const list = this.trips();
    if (!list.length) return [];
    const tips: string[] = [];

    const lowOccupancy = list.filter(t => t.confirmedBookings + t.seatsAvailable > 0 && t.occupancyRate < 30);
    if (lowOccupancy.length) {
      tips.push(`${lowOccupancy.length} voyage(s) ont un taux de remplissage inférieur à 30 % — envisagez une promotion ou un ajustement tarifaire.`);
    }

    const highPerformers = list.filter(t => t.averageRating >= 4.5 && t.feedbackCount > 0);
    if (highPerformers.length) {
      const destinations = [...new Set(highPerformers.map(t => t.destinationCity))].join(', ');
      tips.push(`Destinations très bien notées (≥ 4,5★) : ${destinations} — reproduire ces voyages.`);
    }

    const lowRated = list.filter(t => t.feedbackCount > 0 && t.averageRating < 3);
    if (lowRated.length) {
      tips.push(`${lowRated.length} voyage(s) ont une note inférieure à 3★ — analysez les avis pour identifier les problèmes.`);
    }

    const byDest = new Map<string, number>();
    list.forEach(t => byDest.set(t.destinationCity, (byDest.get(t.destinationCity) ?? 0) + t.confirmedBookings));
    if (byDest.size > 0) {
      const topDest = [...byDest.entries()].sort((a, b) => b[1] - a[1])[0];
      tips.push(`Destination la plus demandée : ${topDest[0]} (${topDest[1]} réservation(s) confirmée(s)).`);
    }

    const noFeedback = list.filter(t => t.confirmedBookings > 0 && t.feedbackCount === 0);
    if (noFeedback.length) {
      tips.push(`${noFeedback.length} voyage(s) avec des voyageurs n'ont encore reçu aucun avis — encouragez vos clients à laisser un retour.`);
    }

    return tips;
  });

  ratingDistribution = computed(() => {
    const list = this.trips().filter(t => t.feedbackCount > 0);
    const total = list.reduce((s, t) => s + t.feedbackCount, 0);
    if (!total) return null;

    const buckets: Record<number, number> = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
    list.forEach(t => {
      const rounded = Math.round(t.averageRating);
      if (rounded >= 1 && rounded <= 5) buckets[rounded] += t.feedbackCount;
    });

    return Object.entries(buckets)
      .sort((a, b) => Number(b[0]) - Number(a[0]))
      .map(([star, count]) => ({ star: Number(star), count, pct: total > 0 ? count / total * 100 : 0 }));
  });

  ngOnInit(): void {
    this.tripService.myAnalytics().subscribe({
      next: data => { this.trips.set(data); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger les données analytiques.'); this.loading.set(false); }
    });
  }

  setSort(key: SortKey): void {
    this.sortKey.set(key);
  }

  stars(rating: number): string {
    const r = Math.round(rating);
    return '★'.repeat(r) + '☆'.repeat(5 - r);
  }
}
