import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { User } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  getMe(): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/api/users/me`);
  }

  updateMe(data: { email: string; firstName: string; lastName: string }): Observable<User> {
    return this.http.put<User>(`${environment.apiUrl}/api/users/me`, data);
  }

  deleteMe(): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/users/me`);
  }

  exportMyData(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/api/users/me/export`, { responseType: 'text' as const });
  }
}
