import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * LoginComponent
 *
 * Provides the user login interface for both Customers and Service Center personnel.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  email = '';
  password = '';
  showPassword = false;
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  toggleShowPassword(): void {
    this.showPassword = !this.showPassword;
  }

  constructor(private authService: AuthService, private router: Router) {
    // If already logged in, redirect to appropriate dashboard
    if (this.authService.isAuthenticated()) {
      this.authService.navigateToRoleDashboard();
    }
  }

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.errorMessage.set('Please enter both email and password.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.authService.navigateToRoleDashboard(response.role);
      },
      error: (err) => {
        this.isLoading.set(false);
        const serverMsg = err.error?.message || 'Invalid email or password. Please try again.';
        this.errorMessage.set(serverMsg);
      }
    });
  }

  fillCustomerDemo(): void {
    this.email = 'rahul@example.com';
    this.password = 'password123';
  }

  fillAdminDemo(): void {
    this.email = 'admin@servicemate.com';
    this.password = 'admin123';
  }
}
