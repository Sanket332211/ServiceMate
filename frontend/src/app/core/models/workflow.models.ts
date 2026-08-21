import { FuelType, Transmission } from './vehicle.models';
import { BookingStatus, ServiceType, TimeSlot } from './booking.models';

export type WorkflowStatus =
  | 'CAR_RECEIVED'
  | 'INSPECTION'
  | 'SERVICE_IN_PROGRESS'
  | 'AWAITING_APPROVAL'
  | 'QUALITY_CHECK'
  | 'READY_FOR_DELIVERY'
  | 'COMPLETED';

export type RepairStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type NotificationType =
  | 'SERVICE_STATUS_UPDATED'
  | 'REPAIR_REQUESTED'
  | 'REPAIR_APPROVED'
  | 'REPAIR_REJECTED'
  | 'VEHICLE_READY'
  | 'SERVICE_COMPLETED'
  | 'BOOKING_CONFIRMED'
  | 'BOOKING_CANCELLED';

export interface AdditionalRepairRequest {
  description: string;
  reason: string;
  estimatedAmount: number;
}

export interface AdditionalRepairResponse {
  id: number;
  bookingId: number;
  description: string;
  reason: string;
  estimatedAmount: number;
  status: RepairStatus;
  requestedAt: string;
  respondedAt?: string;
}

export interface WorkflowResponse {
  id?: number;
  bookingId: number;
  vehicleId: number;
  vehicleMake: string;
  vehicleModel: string;
  vehicleRegistrationNumber: string;
  vehicleFuelType?: FuelType;
  vehicleTransmission?: Transmission;
  vehicleCurrentMileage?: number;

  customerName: string;
  customerEmail: string;
  customerPhone?: string;

  serviceType: ServiceType;
  serviceTypeDisplayName: string;
  bookingDate: string;
  timeSlot: TimeSlot;
  timeSlotLabel: string;
  bookingStatus: BookingStatus;

  pickupDropRequired: boolean;
  pickupDropCharge: number;
  estimatedServiceAmount: number;
  estimatedTotalAmount: number;

  workflowStatus?: WorkflowStatus;
  workflowStatusDisplayName?: string;

  carReceivedAt?: string;
  inspectionStartedAt?: string;
  serviceStartedAt?: string;
  qualityCheckStartedAt?: string;
  readyForDeliveryAt?: string;
  completedAt?: string;
  notes?: string;
  createdAt?: string;

  additionalRepairs: AdditionalRepairResponse[];
}

export interface NotificationResponse {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  relatedBookingId?: number;
  isRead: boolean;
  createdAt: string;
}

export interface WorkflowMilestone {
  key: string;
  title: string;
  icon: string;
  description: string;
}

export const WORKFLOW_MILESTONES: WorkflowMilestone[] = [
  {
    key: 'CONFIRMED',
    title: 'Booking Confirmed',
    icon: 'bi-calendar-check',
    description: 'Appointment slot reserved at service center'
  },
  {
    key: 'CAR_RECEIVED',
    title: 'Car Received',
    icon: 'bi-box-arrow-in-down-right',
    description: 'Vehicle received at workshop bay'
  },
  {
    key: 'INSPECTION',
    title: '40-Point Inspection',
    icon: 'bi-search',
    description: 'Comprehensive mechanical & diagnostic assessment'
  },
  {
    key: 'SERVICE_IN_PROGRESS',
    title: 'Service in Progress',
    icon: 'bi-tools',
    description: 'Active maintenance and authorized repairs'
  },
  {
    key: 'QUALITY_CHECK',
    title: 'Quality Check',
    icon: 'bi-shield-check',
    description: 'Final multi-point safety validation & road test'
  },
  {
    key: 'READY_FOR_DELIVERY',
    title: 'Ready for Delivery',
    icon: 'bi-check2-all',
    description: 'Service completed, ready for customer handover'
  },
  {
    key: 'COMPLETED',
    title: 'Delivered / Completed',
    icon: 'bi-hand-thumbs-up',
    description: 'Vehicle handed over to customer'
  }
];
