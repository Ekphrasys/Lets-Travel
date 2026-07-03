import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import { Booking, Payment, RoutePath } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/bookings`;

  myBookings(): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/me`);
  }

  tripSubscribers(tripId: string): Observable<Booking[]> {
    return this.http.get<Booking[]>(`${this.base}/trip/${tripId}`);
  }

  book(tripId: string, paymentMethod?: string): Observable<Booking> {
    return this.http.post<Booking>(this.base, { tripId, paymentMethod });
  }

  confirmPayment(bookingId: string, clientSecret: string): Observable<Booking> {
    return this.http.post<Booking>(`${this.base}/${bookingId}/confirm-payment`, { clientSecret });
  }

  cancel(id: string): Observable<Booking> {
    return this.http.delete<Booking>(`${this.base}/${id}`);
  }

  paymentForBooking(bookingId: string): Observable<Payment> {
    const pid = localStorage.getItem(`bookingPayment:${bookingId}`);
    if (pid) {
      return this.http.get<Payment>(`${environment.apiUrl}/api/payments/${pid}`);
    }
    return this.http.get<Payment[]>(`${environment.apiUrl}/api/payments?bookingId=${bookingId}`).pipe(
      map((list: Payment[]) => {
        const found = list[0];
        if (found) {
          localStorage.setItem(`bookingPayment:${bookingId}`, found.id);
          return found;
        }
        throw new Error('No payment');
      })
    );
  }

  refreshBooking(id: string): Observable<Booking> {
    return this.http.get<Booking>(`${this.base}/${id}`);
  }

  searchRoutes(origin: string, destination: string): Observable<RoutePath[]> {
    return this.http.get<RoutePath[]>(
      `${environment.apiUrl}/api/travels/routes/search?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`
    );
  }
}
