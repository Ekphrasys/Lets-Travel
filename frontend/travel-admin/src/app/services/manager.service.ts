import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import type { ManagerProfile, ReportDetail } from '../models/travel.models';

@Injectable({ providedIn: 'root' })
export class ManagerService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/managers`;

  getProfile(managerId: string): Observable<ManagerProfile> {
    return this.http.get<ManagerProfile>(`${this.base}/${managerId}/profile`);
  }

  getMyReports(): Observable<ReportDetail[]> {
    return this.http.get<ReportDetail[]>(`${this.base}/my/reports`);
  }

  report(managerId: string, reason: string): Observable<unknown> {
    return this.http.post(`${this.base}/${managerId}/reports`, { reason });
  }
}
