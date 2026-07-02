import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Payment } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/payments`;

  myPayments(): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.base}/me`);
  }
}
