import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { BookingService } from '../../services/booking.service';
import { Booking } from '../../models/travel.models';

@Component({
  selector: 'app-bookings',
  imports: [DatePipe],
  templateUrl: './bookings.html',
  styleUrl: './bookings.css'
})
export class BookingsComponent implements OnInit {
  private bookingService = inject(BookingService);
  bookings = signal<Booking[]>([]);
  message = signal('');

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
}
