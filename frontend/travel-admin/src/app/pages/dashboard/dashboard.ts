import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { TripService } from '../../services/trip.service';
import { BookingService } from '../../services/booking.service';
import { AdminService } from '../../services/admin.service';
import { AdminReportView, Trip, Booking, User } from '../../models/travel.models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  tripService = inject(TripService);
  bookingService = inject(BookingService);
  adminService = inject(AdminService);

  // Common data
  trips = signal<Trip[]>([]);
  bookings = signal<Booking[]>([]);

  // Traveler features
  searchQuery = '';
  searchResults = signal<Trip[]>([]);
  autocompleteSuggestions = signal<string[]>([]);
  recommendations = signal<Trip[]>([]);
  managers = signal<User[]>([]);
  reportCounts = signal({ reportsFiled: 0, reportsReceived: 0 });
  
  // Modals / Actions
  showFeedbackModal = false;
  selectedTripForFeedback: Trip | null = null;
  ratingValue = 5;
  commentValue = '';
  showReportModal = false;
  selectedUserForReport: User | null = null;
  reportReason = '';

  // Manager features
  managerDashboardData = signal<any>(null);
  managerTrips = signal<Trip[]>([]);
  showSubscribersModal = false;
  selectedTripForSubscribers: Trip | null = null;
  subscribersList = signal<any[]>([]);

  // Admin features
  adminDashboardData = signal<any>(null);
  adminReports = signal<AdminReportView[]>([]);

  monthlyIncomeEntries = computed(() => {
    const data = this.adminDashboardData();
    if (!data?.incomeByMonth) return [];
    return Object.entries(data.incomeByMonth as Record<string, number>)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([month, income]) => ({ month, income: Number(income) }));
  });

  maxMonthlyIncome = computed(() => {
    const entries = this.monthlyIncomeEntries();
    return entries.length ? Math.max(...entries.map(e => e.income), 1) : 1;
  });

  // UI state
  loading = false;
  successMessage = '';
  errorMessage = '';

  ngOnInit(): void {
    const user = this.auth.currentUser();
    if (!user) return;

    this.tripService.list().subscribe((t: Trip[]) => this.trips.set(t));

    if (this.auth.isAdmin()) {
      this.loadAdminData();
    } else if (this.auth.isManager()) {
      this.loadManagerData(user.sub);
    } else {
      this.loadTravelerData(user.sub);
    }
  }

  // --- Traveler Logic ---
  loadTravelerData(userId: string): void {
    this.bookingService.myBookings().subscribe((b: Booking[]) => this.bookings.set(b));
    this.tripService.recommendations().subscribe((r: Trip[]) => this.recommendations.set(r));
    this.adminService.listManagers().subscribe((m: User[]) => this.managers.set(m));
    this.adminService.getReportCounts(userId).subscribe((c: { reportsFiled: number; reportsReceived: number }) => this.reportCounts.set(c));
  }

  onSearchChange(): void {
    if (!this.searchQuery.trim()) {
      this.searchResults.set([]);
      this.autocompleteSuggestions.set([]);
      return;
    }
    this.tripService.autocomplete(this.searchQuery).subscribe((suggestions: string[]) => {
      this.autocompleteSuggestions.set(suggestions);
    });
  }

  selectSuggestion(suggestion: string): void {
    this.searchQuery = suggestion;
    this.autocompleteSuggestions.set([]);
    this.triggerSearch();
  }

  triggerSearch(): void {
    if (!this.searchQuery.trim()) return;
    this.tripService.search(this.searchQuery).subscribe((results: Trip[]) => {
      this.searchResults.set(results);
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults.set([]);
    this.autocompleteSuggestions.set([]);
  }

  bookTrip(trip: Trip, paymentMethod: string): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.bookingService.book(trip.id, paymentMethod).subscribe({
      next: () => {
        this.successMessage = `Réservation confirmée pour ${trip.title} !`;
        this.ngOnInit(); // reload all
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || "Le paiement a échoué ou plus de places.";
      }
    });
  }

  cancelBooking(booking: Booking): void {
    if (!confirm("Annuler cette réservation ?")) return;
    this.errorMessage = '';
    this.successMessage = '';
    this.bookingService.cancel(booking.id).subscribe({
      next: () => {
        this.successMessage = "Réservation annulée avec succès (remboursée).";
        this.ngOnInit();
      },
      error: (err: any) => {
        this.errorMessage = err.error?.message || "Impossible d'annuler cette réservation.";
      }
    });
  }

  openFeedbackModal(trip: Trip): void {
    this.selectedTripForFeedback = trip;
    this.ratingValue = 5;
    this.commentValue = '';
    this.showFeedbackModal = true;
  }

  submitFeedback(): void {
    if (!this.selectedTripForFeedback) return;
    this.tripService.leaveFeedback(this.selectedTripForFeedback.id, this.ratingValue, this.commentValue).subscribe({
      next: () => {
        this.successMessage = "Avis enregistré !";
        this.showFeedbackModal = false;
        this.ngOnInit();
      },
      error: (err: any) => {
        alert(err.error?.message || "Impossible de laisser un avis.");
      }
    });
  }

  openReportModal(user: User): void {
    this.selectedUserForReport = user;
    this.reportReason = '';
    this.showReportModal = true;
  }

  submitReport(): void {
    if (!this.selectedUserForReport) return;
    this.adminService.reportUser({
      reportedId: this.selectedUserForReport.id,
      reason: this.reportReason
    }).subscribe({
      next: () => {
        alert("Signalement envoyé à l'administrateur.");
        this.showReportModal = false;
        this.ngOnInit();
      },
      error: (err: any) => {
        alert(err.error?.message || "Erreur de signalement.");
      }
    });
  }

  // --- Manager Logic ---
  loadManagerData(managerId: string): void {
    this.tripService.managerDashboard(managerId).subscribe((data: any) => this.managerDashboardData.set(data));
    this.tripService.list().subscribe((allTrips: Trip[]) => {
      this.managerTrips.set(allTrips.filter(t => t.managerId === managerId));
    });
  }

  openSubscribersModal(trip: Trip): void {
    this.selectedTripForSubscribers = trip;
    this.tripService.subscribers(trip.id).subscribe((subs: any[]) => {
      this.subscribersList.set(subs);
      this.showSubscribersModal = true;
    });
  }

  unsubscribeTraveler(userId: string): void {
    if (!this.selectedTripForSubscribers) return;
    if (!confirm("Désabonner ce voyageur de cette offre ?")) return;
    this.tripService.unsubscribe(this.selectedTripForSubscribers.id, userId).subscribe(() => {
      alert("Voyageur désabonné et remboursé.");
      this.openSubscribersModal(this.selectedTripForSubscribers!);
      this.loadManagerData(this.auth.currentUser()!.sub);
    });
  }

  // --- Admin Logic ---
  loadAdminData(): void {
    this.tripService.adminDashboard().subscribe((data: any) => this.adminDashboardData.set(data));
    this.adminService.listReports().subscribe((reports: any[]) => this.adminReports.set(reports));
  }

  floorRating(value: number): number {
    return Math.min(5, Math.max(0, Math.floor(value)));
  }

  resolveReport(reportId: string): void {
    if (!confirm("Marquer ce signalement comme résolu ?")) return;
    this.adminService.resolveReport(reportId).subscribe(() => {
      alert("Signalement résolu.");
      this.loadAdminData();
    });
  }
}
