import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../models/travel.models';

export interface JwtPayload {
  sub: string;
  email: string;
  role: 'USER' | 'TRAVEL_MANAGER' | 'ADMIN' | 'MANAGER' | 'TRAVELER';
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  currentUser = signal<JwtPayload | null>(null);

  constructor() {
    this.restoreSession();
  }

  register(data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/auth/register`, data).pipe(
      tap(res => this.saveSession(res))
    );
  }

  login(credentials: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, credentials).pipe(
      tap(res => this.saveSession(res))
    );
  }

  logout(): void {
    localStorage.removeItem('token');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    const user = this.currentUser();
    return user !== null && user.exp * 1000 > Date.now();
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  isTravelManager(): boolean {
    const role = this.currentUser()?.role;
    return role === 'TRAVEL_MANAGER' || role === 'MANAGER';
  }

  isManager(): boolean {
    const role = this.currentUser()?.role;
    return role === 'TRAVEL_MANAGER' || role === 'MANAGER';
  }

  isTraveler(): boolean {
    const role = this.currentUser()?.role;
    return role === 'TRAVELER' || role === 'USER';
  }

  isManagerOrAdmin(): boolean {
    const role = this.currentUser()?.role;
    return role === 'ADMIN' || role === 'TRAVEL_MANAGER' || role === 'MANAGER';
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem('token', res.token);
    this.restoreSession();
  }

  private restoreSession(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.currentUser.set(null);
      return;
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1])) as JwtPayload;
      if (payload.exp * 1000 < Date.now()) {
        this.logout();
        return;
      }
      this.currentUser.set(payload);
    } catch {
      this.logout();
    }
  }
}
