import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TripService } from '../../services/trip.service';
import { ManagerPerformance } from '../../models/travel.models';

@Component({
  selector: 'app-admin-managers',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-managers.html',
  styleUrl: './admin-managers.css'
})
export class AdminManagersComponent implements OnInit {
  private tripService = inject(TripService);

  managers = signal<ManagerPerformance[]>([]);
  loading = signal(true);

  // Max values used to scale the breakdown bars
  maxScore   = computed(() => Math.max(...this.managers().map(m => m.performanceScore), 1));
  maxIncome  = computed(() => Math.max(...this.managers().map(m => m.income), 1));
  maxTrips   = computed(() => Math.max(...this.managers().map(m => m.tripsCount), 1));

  globalAvgRating = computed(() => {
    const all = this.managers();
    if (!all.length) return 0;
    const rated = all.filter(m => m.feedbackCount > 0);
    if (!rated.length) return 0;
    return rated.reduce((s, m) => s + m.averageRating, 0) / rated.length;
  });

  totalIncome = computed(() =>
    this.managers().reduce((s, m) => s + m.income, 0)
  );

  top3 = computed(() => this.managers().slice(0, 3));

  ngOnInit(): void {
    this.tripService.adminManagersRanking().subscribe({
      next: data => { this.managers.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  // Score breakdown components (pts)
  ratingPts(m: ManagerPerformance): number { return m.averageRating * 20; }
  tripsPts(m: ManagerPerformance):  number { return m.tripsCount * 5; }
  incomePts(m: ManagerPerformance): number { return m.income / 100; }

  stars(rating: number): string {
    const full = Math.min(5, Math.max(0, Math.round(rating)));
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }
}
