import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * RegisterComponent
 *
 * Provides the public user registration interface with comprehensive field-level validation,
 * show/hide password toggles, and server error mapping.
 * Strictly registers users with the CUSTOMER role.
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  name = '';
  email = '';
  phone = '';
  password = '';
  confirmPassword = '';

  showPassword = false;
  showConfirmPassword = false;

  isSubmitted = false;
  touched: { [key: string]: boolean } = {};
  backendErrors: { [key: string]: string } = {};

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isAuthenticated()) {
      this.authService.navigateToRoleDashboard();
    }
  }

  toggleShowPassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleShowConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  markTouched(field: string): void {
    this.touched[field] = true;
  }

  onFieldChange(field: string): void {
    delete this.backendErrors[field];
    this.errorMessage.set(null);
  }

  getNameError(): string | null {
    if (this.backendErrors['name']) {
      return this.backendErrors['name'];
    }
    const val = this.name.trim();
    if (!val) {
      return 'Full name is required.';
    }
    if (val.length < 2) {
      return 'Full name must contain at least 2 characters.';
    }
    return null;
  }

  getEmailError(): string | null {
    if (this.backendErrors['email']) {
      return this.backendErrors['email'];
    }
    const val = this.email.trim();
    if (!val) {
      return 'Email address is required.';
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(val)) {
      return 'Please enter a valid email address.';
    }
    return null;
  }

  getPhoneError(): string | null {
    if (this.backendErrors['phone']) {
      return this.backendErrors['phone'];
    }
    const val = this.phone.trim();
    if (!val) {
      return null; // Phone is optional
    }
    const phoneRegex = /^\d{10}$/;
    if (!phoneRegex.test(val)) {
      return 'Please enter a valid 10-digit phone number.';
    }
    return null;
  }

  getPasswordError(): string | null {
    if (this.backendErrors['password']) {
      return this.backendErrors['password'];
    }
    if (!this.password) {
      return 'Password is required.';
    }
    if (this.password.length < 8) {
      return 'Password must contain at least 8 characters.';
    }
    if (!/[A-Z]/.test(this.password)) {
      return 'Password must contain at least one uppercase letter.';
    }
    if (!/[a-z]/.test(this.password)) {
      return 'Password must contain at least one lowercase letter.';
    }
    if (!/[0-9]/.test(this.password)) {
      return 'Password must contain at least one number.';
    }
    if (!/[^A-Za-z0-9]/.test(this.password)) {
      return 'Password must contain at least one special character.';
    }
    return null;
  }

  getConfirmPasswordError(): string | null {
    if (this.backendErrors['confirmPassword']) {
      return this.backendErrors['confirmPassword'];
    }
    if (!this.confirmPassword) {
      return 'Please confirm your password.';
    }
    if (this.confirmPassword !== this.password) {
      return 'Passwords do not match.';
    }
    return null;
  }

  get isFormValid(): boolean {
    return (
      !this.getNameError() &&
      !this.getEmailError() &&
      !this.getPhoneError() &&
      !this.getPasswordError() &&
      !this.getConfirmPasswordError()
    );
  }

  onSubmit(): void {
    this.isSubmitted = true;
    this.errorMessage.set(null);

    if (!this.isFormValid) {
      return;
    }

    this.isLoading.set(true);

    this.authService.register({
      name: this.name.trim(),
      email: this.email.trim(),
      phone: this.phone.trim() || undefined,
      password: this.password
    }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.authService.navigateToRoleDashboard(response.role);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errObj = err.error;
        if (errObj?.errors && typeof errObj.errors === 'object') {
          for (const key of Object.keys(errObj.errors)) {
            this.backendErrors[key] = errObj.errors[key];
          }
        }
        const serverMsg = errObj?.message || 'Registration failed. Please check your information.';
        if (serverMsg.toLowerCase().includes('already exists')) {
          this.backendErrors['email'] = 'An account with this email already exists.';
        } else if (!errObj?.errors || Object.keys(errObj.errors).length === 0) {
          this.errorMessage.set(serverMsg);
        }
      }
    });
  }
}
