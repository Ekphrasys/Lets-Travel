import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-payment-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './payment-form.html',
  styleUrl: './payment-form.css'
})
export class PaymentFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  isEdit = false;
  paymentId = '';

  form = this.fb.nonNullable.group({
    bookingId: ['', Validators.required],
    userId: ['', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    status: ['COMPLETED', Validators.required]
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.paymentId = id;
      this.adminService.getPayment(id).subscribe(payment => {
        this.form.patchValue({
          bookingId: payment.bookingId,
          userId: payment.userId,
          amount: payment.amount,
          status: payment.status
        });
      });
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    const data = this.form.getRawValue();
    const req = this.isEdit
      ? this.adminService.updatePayment(this.paymentId, { amount: data.amount, status: data.status })
      : this.adminService.createPayment({
          bookingId: data.bookingId,
          userId: data.userId,
          amount: data.amount
        });
    req.subscribe(() => this.router.navigate(['/admin/payments']));
  }
}
