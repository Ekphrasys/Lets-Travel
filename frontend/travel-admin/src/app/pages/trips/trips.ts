import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, CurrencyPipe } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { TripService } from '../../services/trip.service';
import { BookingService } from '../../services/booking.service';
import { Trip } from '../../models/travel.models';

@Component({
  selector: 'app-trips',
  imports: [RouterLink, DatePipe, CurrencyPipe],
  templateUrl: './trips.html',
  styleUrl: './trips.css'
})
export class TripsComponent implements OnInit {
  auth = inject(AuthService);
  private tripService = inject(TripService);
  private bookingService = inject(BookingService);

  trips = signal<Trip[]>([]);
  message = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.tripService.list().subscribe(t => this.trips.set(t));
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

  deleteTrip(id: string): void {
    if (!confirm('Supprimer ce voyage ?')) return;
    this.tripService.delete(id).subscribe(() => this.load());
  }
}
