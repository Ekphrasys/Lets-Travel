import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BookingService } from '../../services/booking.service';
import { TripService } from '../../services/trip.service';
import { AdminService } from '../../services/admin.service';
import { Booking, Trip, User } from '../../models/travel.models';

interface SubscriberRow {
  booking: Booking;
  user: User | null;
  showProfile: boolean;
}

@Component({
  selector: 'app-manager-subscribers',
  imports: [RouterLink, DatePipe],
  templateUrl: './manager-subscribers.html',
  styleUrl: './manager-subscribers.css'
})
export class ManagerSubscribersComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private bookingService = inject(BookingService);
  private tripService = inject(TripService);
  private adminService = inject(AdminService);

  trip = signal<Trip | null>(null);
  rows = signal<SubscriberRow[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    const tripId = this.route.snapshot.paramMap.get('id')!;
    this.tripService.getById(tripId).subscribe(t => this.trip.set(t));
    this.bookingService.tripSubscribers(tripId).subscribe({
      next: bookings => {
        if (!bookings.length) {
          this.rows.set([]);
          this.loading.set(false);
          return;
        }
        const userRequests = bookings.map(b =>
          this.adminService.getUser(b.userId).pipe(catchError(() => of(null)))
        );
        forkJoin(userRequests).subscribe(users => {
          this.rows.set(bookings.map((b, i) => ({ booking: b, user: users[i], showProfile: false })));
          this.loading.set(false);
        });
      },
      error: () => { this.error.set('Impossible de charger les abonnés.'); this.loading.set(false); }
    });
  }

  toggleProfile(row: SubscriberRow): void {
    row.showProfile = !row.showProfile;
  }

  unsubscribe(row: SubscriberRow): void {
    const name = row.user ? `${row.user.firstName} ${row.user.lastName}` : 'ce voyageur';
    if (!confirm(`Désabonner ${name} de ce voyage ?`)) return;
    this.bookingService.cancel(row.booking.id).subscribe({
      next: updated => {
        this.rows.update(list =>
          list.map(r => r.booking.id === updated.id ? { ...r, booking: updated } : r)
        );
      },
      error: () => alert('Erreur lors du désabonnement.')
    });
  }
}
