import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { VehicleService } from '../../../core/services/vehicle.service';
import { BookingService } from '../../../core/services/booking.service';
import { Vehicle } from '../../../core/models/vehicle.models';
import {
  BookingRequest,
  BookingResponse,
  SERVICE_TYPES_CONFIG,
  ServiceType,
  ServiceTypeInfo,
  SlotAvailabilityResponse,
  TimeSlot
} from '../../../core/models/booking.models';

export interface DateOption {
  dateString: string; // YYYY-MM-DD
  displayDay: string; // e.g. "Today", "Tomorrow", "Wed"
  displayDate: string; // e.g. "19 Aug"
}

/**
 * BookingWizardComponent
 *
 * Implements the 6-step customer service booking wizard with real-time capacity checks
 * and intelligent AI service recommendation preselection.
 */
@Component({
  selector: 'app-booking-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './booking-wizard.component.html'
})
export class BookingWizardComponent implements OnInit {
  // Wizard Steps: 1: Vehicle, 2: Service, 3: Date & Slot, 4: Pickup/Drop, 5: Review, 6: Confirmed
  currentStep = signal<number>(1);

  // Selections
  selectedVehicle = signal<Vehicle | null>(null);
  selectedServices = signal<ServiceTypeInfo[]>([]);
  selectedDate = signal<string>('');
  selectedSlot = signal<SlotAvailabilityResponse | null>(null);
  pickupDropRequired = signal<boolean>(false);
  aiRecommendedPackage = signal<ServiceType | null>(null);

  // Available Data
  servicePackages = SERVICE_TYPES_CONFIG;
  availableDates: DateOption[] = [];
  slotsAvailability = signal<SlotAvailabilityResponse[]>([]);

  // State Signals
  isLoadingSlots = signal(false);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);
  confirmedBooking = signal<BookingResponse | null>(null);

  constructor(
    public vehicleService: VehicleService,
    public bookingService: BookingService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.generateDateOptions();

    this.route.queryParams.subscribe({
      next: (params) => {
        const vIdParam = params['vehicleId'] ? Number(params['vehicleId']) : null;
        const pkgParam = params['servicePackage'] as ServiceType | undefined;

        this.vehicleService.loadMyVehicles().subscribe({
          next: (vehicles) => {
            if (vehicles && vehicles.length > 0) {
              let matchedVehicle = vehicles[0];
              if (vIdParam) {
                const found = vehicles.find((v) => v.id === vIdParam);
                if (found) {
                  matchedVehicle = found;
                }
              }
              this.selectedVehicle.set(matchedVehicle);

              if (pkgParam) {
                const matchedPkg = this.servicePackages.find((p) => p.type === pkgParam);
                if (matchedPkg) {
                  this.selectedServices.set([matchedPkg]);
                  this.aiRecommendedPackage.set(pkgParam);
                  // Open Step 2 with AI preselected package
                  this.currentStep.set(2);
                }
              }
            }
          }
        });
      }
    });
  }

  generateDateOptions(): void {
    const dates: DateOption[] = [];
    const today = new Date();

    for (let i = 0; i <= 7; i++) {
      const d = new Date();
      d.setDate(today.getDate() + i);

      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const dateString = `${year}-${month}-${day}`;

      let displayDay = '';
      if (i === 0) displayDay = 'Today';
      else if (i === 1) displayDay = 'Tomorrow';
      else displayDay = d.toLocaleDateString('en-US', { weekday: 'short' });

      const displayDate = d.toLocaleDateString('en-US', { day: 'numeric', month: 'short' });

      dates.push({ dateString, displayDay, displayDate });
    }

    this.availableDates = dates;
    if (dates.length > 0) {
      this.selectDate(dates[0].dateString);
    }
  }

  selectVehicle(vehicle: Vehicle): void {
    this.selectedVehicle.set(vehicle);
    this.errorMessage.set(null);
  }

  toggleService(service: ServiceTypeInfo): void {
    const current = this.selectedServices();
    const exists = current.some(s => s.type === service.type);
    if (exists) {
      this.selectedServices.set(current.filter(s => s.type !== service.type));
    } else {
      this.selectedServices.set([...current, service]);
    }
    this.errorMessage.set(null);
  }

  isServiceSelected(service: ServiceTypeInfo): boolean {
    return this.selectedServices().some(s => s.type === service.type);
  }

  get selectedServicesSummary(): string {
    return this.selectedServices().map(s => s.displayName).join(', ');
  }

  selectDate(dateStr: string): void {
    this.selectedDate.set(dateStr);
    this.selectedSlot.set(null);
    this.loadSlotAvailability(dateStr);
  }

  loadSlotAvailability(dateStr: string): void {
    this.isLoadingSlots.set(true);
    this.bookingService.getAvailability(dateStr).subscribe({
      next: (slots) => {
        this.slotsAvailability.set(slots);
        this.isLoadingSlots.set(false);
      },
      error: () => {
        this.isLoadingSlots.set(false);
      }
    });
  }

  selectSlot(slot: SlotAvailabilityResponse): void {
    if (slot.available) {
      this.selectedSlot.set(slot);
      this.errorMessage.set(null);
    }
  }

  get estimatedServiceAmount(): number {
    return this.selectedServices().reduce((sum, s) => sum + s.basePrice, 0);
  }

  get pickupDropCharge(): number {
    return this.pickupDropRequired() ? 300 : 0;
  }

  get estimatedTotalAmount(): number {
    return this.estimatedServiceAmount + this.pickupDropCharge;
  }

  goToStep(step: number): void {
    this.errorMessage.set(null);

    if (step === 2 && !this.selectedVehicle()) {
      this.errorMessage.set('Please select a vehicle to proceed.');
      return;
    }
    if (step === 3 && this.selectedServices().length === 0) {
      this.errorMessage.set('Please select at least one service package to proceed.');
      return;
    }
    if (step === 4 && (!this.selectedDate() || !this.selectedSlot())) {
      this.errorMessage.set('Please select an available date and time slot.');
      return;
    }

    this.currentStep.set(step);
  }

  confirmAndBook(): void {
    if (!this.selectedVehicle() || this.selectedServices().length === 0 || !this.selectedDate() || !this.selectedSlot()) {
      this.errorMessage.set('Please complete all booking steps.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const selected = this.selectedServices();
    const request: BookingRequest = {
      vehicleId: this.selectedVehicle()!.id,
      serviceType: selected[0].type,
      serviceTypes: selected.map(s => s.type),
      bookingDate: this.selectedDate(),
      timeSlot: this.selectedSlot()!.slot,
      pickupDropRequired: this.pickupDropRequired()
    };

    this.bookingService.createBooking(request).subscribe({
      next: (response) => {
        this.isSubmitting.set(false);
        this.confirmedBooking.set(response);
        this.currentStep.set(6); // Confirmation step
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to complete booking. Please try again.');
      }
    });
  }

  viewAllBookings(): void {
    this.router.navigate(['/customer/bookings']);
  }
}
