import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import type { TravelerProfile } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class TravelerService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/travelers`;

  getProfile(travelerId: string): Observable<TravelerProfile> {
    return this.http.get<TravelerProfile>(`${this.base}/${travelerId}/profile`);
  }
}
