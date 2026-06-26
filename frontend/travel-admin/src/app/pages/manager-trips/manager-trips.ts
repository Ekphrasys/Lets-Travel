import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TripService } from '../../services/trip.service';
import { AuthService } from '../../services/auth.service';
import { Trip } from '../../models/travel.models';

@Component({
  selector: 'app-manager-trips',
  imports: [RouterLink],
  templateUrl: './manager-trips.html',
  styleUrl: './manager-trips.css'
})
export class ManagerTripsComponent implements OnInit {
  auth = inject(AuthService);
  private tripService = inject(TripService);

  trips = signal<Trip[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.tripService.myTrips().subscribe({
      next: trips => { this.trips.set(trips); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger vos voyages.'); this.loading.set(false); }
    });
  }

  delete(id: string): void {
    if (!confirm('Supprimer ce voyage ?')) return;
    this.tripService.delete(id).subscribe(() =>
      this.trips.update(list => list.filter(t => t.id !== id))
    );
  }
}
