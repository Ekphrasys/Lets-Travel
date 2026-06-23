import { CurrencyPipe, DatePipe, SlicePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { Payment } from '../../models/travel.models';

@Component({
  selector: 'app-admin-payments',
  imports: [DatePipe, CurrencyPipe, SlicePipe, RouterLink],
  templateUrl: './admin-payments.html',
  styleUrl: './admin-payments.css'
})
export class AdminPaymentsComponent implements OnInit {
  private adminService = inject(AdminService);
  payments = signal<Payment[]>([]);

  ngOnInit(): void {
    this.adminService.listPayments().subscribe(p => this.payments.set(p));
  }

  deletePayment(id: string): void {
    if (!confirm('Supprimer ce paiement ?')) return;
    this.adminService.deletePayment(id).subscribe(() => {
      this.adminService.listPayments().subscribe(p => this.payments.set(p));
    });
  }
}
