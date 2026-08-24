export type ServiceType =
  | 'GENERAL_SERVICE'
  | 'OIL_CHANGE'
  | 'AC_SERVICE'
  | 'BRAKE_SERVICE'
  | 'BATTERY_SERVICE';

export type TimeSlot =
  | 'MORNING_SLOT_1'
  | 'MORNING_SLOT_2'
  | 'AFTERNOON_SLOT_1'
  | 'AFTERNOON_SLOT_2';

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface BookingRequest {
  vehicleId: number;
  serviceType?: ServiceType;
  serviceTypes?: ServiceType[];
  bookingDate: string; // YYYY-MM-DD
  timeSlot: TimeSlot;
  pickupDropRequired: boolean;
}

export interface BookingResponse {
  id: number;
  vehicleId: number;
  vehicleMake: string;
  vehicleModel: string;
  vehicleRegistrationNumber: string;
  serviceType: ServiceType;
  serviceTypeDisplayName: string;
  bookingDate: string;
  timeSlot: TimeSlot;
  timeSlotLabel: string;
  status: BookingStatus;
  pickupDropRequired: boolean;
  pickupDropCharge: number;
  estimatedServiceAmount: number;
  estimatedTotalAmount: number;
  createdAt: string;
}

export interface SlotAvailabilityResponse {
  slot: TimeSlot;
  label: string;
  capacity: number;
  booked: number;
  remaining: number;
  available: boolean;
  past?: boolean;
}

export interface ServiceTypeInfo {
  type: ServiceType;
  displayName: string;
  basePrice: number;
  description: string;
  icon: string;
}

export const SERVICE_TYPES_CONFIG: ServiceTypeInfo[] = [
  {
    type: 'GENERAL_SERVICE',
    displayName: 'General Service',
    basePrice: 1499,
    description: 'Comprehensive 40-point full vehicle inspection, fluid top-ups & filter cleaning',
    icon: 'bi-tools'
  },
  {
    type: 'OIL_CHANGE',
    displayName: 'Oil Change',
    basePrice: 999,
    description: 'Premium synthetic engine oil replacement & oil filter renewal',
    icon: 'bi-droplet-half'
  },
  {
    type: 'AC_SERVICE',
    displayName: 'AC Service & Inspection',
    basePrice: 1299,
    description: 'AC gas refill, cabin filter deep clean, condenser cooling check & duct sanitization',
    icon: 'bi-snow'
  },
  {
    type: 'BRAKE_SERVICE',
    displayName: 'Brake Service & Fluid',
    basePrice: 1799,
    description: 'Front & rear brake pad overhaul, rotor cleaning, line bleeding & fluid replacement',
    icon: 'bi-disc'
  },
  {
    type: 'BATTERY_SERVICE',
    displayName: 'Battery Inspection & Care',
    basePrice: 499,
    description: 'Voltage load test, terminal anti-corrosion treatment & alternator output test',
    icon: 'bi-battery-charging'
  }
];
