import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Trip } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class TripService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/travels`;

  list(): Observable<Trip[]> {
    return this.http.get<Trip[]>(this.base);
  }

  create(trip: Omit<Trip, 'id' | 'status'>): Observable<Trip> {
    return this.http.post<Trip>(this.base, trip);
  }

  update(id: string, trip: Omit<Trip, 'id' | 'status'>): Observable<Trip> {
    return this.http.put<Trip>(`${this.base}/${id}`, trip);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  search(query: string): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${this.base}/search?query=${encodeURIComponent(query)}`);
  }

  autocomplete(query: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/search/autocomplete?query=${encodeURIComponent(query)}`);
  }

  recommendations(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${this.base}/recommendations`);
  }

  leaveFeedback(tripId: string, rating: number, comment: string): Observable<any> {
    return this.http.post<any>(`${this.base}/${tripId}/feedback`, { rating, comment });
  }

  feedbacks(tripId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/${tripId}/feedbacks`);
  }

  adminDashboard(): Observable<any> {
    return this.http.get<any>(`${this.base}/admin/dashboard`);
  }

  managerDashboard(managerId: string): Observable<any> {
    return this.http.get<any>(`${this.base}/managers/${managerId}/dashboard`);
  }

  subscribers(tripId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/${tripId}/subscribers`);
  }

  unsubscribe(tripId: string, userId: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${tripId}/unsubscribe/${userId}`, {});
  }

  allFeedbacks(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/feedbacks`);
  }
}
