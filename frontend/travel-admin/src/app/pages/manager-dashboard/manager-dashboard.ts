import { Component, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TripService } from '../../services/trip.service';
import type { ManagerStats } from '../../models/travel.models';

@Component({
  selector: 'app-manager-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './manager-dashboard.html',
  styleUrl: './manager-dashboard.css'
})
export class ManagerDashboardComponent implements OnInit {
  private tripService = inject(TripService);

  stats = signal<ManagerStats | null>(null);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.tripService.myStats().subscribe({
      next: s => { this.stats.set(s); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger les statistiques.'); this.loading.set(false); }
    });
  }
}
