import { Routes } from '@angular/router';
import { authGuard, guestGuard, adminGuard } from './core/guards/auth.guard';
import { LoginComponent } from './pages/login/login';
import { RegisterComponent } from './pages/register/register';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { TripsComponent } from './pages/trips/trips';
import { TripFormComponent } from './pages/trip-form/trip-form';
import { BookingsComponent } from './pages/bookings/bookings';
import { RoutesSearchComponent } from './pages/routes-search/routes-search';
import { AdminUsersComponent } from './pages/admin-users/admin-users';
import { AdminPaymentsComponent } from './pages/admin-payments/admin-payments';
import { UserFormComponent } from './pages/user-form/user-form';
import { PaymentFormComponent } from './pages/payment-form/payment-form';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'trips', component: TripsComponent, canActivate: [authGuard] },
  { path: 'trips/new', component: TripFormComponent, canActivate: [adminGuard] },
  { path: 'trips/:id/edit', component: TripFormComponent, canActivate: [adminGuard] },
  { path: 'bookings', component: BookingsComponent, canActivate: [authGuard] },
  { path: 'routes', component: RoutesSearchComponent },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [adminGuard] },
  { path: 'admin/users/new', component: UserFormComponent, canActivate: [adminGuard] },
  { path: 'admin/users/:id/edit', component: UserFormComponent, canActivate: [adminGuard] },
  { path: 'admin/payments', component: AdminPaymentsComponent, canActivate: [adminGuard] },
  { path: 'admin/payments/new', component: PaymentFormComponent, canActivate: [adminGuard] },
  { path: 'admin/payments/:id/edit', component: PaymentFormComponent, canActivate: [adminGuard] },
  { path: '**', redirectTo: '' }
];
