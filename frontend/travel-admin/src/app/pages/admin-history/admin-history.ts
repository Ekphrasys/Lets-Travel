import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TripService } from '../../services/trip.service';
import { FeedbackService } from '../../services/feedback.service';
import { TripAnalytics, Feedback } from '../../models/travel.models';

@Component({
  selector: 'app-admin-history',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-history.html',
  styleUrl: './admin-history.css'
})
export class AdminHistoryComponent implements OnInit {
  private tripService = inject(TripService);
  private feedbackService = inject(FeedbackService);

  trips = signal<TripAnalytics[]>([]);
  feedbacks = signal<Feedback[]>([]);
  loading = signal(true);
  ratingFilter = signal<number | null>(null);
  tripSearch = signal('');

  filteredFeedbacks = computed(() => {
    const filter = this.ratingFilter();
    return this.feedbacks().filter(f => filter === null || f.rating === filter);
  });

  filteredTrips = computed(() => {
    const q = this.tripSearch().toLowerCase().trim();
    if (!q) return this.trips();
    return this.trips().filter(t =>
      t.title.toLowerCase().includes(q) ||
      t.originCity.toLowerCase().includes(q) ||
      t.destinationCity.toLowerCase().includes(q)
    );
  });

  averageGlobalRating = computed(() => {
    const all = this.feedbacks();
    if (!all.length) return 0;
    return all.reduce((sum, f) => sum + f.rating, 0) / all.length;
  });

  positiveRatingPct = computed(() => {
    const all = this.feedbacks();
    if (!all.length) return 0;
    return Math.round(all.filter(f => f.rating >= 4).length / all.length * 100);
  });

  ratingDistribution = computed(() => {
    const all = this.feedbacks();
    return [5, 4, 3, 2, 1].map(star => ({
      star,
      count: all.filter(f => f.rating === star).length,
      pct: all.length ? all.filter(f => f.rating === star).length / all.length * 100 : 0
    }));
  });

  ngOnInit(): void {
    let done = 0;
    const check = () => { if (++done === 2) this.loading.set(false); };

    this.tripService.adminTravelHistory().subscribe({
      next: data => { this.trips.set(data); check(); },
      error: () => check()
    });

    this.feedbackService.allFeedbacks().subscribe({
      next: data => { this.feedbacks.set(data); check(); },
      error: () => check()
    });
  }

  setRatingFilter(rating: number | null): void {
    this.ratingFilter.set(this.ratingFilter() === rating ? null : rating);
  }

  setTripSearch(value: string): void {
    this.tripSearch.set(value);
  }

  floorRating(value: number): number {
    return Math.min(5, Math.max(0, Math.floor(value)));
  }
}
