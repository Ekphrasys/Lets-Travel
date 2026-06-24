import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FeedbackService } from '../../services/feedback.service';
import { TripService } from '../../services/trip.service';
import { Feedback, Trip } from '../../models/travel.models';

@Component({
  selector: 'app-manager-feedback',
  imports: [RouterLink, DatePipe],
  templateUrl: './manager-feedback.html',
  styleUrl: './manager-feedback.css'
})
export class ManagerFeedbackComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private feedbackService = inject(FeedbackService);
  private tripService = inject(TripService);

  trip = signal<Trip | null>(null);
  feedbacks = signal<Feedback[]>([]);
  loading = signal(true);
  error = signal('');

  get averageRating(): string {
    const list = this.feedbacks();
    if (!list.length) return '—';
    const avg = list.reduce((sum, f) => sum + f.rating, 0) / list.length;
    return avg.toFixed(1);
  }

  ngOnInit(): void {
    const tripId = this.route.snapshot.paramMap.get('id')!;
    this.tripService.getById(tripId).subscribe(t => this.trip.set(t));
    this.feedbackService.byTrip(tripId).subscribe({
      next: feedbacks => { this.feedbacks.set(feedbacks); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger les avis.'); this.loading.set(false); }
    });
  }

  stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }
}
