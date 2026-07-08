import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingService } from '../../services/booking.service';
import { FeedbackService } from '../../services/feedback.service';
import { AdminService } from '../../services/admin.service';
import { Booking, Payment } from '../../models/travel.models';

@Component({
  selector: 'app-bookings',
  imports: [CommonModule, DatePipe, FormsModule],
  templateUrl: './bookings.html',
  styleUrl: './bookings.css'
})
export class BookingsComponent implements OnInit {
  private bookingService = inject(BookingService);
  private feedbackService = inject(FeedbackService);
  private adminService = inject(AdminService);

  bookings = signal<Booking[]>([]);
  payments = signal<Map<string, Payment>>(new Map());
  message = signal('');
  loadingPayments = signal(false);

  activeFeedbackBookingId = signal<string | null>(null);
  feedbackRating = signal(0);
  feedbackComment = signal('');
  submittedBookingIds = signal<Set<string>>(new Set());

  readonly stars = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loadingPayments.set(true);
    this.bookingService.myBookings().subscribe((b: Booking[]) => {
      this.bookings.set(b);
      this.feedbackService.myFeedbacks().subscribe(feedbacks => {
        const reviewedTripIds = new Set(feedbacks.map(f => f.tripId));
        const reviewedBookingIds = b.filter(booking => reviewedTripIds.has(booking.tripId)).map(booking => booking.id);
        this.submittedBookingIds.set(new Set(reviewedBookingIds));
      });
      const promises: Promise<void>[] = [];
      b.forEach(booking => {
        if (booking.paymentId) {
          promises.push(this.adminService.getPayment(booking.paymentId).toPromise().then(
            (p) => p ? this.payments.update(m => new Map(m).set(booking.id, p)) : Promise.resolve(),
            () => Promise.resolve()
          ));
        }
      });
      Promise.all(promises).finally(() => this.loadingPayments.set(false));
    });
  }

  paymentFor(bookingId: string): Payment | null {
    return this.payments().get(bookingId) || null;
  }

  paymentMethodLabel(method: string | undefined): string {
    const icons: Record<string, string> = { CARD: '💳', PAYPAL: '💰', BANK_TRANSFER: '🏦' };
    const labels: Record<string, string> = { CARD: 'Carte', PAYPAL: 'PayPal', BANK_TRANSFER: 'Virement' };
    if (!method) return '';
    return `${icons[method] ?? ''} ${labels[method] ?? method}`;
  }

  canCancel(booking: Booking): boolean {
    if (!booking.tripDepartureDate) return true;
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 3);
    return new Date(booking.tripDepartureDate) > cutoff;
  }

  cancel(id: string): void {
    if (!confirm('Annuler cette réservation ?')) return;
    this.message.set('');
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
      error: () => this.message.set("Impossible d'envoyer l'avis.")
    });
  }

  hasSubmitted(bookingId: string): boolean {
    return this.submittedBookingIds().has(bookingId);
  }
}
