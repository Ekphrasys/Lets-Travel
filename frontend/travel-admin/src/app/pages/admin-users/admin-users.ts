import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { User } from '../../models/travel.models';

@Component({
  selector: 'app-admin-users',
  imports: [RouterLink],
  templateUrl: './admin-users.html',
  styleUrl: './admin-users.css'
})
export class AdminUsersComponent implements OnInit {
  private adminService = inject(AdminService);
  users = signal<User[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.adminService.listUsers().subscribe(u => this.users.set(u));
  }

  deleteUser(id: string): void {
    if (!confirm('Supprimer cet utilisateur ?')) return;
    this.adminService.deleteUser(id).subscribe(() => this.load());
  }
}
