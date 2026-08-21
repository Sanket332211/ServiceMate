import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BookingService } from '../../../core/services/booking.service';
import { BookingResponse } from '../../../core/models/booking.models';

/**
 * BookingListComponent
 *
 * Displays the authenticated customer's booking history and handles booking cancellation.
 */
@Component({
  selector: 'app-booking-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './booking-list.component.html'
})
export class BookingListComponent implements OnInit {
  selectedFilter = signal<'ALL' | 'CONFIRMED' | 'CANCELLED'>('ALL');
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  cancellingId = signal<number | null>(null);

  constructor(public bookingService: BookingService) {}

  ngOnInit(): void {
    this.bookingService.loadMyBookings().subscribe();
  }

  get filteredBookings(): BookingResponse[] {
    const list = [...this.bookingService.bookingsSignal()];
    list.sort((a, b) => {
      if (a.createdAt && b.createdAt) {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      return b.id - a.id;
    });

    const filter = this.selectedFilter();
    if (filter === 'CONFIRMED') {
      return list.filter((b) => b.status === 'CONFIRMED');
    }
    if (filter === 'CANCELLED') {
      return list.filter((b) => b.status === 'CANCELLED');
    }
    return list;
  }

  onCancelBooking(booking: BookingResponse): void {
    if (confirm(`Are you sure you want to cancel booking #BK-${booking.id} for ${booking.vehicleMake} ${booking.vehicleModel} on ${booking.bookingDate}? This will immediately release your slot for other customers.`)) {
      this.cancellingId.set(booking.id);
      this.errorMessage.set(null);

      this.bookingService.cancelBooking(booking.id).subscribe({
        next: () => {
          this.cancellingId.set(null);
          this.showSuccess(`Booking #BK-${booking.id} cancelled successfully. Slot capacity released.`);
        },
        error: (err) => {
          this.cancellingId.set(null);
          this.errorMessage.set(err.error?.message || 'Failed to cancel booking.');
        }
      });
    }
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
