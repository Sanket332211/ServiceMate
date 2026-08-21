import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { VehicleService } from '../../core/services/vehicle.service';
import { FuelType, Transmission, Vehicle, VehicleRequest } from '../../core/models/vehicle.models';

/**
 * VehicleListComponent
 *
 * Provides dedicated vehicle CRUD management for authenticated customers at /customer/vehicles.
 */
@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './vehicle-list.component.html'
})
export class VehicleListComponent implements OnInit {
  // UI State Signals
  showModal = signal(false);
  isEditing = signal(false);
  editingVehicleId = signal<number | null>(null);
  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // Form Model
  form: VehicleRequest = {
    registrationNumber: '',
    make: '',
    model: '',
    manufacturingYear: new Date().getFullYear(),
    fuelType: 'PETROL',
    transmission: 'AUTOMATIC',
    currentMileage: 0
  };

  // Enums for dropdowns
  fuelTypes: FuelType[] = ['PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG'];
  transmissions: Transmission[] = ['MANUAL', 'AUTOMATIC'];

  constructor(public vehicleService: VehicleService) {}

  ngOnInit(): void {
    this.vehicleService.loadMyVehicles().subscribe();
  }

  openAddModal(): void {
    this.isEditing.set(false);
    this.editingVehicleId.set(null);
    this.errorMessage.set(null);
    this.form = {
      registrationNumber: '',
      make: '',
      model: '',
      manufacturingYear: new Date().getFullYear(),
      fuelType: 'PETROL',
      transmission: 'AUTOMATIC',
      currentMileage: 0
    };
    this.showModal.set(true);
  }

  openEditModal(vehicle: Vehicle): void {
    this.isEditing.set(true);
    this.editingVehicleId.set(vehicle.id);
    this.errorMessage.set(null);
    this.form = {
      registrationNumber: vehicle.registrationNumber,
      make: vehicle.make,
      model: vehicle.model,
      manufacturingYear: vehicle.manufacturingYear,
      fuelType: vehicle.fuelType,
      transmission: vehicle.transmission,
      currentMileage: vehicle.currentMileage
    };
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.errorMessage.set(null);
  }

  fillSampleVehicle(sampleNumber: 1 | 2): void {
    if (sampleNumber === 1) {
      this.form = {
        registrationNumber: 'MH12AB1234',
        make: 'Maruti Suzuki',
        model: 'Swift',
        manufacturingYear: 2022,
        fuelType: 'PETROL',
        transmission: 'MANUAL',
        currentMileage: 25000
      };
    } else {
      this.form = {
        registrationNumber: 'MH14CD5678',
        make: 'Hyundai',
        model: 'Creta',
        manufacturingYear: 2023,
        fuelType: 'DIESEL',
        transmission: 'AUTOMATIC',
        currentMileage: 18000
      };
    }
    this.errorMessage.set(null);
  }

  onSave(): void {
    if (!this.form.registrationNumber || !this.form.make || !this.form.model || !this.form.manufacturingYear) {
      this.errorMessage.set('Please fill out all required fields.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    // Normalize registration number
    this.form.registrationNumber = this.form.registrationNumber.trim().toUpperCase();

    if (this.isEditing() && this.editingVehicleId()) {
      this.vehicleService.updateVehicle(this.editingVehicleId()!, this.form).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.showModal.set(false);
          this.showSuccess('Vehicle updated successfully.');
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to update vehicle.');
        }
      });
    } else {
      this.vehicleService.createVehicle(this.form).subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.showModal.set(false);
          this.showSuccess('New vehicle registered successfully.');
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(err.error?.message || 'Failed to register vehicle.');
        }
      });
    }
  }

  onDelete(vehicle: Vehicle): void {
    if (confirm(`Are you sure you want to remove ${vehicle.make} ${vehicle.model} (${vehicle.registrationNumber})?`)) {
      this.vehicleService.deleteVehicle(vehicle.id).subscribe({
        next: () => {
          this.showSuccess('Vehicle removed successfully.');
        },
        error: (err) => {
          alert(err.error?.message || 'Failed to delete vehicle.');
        }
      });
    }
  }

  private showSuccess(msg: string): void {
    this.successMessage.set(msg);
    setTimeout(() => this.successMessage.set(null), 4000);
  }
}
