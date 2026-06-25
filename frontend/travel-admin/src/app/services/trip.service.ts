import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import type { ManagerStats, Trip, TripAnalytics } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class TripService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/travels`;

  list(): Observable<Trip[]> {
    return this.http.get<Trip[]>(this.base);
  }

  getById(id: string): Observable<Trip> {
    return this.http.get<Trip>(`${this.base}/${id}`);
  }

  myTrips(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${this.base}/my`);
  }

  myStats(): Observable<ManagerStats> {
    return this.http.get<ManagerStats>(`${this.base}/stats`);
  }

  myAnalytics(): Observable<TripAnalytics[]> {
    return this.http.get<TripAnalytics[]>(`${this.base}/analytics`);
  }

  create(trip: Omit<Trip, 'id' | 'status' | 'managerId'>): Observable<Trip> {
    return this.http.post<Trip>(this.base, trip);
  }

  update(id: string, trip: Omit<Trip, 'id' | 'status' | 'managerId'>): Observable<Trip> {
    return this.http.put<Trip>(`${this.base}/${id}`, trip);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
