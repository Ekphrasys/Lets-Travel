import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TripService } from '../../services/trip.service';
import { BookingService } from '../../services/booking.service';
import { Trip, Booking } from '../../models/travel.models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private tripService = inject(TripService);
  private bookingService = inject(BookingService);

  trips = signal<Trip[]>([]);
  bookings = signal<Booking[]>([]);

  ngOnInit(): void {
    this.tripService.list().subscribe(t => this.trips.set(t));
    this.bookingService.myBookings().subscribe(b => this.bookings.set(b));
  }
}
