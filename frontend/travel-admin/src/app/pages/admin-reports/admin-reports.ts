import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { AdminReportView, AdminManagerReportView } from '../../models/travel.models';

type Tab = 'user' | 'manager';
type StatusFilter = 'ALL' | 'PENDING' | 'RESOLVED';

@Component({
  selector: 'app-admin-reports',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-reports.html',
  styleUrl: './admin-reports.css'
})
export class AdminReportsComponent implements OnInit {
  private adminService = inject(AdminService);

  activeTab = signal<Tab>('user');
  statusFilter = signal<StatusFilter>('ALL');
  loading = signal(true);

  userReports = signal<AdminReportView[]>([]);
  managerReports = signal<AdminManagerReportView[]>([]);

  filteredUserReports = computed(() => {
    const f = this.statusFilter();
    return f === 'ALL' ? this.userReports() : this.userReports().filter(r => r.status === f);
  });

  filteredManagerReports = computed(() => {
    const f = this.statusFilter();
    return f === 'ALL' ? this.managerReports() : this.managerReports().filter(r => r.status === f);
  });

  pendingUserCount    = computed(() => this.userReports().filter(r => r.status === 'PENDING').length);
  pendingManagerCount = computed(() => this.managerReports().filter(r => r.status === 'PENDING').length);
  totalPending        = computed(() => this.pendingUserCount() + this.pendingManagerCount());

  ngOnInit(): void {
    let done = 0;
    const check = () => { if (++done === 2) this.loading.set(false); };

    this.adminService.listReports().subscribe({
      next: d => { this.userReports.set(d); check(); },
      error: () => check()
    });
    this.adminService.listManagerReports().subscribe({
      next: d => { this.managerReports.set(d); check(); },
      error: () => check()
    });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.statusFilter.set('ALL');
  }

  setFilter(f: StatusFilter): void {
    this.statusFilter.set(f);
  }

  resolveUserReport(id: string): void {
    if (!confirm('Marquer ce signalement comme résolu ?')) return;
    this.adminService.resolveReport(id).subscribe(updated => {
      this.userReports.update(list =>
        list.map(r => r.id === updated.id ? updated : r)
      );
    });
  }

  resolveManagerReport(id: string): void {
    if (!confirm('Marquer ce signalement comme résolu ?')) return;
    this.adminService.resolveManagerReport(id).subscribe(updated => {
      this.managerReports.update(list =>
        list.map(r => r.id === updated.id ? updated : r)
      );
    });
  }

  roleLabel(role: string): string {
    const map: Record<string, string> = {
      TRAVEL_MANAGER: 'Manager', ADMIN: 'Admin', TRAVELER: 'Voyageur', USER: 'Voyageur'
    };
    return map[role] ?? role;
  }
}
