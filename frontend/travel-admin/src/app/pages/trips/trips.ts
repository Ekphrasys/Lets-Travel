import { Component, inject, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, of, takeUntil } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { TripService } from '../../services/trip.service';
import { BookingService } from '../../services/booking.service';
import { Trip } from '../../models/travel.models';

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
  suggestions = signal<string[]>([]);
  showSuggestions = signal(false);
  message = signal('');

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.load();

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

  book(tripId: string): void {
    this.bookingService.book(tripId).subscribe({
      next: () => {
        this.message.set('Réservation confirmée !');
        this.load();
      },
      error: () => this.message.set('Réservation échouée (plus de places ou paiement refusé).')
    });
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
