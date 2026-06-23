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
}
