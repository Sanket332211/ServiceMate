import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { VehicleService } from '../../core/services/vehicle.service';
import { BookingService } from '../../core/services/booking.service';
import { AiService } from '../../core/services/ai.service';
import { AiServiceAdvisorResponse } from '../../core/models/ai.models';

/**
 * CustomerDashboardComponent
 *
 * Main customer portal overview showing garage summary, active booking status,
 * quick actions, and the interactive AI Service Advisor.
 */
@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './customer-dashboard.component.html'
})
export class CustomerDashboardComponent implements OnInit {
  // AI Service Advisor Reactive State
  selectedVehicleId = signal<number | null>(null);
  problemDescription = signal<string>('');
  isAdvisorLoading = signal<boolean>(false);
  advisorResponse = signal<AiServiceAdvisorResponse | null>(null);
  advisorError = signal<string | null>(null);

  constructor(
    public authService: AuthService,
    public vehicleService: VehicleService,
    public bookingService: BookingService,
    private aiService: AiService
  ) {}

  ngOnInit(): void {
    this.vehicleService.loadMyVehicles().subscribe({
      next: (vehicles) => {
        if (vehicles && vehicles.length > 0 && !this.selectedVehicleId()) {
          this.selectedVehicleId.set(vehicles[0].id);
        }
      }
    });
    this.bookingService.loadMyBookings().subscribe();
  }

  get confirmedBookingsCount(): number {
    return this.bookingService.bookingsSignal().filter((b) => b.status === 'CONFIRMED').length;
  }

  get upcomingConfirmedBookings() {
    return this.bookingService.bookingsSignal().filter((b) => b.status === 'CONFIRMED').slice(0, 2);
  }

  getAiRecommendation(): void {
    const vId = this.selectedVehicleId();
    const problem = this.problemDescription().trim();

    if (!vId) {
      this.advisorError.set('Please select a vehicle from your garage.');
      return;
    }

    if (!problem || problem.length < 5) {
      this.advisorError.set('Please describe the problem in at least 5 characters.');
      return;
    }

    this.isAdvisorLoading.set(true);
    this.advisorError.set(null);
    this.advisorResponse.set(null);

    this.aiService.getServiceAdvice({
      vehicleId: vId,
      problemDescription: problem
    }).subscribe({
      next: (res) => {
        this.advisorResponse.set(res);
        this.isAdvisorLoading.set(false);
      },
      error: (err) => {
        let msg = err.error?.message;
        if (!msg) {
          if (err.status === 503) {
            msg = 'AI Service Advisor is temporarily unavailable. Please try again later.';
          } else if (err.status === 403) {
            msg = 'You do not have permission to request AI advice for this vehicle.';
          } else if (err.status === 400) {
            msg = 'Invalid input. Please provide a clear description of the vehicle issue.';
          } else {
            msg = 'Unable to connect to Service Advisor. Please check your connection and try again.';
          }
        }
        this.advisorError.set(msg);
        this.isAdvisorLoading.set(false);
      }
    });
  }


  getUrgencyBadgeClass(urgency: string | undefined): string {
    switch (urgency?.toUpperCase()) {
      case 'HIGH':
        return 'badge bg-danger-subtle text-danger border border-danger-subtle px-3 py-1';
      case 'LOW':
        return 'badge bg-success-subtle text-success border border-success-subtle px-3 py-1';
      case 'MEDIUM':
      default:
        return 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-3 py-1';
    }
  }

  onLogout(): void {
    this.authService.logout();
  }
}
