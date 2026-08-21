import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  AdditionalRepairRequest,
  AdditionalRepairResponse,
  WorkflowResponse
} from '../models/workflow.models';
import { environment } from '../../../environments/environment';

/**
 * WorkflowService
 *
 * Manages service workflow milestone transitions, vehicle operations, and additional repair requests.
 */
@Injectable({
  providedIn: 'root'
})
export class WorkflowService {
  private readonly SC_API_URL = `${environment.apiUrl}/service-center/bookings`;
  private readonly CUSTOMER_API_URL = environment.apiUrl;

  activeWorkflowSignal = signal<WorkflowResponse | null>(null);
  serviceCenterWorkflowsSignal = signal<WorkflowResponse[]>([]);
  isLoading = signal(false);

  constructor(private http: HttpClient) {}

  /**
   * Customer / Shared: Fetches workflow status for a specific booking.
   */
  getWorkflowForBooking(bookingId: number): Observable<WorkflowResponse> {
    this.isLoading.set(true);
    return this.http.get<WorkflowResponse>(`${this.CUSTOMER_API_URL}/bookings/${bookingId}/workflow`).pipe(
      tap({
        next: (wf) => {
          this.activeWorkflowSignal.set(wf);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  /**
   * Service Center: Fetches all workshop bookings with workflow status.
   */
  loadServiceCenterWorkflows(): Observable<WorkflowResponse[]> {
    this.isLoading.set(true);
    return this.http.get<WorkflowResponse[]>(this.SC_API_URL).pipe(
      tap({
        next: (list) => {
          this.serviceCenterWorkflowsSignal.set(list);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  /**
   * Service Center: CONFIRMED -> CAR_RECEIVED
   */
  receiveVehicle(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/receive`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: CAR_RECEIVED -> INSPECTION
   */
  startInspection(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/start-inspection`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: INSPECTION -> SERVICE_IN_PROGRESS
   */
  startService(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/start-service`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: SERVICE_IN_PROGRESS -> QUALITY_CHECK
   */
  startQualityCheck(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/start-quality-check`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: QUALITY_CHECK -> READY_FOR_DELIVERY
   */
  markReadyForDelivery(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/mark-ready`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: READY_FOR_DELIVERY -> COMPLETED
   */
  completeService(bookingId: number, notes?: string): Observable<WorkflowResponse> {
    return this.http.post<WorkflowResponse>(`${this.SC_API_URL}/${bookingId}/complete`, { notes }).pipe(
      tap((wf) => this.updateLocalWorkflow(wf))
    );
  }

  /**
   * Service Center: Creates an additional repair request.
   */
  createAdditionalRepair(bookingId: number, request: AdditionalRepairRequest): Observable<AdditionalRepairResponse> {
    return this.http.post<AdditionalRepairResponse>(`${this.SC_API_URL}/${bookingId}/repairs`, request).pipe(
      tap((newRepair) => {
        const current = this.activeWorkflowSignal();
        if (current && current.bookingId === bookingId) {
          current.additionalRepairs = [newRepair, ...(current.additionalRepairs || [])];
          current.workflowStatus = 'AWAITING_APPROVAL';
          current.workflowStatusDisplayName = 'Awaiting Customer Approval for Additional Repair';
          this.activeWorkflowSignal.set({ ...current });
        }
      })
    );
  }

  /**
   * Customer: Approves an additional repair request.
   */
  approveRepair(repairId: number): Observable<AdditionalRepairResponse> {
    return this.http.post<AdditionalRepairResponse>(`${this.CUSTOMER_API_URL}/repairs/${repairId}/approve`, {}).pipe(
      tap((updatedRepair) => this.updateLocalRepair(updatedRepair))
    );
  }

  /**
   * Customer: Rejects an additional repair request.
   */
  rejectRepair(repairId: number): Observable<AdditionalRepairResponse> {
    return this.http.post<AdditionalRepairResponse>(`${this.CUSTOMER_API_URL}/repairs/${repairId}/reject`, {}).pipe(
      tap((updatedRepair) => this.updateLocalRepair(updatedRepair))
    );
  }

  private updateLocalWorkflow(updated: WorkflowResponse): void {
    this.activeWorkflowSignal.set(updated);
    this.serviceCenterWorkflowsSignal.update((list) =>
      list.map((w) => (w.bookingId === updated.bookingId ? updated : w))
    );
  }

  private updateLocalRepair(updatedRepair: AdditionalRepairResponse): void {
    const current = this.activeWorkflowSignal();
    if (current && current.additionalRepairs) {
      current.additionalRepairs = current.additionalRepairs.map((r) =>
        r.id === updatedRepair.id ? updatedRepair : r
      );
      this.activeWorkflowSignal.set({ ...current });
    }
  }
}
