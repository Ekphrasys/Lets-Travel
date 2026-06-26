import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CreateUserPayload, Payment, UpdatePaymentPayload, UpdateUserPayload, User } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  listUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.apiUrl}/api/users`);
  }

  getUser(id: string): Observable<User> {
    return this.http.get<User>(`${environment.apiUrl}/api/users/${id}`);
  }

  createUser(payload: CreateUserPayload): Observable<User> {
    return this.http.post<User>(`${environment.apiUrl}/api/users`, payload);
  }

  updateUser(id: string, payload: UpdateUserPayload): Observable<User> {
    return this.http.put<User>(`${environment.apiUrl}/api/users/${id}`, payload);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/users/${id}`);
  }

  listPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${environment.apiUrl}/api/payments`);
  }

  getPayment(id: string): Observable<Payment> {
    return this.http.get<Payment>(`${environment.apiUrl}/api/payments/${id}`);
  }

  createPayment(payload: { bookingId: string; userId: string; amount: number }): Observable<Payment> {
    return this.http.post<Payment>(`${environment.apiUrl}/api/payments`, payload);
  }

  updatePayment(id: string, payload: UpdatePaymentPayload): Observable<Payment> {
    return this.http.put<Payment>(`${environment.apiUrl}/api/payments/${id}`, payload);
  }

  deletePayment(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/payments/${id}`);
  }

  listReports(): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/api/users/reports`);
  }

  resolveReport(id: string): Observable<any> {
    return this.http.put<any>(`${environment.apiUrl}/api/users/reports/${id}/resolve`, {});
  }

  reportUser(payload: { reportedId: string; tripId?: string; reason: string }): Observable<any> {
    return this.http.post<any>(`${environment.apiUrl}/api/users/reports`, payload);
  }

  getReportCounts(userId: string): Observable<{ reportsFiled: number; reportsReceived: number }> {
    return this.http.get<{ reportsFiled: number; reportsReceived: number }>(`${environment.apiUrl}/api/users/${userId}/report-counts`);
  }

  listManagers(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.apiUrl}/api/users/managers`);
  }
}
