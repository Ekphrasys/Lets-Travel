import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe, DecimalPipe, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ManagerService } from '../../services/manager.service';
import { AuthService } from '../../services/auth.service';
import type { ManagerProfile, ReportDetail } from '../../models/travel.models';

@Component({
  selector: 'app-manager-profile',
  imports: [DatePipe, DecimalPipe, CurrencyPipe, FormsModule],
  templateUrl: './manager-profile.html',
  styleUrl: './manager-profile.css'
})
export class ManagerProfileComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private managerService = inject(ManagerService);
  auth = inject(AuthService);

  profile = signal<ManagerProfile | null>(null);
  loading = signal(true);
  error = signal('');

  myReports = signal<ReportDetail[]>([]);
  reportsLoading = signal(false);

  reportReason = signal('');
  reportSent = signal(false);
  reportError = signal('');
  reportLoading = signal(false);

  private managerId = '';

  isOwnProfile = false;

  ngOnInit(): void {
    this.managerId = this.route.snapshot.paramMap.get('managerId') ?? '';
    this.isOwnProfile = this.auth.currentUser()?.sub === this.managerId;

    this.managerService.getProfile(this.managerId).subscribe({
      next: p => { this.profile.set(p); this.loading.set(false); },
      error: () => { this.error.set('Manager introuvable.'); this.loading.set(false); }
    });

    if (this.isOwnProfile) {
      this.reportsLoading.set(true);
      this.managerService.getMyReports().subscribe({
        next: r => { this.myReports.set(r); this.reportsLoading.set(false); },
        error: () => this.reportsLoading.set(false)
      });
    }
  }

  stars(rating: number): string {
    const full = Math.round(rating);
    return '★'.repeat(full) + '☆'.repeat(5 - full);
  }

  submitReport(): void {
    const reason = this.reportReason().trim();
    if (!reason) return;
    this.reportLoading.set(true);
    this.reportError.set('');
    this.managerService.report(this.managerId, reason).subscribe({
      next: () => {
        this.reportSent.set(true);
        this.reportLoading.set(false);
      },
      error: (err) => {
        this.reportError.set(
          err.status === 409
            ? 'Vous avez déjà signalé ce manager.'
            : 'Impossible d\'envoyer le signalement.'
        );
        this.reportLoading.set(false);
      }
    });
  }
}
