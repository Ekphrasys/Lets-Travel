import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-privacy',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './privacy.html',
  styleUrl: './privacy.css'
})
export class PrivacyComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private auth = inject(AuthService);

  loading = signal(false);
  deleteLoading = signal(false);
  successMessage = signal('');
  errorMessage = signal('');
  showConfirm = signal(false);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required]
  });

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (u) => {
        this.form.patchValue({ email: u.email, firstName: u.firstName, lastName: u.lastName });
      },
      error: () => {}
    });
  }

  updateProfile(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');
    this.userService.updateMe(this.form.getRawValue()).subscribe({
      next: () => {
        this.successMessage.set('Profil mis à jour avec succès.');
        this.loading.set(false);
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || 'Impossible de mettre à jour le profil.');
        this.loading.set(false);
      }
    });
  }

  exportData(): void {
    this.userService.exportMyData().subscribe({
      next: (data) => {
        const blob = new Blob([data], { type: 'application/json' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'data-export.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => {
        alert(err.error?.message || "Impossible d'exporter vos données.");
      }
    });
  }

  deleteAccount(): void {
    this.deleteLoading.set(true);
    this.userService.deleteMe().subscribe({
      next: () => {
        this.auth.logout();
      },
      error: (err: any) => {
        alert(err.error?.message || 'Impossible de supprimer votre compte.');
        this.deleteLoading.set(false);
        this.showConfirm.set(false);
      }
    });
  }
}
