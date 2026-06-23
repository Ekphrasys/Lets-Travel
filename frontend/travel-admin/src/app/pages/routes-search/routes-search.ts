import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { BookingService } from '../../services/booking.service';
import { RoutePath } from '../../models/travel.models';

@Component({
  selector: 'app-routes-search',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './routes-search.html',
  styleUrl: './routes-search.css'
})
export class RoutesSearchComponent {
  private fb = inject(FormBuilder);
  private bookingService = inject(BookingService);

  routes = signal<RoutePath[]>([]);
  searched = signal(false);

  form = this.fb.nonNullable.group({
    origin: ['Paris', Validators.required],
    destination: ['Tokyo', Validators.required]
  });

  search(): void {
    if (this.form.invalid) return;
    const { origin, destination } = this.form.getRawValue();
    this.bookingService.searchRoutes(origin, destination).subscribe(r => {
      this.routes.set(r);
      this.searched.set(true);
    });
  }
}
