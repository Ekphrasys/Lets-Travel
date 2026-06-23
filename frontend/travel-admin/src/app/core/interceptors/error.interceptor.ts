import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        const isAuthRequest =
          req.url.includes('/api/auth/login') || req.url.includes('/api/auth/register');
        if (!isAuthRequest) {
          auth.logout();
        }
      }
      return throwError(() => err);
    })
  );
};
