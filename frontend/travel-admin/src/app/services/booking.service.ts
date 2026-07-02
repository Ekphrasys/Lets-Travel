import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Booking, RoutePath } from '../models/travel.models';

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

  searchRoutes(origin: string, destination: string): Observable<RoutePath[]> {
    return this.http.get<RoutePath[]>(
      `${environment.apiUrl}/api/travels/routes/search?origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`
    );
  }
}
