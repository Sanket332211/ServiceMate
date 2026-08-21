import { AdditionalRepairResponse } from './workflow.models';

export interface ServiceItemDto {
  id?: number;
  description: string;
  category?: string; // 'FLUIDS' | 'PARTS' | 'LABOUR'
  quantity: number;
  unitPrice: number;
  totalPrice?: number;
}

export interface InspectionFindingDto {
  id?: number;
  component: string;
  conditionStatus: string; // 'Good' | 'Fair' | 'Needs Attention' | 'Replaced'
  notes?: string;
}

export interface ServiceCompletionRequest {
  mileage: number;
  serviceSummary: string;
  items: ServiceItemDto[];
  inspectionFindings: InspectionFindingDto[];
}

export interface ServiceRecordResponse {
  id: number;
  bookingId: number;
  vehicleId: number;
  vehicleMake: string;
  vehicleModel: string;
  vehicleRegistrationNumber: string;
  vehicleFuelType: string;
  vehicleTransmission: string;
  vehicleCurrentMileage?: number;
  customerName: string;
  customerEmail: string;
  customerPhone?: string;
  serviceType: string;
  serviceTypeDisplayName: string;
  serviceDate: string;
  mileage: number;
  serviceSummary: string;
  actualBaseServiceAmount: number;
  actualAdditionalRepairsAmount: number;
  pickupDropUsed: boolean;
  pickupDropCharge: number;
  actualTotalAmount: number;
  createdAt: string;
  finalizedAt?: string;
  items: ServiceItemDto[];
  inspectionFindings: InspectionFindingDto[];
  additionalRepairs: AdditionalRepairResponse[];
}

export interface VehicleServiceHistoryResponse {
  vehicleId: number;
  vehicleMake: string;
  vehicleModel: string;
  vehicleRegistrationNumber: string;
  vehicleFuelType: string;
  vehicleTransmission: string;
  currentMileage: number;
  customerName: string;
  totalCompletedVisits: number;
  totalAmountSpent: number;
  records: ServiceRecordResponse[];
}
