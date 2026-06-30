import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingService } from '../../services/booking.service';
import { FeedbackService } from '../../services/feedback.service';
import { Booking } from '../../models/travel.models';

@Component({
  selector: 'app-bookings',
  imports: [DatePipe, FormsModule],
  templateUrl: './bookings.html',
  styleUrl: './bookings.css'
})
export class BookingsComponent implements OnInit {
  private bookingService = inject(BookingService);
  private feedbackService = inject(FeedbackService);

  bookings = signal<Booking[]>([]);
  message = signal('');

  activeFeedbackBookingId = signal<string | null>(null);
  feedbackRating = signal(0);
  feedbackComment = signal('');
  submittedBookingIds = signal<Set<string>>(new Set());

  readonly stars = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.bookingService.myBookings().subscribe(b => this.bookings.set(b));
  }

  cancel(id: string): void {
    if (!confirm('Annuler cette réservation ?')) return;
    this.bookingService.cancel(id).subscribe({
      next: () => {
        this.message.set('Réservation annulée.');
        this.load();
      },
      error: (err) => this.message.set(err.error?.message || 'Annulation impossible.')
    });
  }

  openFeedback(bookingId: string): void {
    this.activeFeedbackBookingId.set(bookingId);
    this.feedbackRating.set(0);
    this.feedbackComment.set('');
  }

  closeFeedback(): void {
    this.activeFeedbackBookingId.set(null);
  }

  setRating(star: number): void {
    this.feedbackRating.set(star);
  }

  submitFeedback(tripId: string, bookingId: string): void {
    const rating = this.feedbackRating();
    if (rating === 0) return;
    this.feedbackService.submit(tripId, rating, this.feedbackComment()).subscribe({
      next: () => {
        this.submittedBookingIds.update(s => new Set([...s, bookingId]));
        this.activeFeedbackBookingId.set(null);
        this.message.set('Merci pour votre avis !');
      },
      error: () => this.message.set('Impossible d\'envoyer l\'avis.')
    });
  }

  canCancel(booking: Booking): boolean {
    if (!booking.tripDepartureDate) return true;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 3);
    return new Date(booking.tripDepartureDate) > cutoff;
  }

  hasSubmitted(bookingId: string): boolean {
    return this.submittedBookingIds().has(bookingId);
  }
}
