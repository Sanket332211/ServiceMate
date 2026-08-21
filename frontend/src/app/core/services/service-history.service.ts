import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ServiceCompletionRequest,
  ServiceRecordResponse,
  VehicleServiceHistoryResponse
} from '../models/service-history.models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ServiceHistoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  /**
   * Retrieves complete chronological service history for a vehicle.
   */
  getVehicleServiceHistory(vehicleId: number): Observable<VehicleServiceHistoryResponse> {
    return this.http.get<VehicleServiceHistoryResponse>(`${this.baseUrl}/vehicles/${vehicleId}/service-history`);
  }

  /**
   * Downloads complete multi-visit vehicle service history PDF.
   */
  downloadVehicleHistoryPdf(vehicleId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/vehicles/${vehicleId}/service-history/pdf`, {
      responseType: 'blob'
    });
  }

  /**
   * Retrieves a single finalized service record.
   */
  getSingleServiceRecord(recordId: number): Observable<ServiceRecordResponse> {
    return this.http.get<ServiceRecordResponse>(`${this.baseUrl}/service-records/${recordId}`);
  }

  /**
   * Downloads a single service visit PDF report.
   */
  downloadSingleServicePdf(recordId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/service-records/${recordId}/pdf`, {
      responseType: 'blob'
    });
  }

  /**
   * Workshop: Saves or updates service completion details during QUALITY_CHECK.
   */
  saveServiceCompletionDetails(bookingId: number, request: ServiceCompletionRequest): Observable<ServiceRecordResponse> {
    return this.http.post<ServiceRecordResponse>(`${this.baseUrl}/service-center/bookings/${bookingId}/service-record`, request);
  }

  /**
   * Workshop: Retrieves existing completion details for a booking.
   */
  getServiceCompletionDetails(bookingId: number): Observable<ServiceRecordResponse> {
    return this.http.get<ServiceRecordResponse>(`${this.baseUrl}/service-center/bookings/${bookingId}/service-record`);
  }
}
