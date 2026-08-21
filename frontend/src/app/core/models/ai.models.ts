import { ServiceType } from './booking.models';

/**
 * AI Service Models
 */

export interface AiServiceAdvisorRequest {
  vehicleId: number;
  problemDescription: string;
}

export interface AiServiceAdvisorResponse {
  possibleSystem: string;
  recommendedService: string;
  recommendedPackage: ServiceType;
  recommendedPackageName: string;
  recommendedPackagePrice: number;
  urgency: 'LOW' | 'MEDIUM' | 'HIGH';
  explanation: string;
  disclaimer: string;
}

export interface AiServiceSummaryResponse {
  serviceRecordId: number;
  summary: string;
  disclaimer: string;
}
