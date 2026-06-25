import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Feedback } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/feedbacks`;

  submit(tripId: string, rating: number, comment: string): Observable<Feedback> {
    return this.http.post<Feedback>(this.base, { tripId, rating, comment });
  }

  byTrip(tripId: string): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/trip/${tripId}`);
  }

  myTripsFeedback(): Observable<Feedback[]> {
    return this.http.get<Feedback[]>(`${this.base}/my-trips`);
  }
}
