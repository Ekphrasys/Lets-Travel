import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TripService } from '../../services/trip.service';

@Component({
  selector: 'app-trip-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './trip-form.html',
  styleUrl: './trip-form.css'
})
export class TripFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private tripService = inject(TripService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  isEdit = false;
  tripId = '';

  form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    originCity: ['', Validators.required],
    destinationCity: ['', Validators.required],
    departureDate: ['', Validators.required],
    price: [0, [Validators.required, Validators.min(0)]],
    seatsAvailable: [1, [Validators.required, Validators.min(1)]]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.tripId = id;
      this.tripService.list().subscribe(trips => {
        const trip = trips.find(t => t.id === id);
        if (trip) {
          this.form.patchValue({
            title: trip.title,
            originCity: trip.originCity,
            destinationCity: trip.destinationCity,
            departureDate: trip.departureDate,
            price: trip.price,
            seatsAvailable: trip.seatsAvailable
          });
        }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const data = this.form.getRawValue();
    const req = this.isEdit
      ? this.tripService.update(this.tripId, data)
      : this.tripService.create(data);
    req.subscribe(() => this.router.navigate(['/trips']));
  }
}
