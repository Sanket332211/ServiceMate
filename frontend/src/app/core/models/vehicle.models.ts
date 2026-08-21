export type FuelType = 'PETROL' | 'DIESEL' | 'ELECTRIC' | 'HYBRID' | 'CNG';
export type Transmission = 'MANUAL' | 'AUTOMATIC';

export interface Vehicle {
  id: number;
  ownerId?: number;
  ownerName?: string;
  ownerEmail?: string;
  registrationNumber: string;
  make: string;
  model: string;
  manufacturingYear: number;
  fuelType: FuelType;
  transmission: Transmission;
  currentMileage: number;
  createdAt?: string;
}

export interface VehicleRequest {
  registrationNumber: string;
  make: string;
  model: string;
  manufacturingYear: number;
  fuelType: FuelType;
  transmission: Transmission;
  currentMileage?: number;
}
