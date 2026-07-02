import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { TripService } from '../../services/trip.service';
import { BookingService } from '../../services/booking.service';
import { Trip, PaymentMethod, PAYMENT_METHODS } from '../../models/travel.models';

@Component({
  selector: 'app-trips',
  imports: [RouterLink, DatePipe, CurrencyPipe, ReactiveFormsModule],
  templateUrl: './trips.html',
  styleUrl: './trips.css'
})
export class TripsComponent implements OnInit, OnDestroy {
  auth = inject(AuthService);
  private tripService = inject(TripService);
  private bookingService = inject(BookingService);

  searchControl = new FormControl('');

  private allTrips = signal<Trip[]>([]);
  trips = signal<Trip[]>([]);
  suggestedTrips = signal<Trip[]>([]);
  suggestions = signal<string[]>([]);
  showSuggestions = signal(false);
  suggestionsOpen = signal(true);
  message = signal('');

  // Payment modal state — step 1: pick method, step 2: enter details
  pendingBookingTrip = signal<Trip | null>(null);
  paymentStep = signal<1 | 2>(1);
  selectedPaymentMethod = signal<PaymentMethod>('CARD');
  pendingClientSecret = signal<string | null>(null);
  pendingBookingId = signal<string | null>(null);
  isConfirming = signal(false);

  // Mock card / PayPal form fields
  cardNumber = signal('');
  cardExpiry = signal('');
  cardCvc = signal('');
  paypalEmail = signal('');

  readonly paymentMethods = PAYMENT_METHODS;

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.load();
    if (this.auth.isAuthenticated()) {
      this.tripService.suggestions().subscribe(s =>
        this.suggestedTrips.set(s.filter(t => this.isBookable(t)))
      );
    }

    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        const query = (q ?? '').trim();
        if (!query) {
          this.suggestions.set([]);
          this.trips.set(this.allTrips());
          return of(null);
        }
        this.tripService.autocomplete(query).subscribe(s => this.suggestions.set(s));
        return this.tripService.search(query);
      }),
      takeUntil(this.destroy$)
    ).subscribe(results => {
      if (results !== null) {
        this.trips.set(results);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  load(): void {
    this.tripService.list().subscribe(t => {
      this.allTrips.set(t);
      this.trips.set(t);
    });
  }

  selectSuggestion(suggestion: string): void {
    this.searchControl.setValue(suggestion);
    this.suggestions.set([]);
    this.showSuggestions.set(false);
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.suggestions.set([]);
    this.trips.set(this.allTrips());
  }

  hideSuggestions(): void {
    setTimeout(() => this.showSuggestions.set(false), 150);
  }

  book(trip: Trip): void {
    this.pendingBookingTrip.set(trip);
    this.paymentStep.set(1);
    this.selectedPaymentMethod.set('CARD');
    this.cardNumber.set('');
    this.cardExpiry.set('');
    this.cardCvc.set('');
    this.paypalEmail.set('');
  }

  proceedToPaymentDetails(): void {
    const trip = this.pendingBookingTrip();
    if (!trip) return;
    this.bookingService.book(trip.id, this.selectedPaymentMethod()).subscribe({
      next: (booking) => {
        this.pendingBookingId.set(booking.id);
        this.pendingClientSecret.set(booking.clientSecret ?? null);
        this.paymentStep.set(2);
      },
      error: () => {
        this.closePaymentModal();
        this.message.set('Réservation échouée (plus de places disponibles).');
      }
    });
  }

  confirmPayment(): void {
    const bookingId = this.pendingBookingId();
    const clientSecret = this.pendingClientSecret();
    if (!bookingId || !clientSecret || this.isConfirming()) return;
    this.isConfirming.set(true);
    this.bookingService.confirmPayment(bookingId, clientSecret).subscribe({
      next: () => {
        this.closePaymentModal();
        this.message.set('Paiement réussi ! Votre réservation est confirmée.');
        this.load();
      },
      error: () => {
        this.isConfirming.set(false);
        this.message.set('Paiement refusé. Veuillez réessayer.');
      }
    });
  }

  closePaymentModal(): void {
    this.pendingBookingTrip.set(null);
    this.pendingBookingId.set(null);
    this.pendingClientSecret.set(null);
    this.paymentStep.set(1);
    this.isConfirming.set(false);
  }

  isPaymentFormValid(): boolean {
    if (this.selectedPaymentMethod() === 'PAYPAL') {
      return this.paypalEmail().includes('@');
    }
    return this.cardNumber().replace(/\s/g, '').length === 16
      && this.cardExpiry().length >= 4
      && this.cardCvc().length >= 3;
  }

  formatCardNumber(raw: string): void {
    const digits = raw.replace(/\D/g, '').slice(0, 16);
    this.cardNumber.set(digits.replace(/(.{4})/g, '$1 ').trim());
  }

  formatExpiry(raw: string): void {
    const digits = raw.replace(/\D/g, '').slice(0, 4);
    this.cardExpiry.set(digits.length >= 3 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits);
  }

  formatCvc(raw: string): void {
    this.cardCvc.set(raw.replace(/\D/g, '').slice(0, 4));
  }

  isBookable(trip: Trip): boolean {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 3);
    return trip.status === 'ACTIVE' && trip.seatsAvailable > 0 && new Date(trip.departureDate) > cutoff;
  }

  canBook(trip: Trip): boolean {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() + 3);
    return new Date(trip.departureDate) > cutoff;
  }

  deleteTrip(id: string): void {
    if (!confirm('Supprimer ce voyage ?')) return;
    this.tripService.delete(id).subscribe(() => this.load());
  }
}
