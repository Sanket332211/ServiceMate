import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { WorkflowService } from '../../../core/services/workflow.service';
import { ServiceHistoryService } from '../../../core/services/service-history.service';
import {
  AdditionalRepairResponse,
  WORKFLOW_MILESTONES,
  WorkflowMilestone,
  WorkflowResponse,
  WorkflowStatus
} from '../../../core/models/workflow.models';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-service-tracker',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './service-tracker.component.html',
  styleUrls: ['./service-tracker.component.css']
})
export class ServiceTrackerComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  private workflowService = inject(WorkflowService);
  private historyService = inject(ServiceHistoryService);

  bookingId = signal<number>(0);
  workflow = signal<WorkflowResponse | null>(null);
  isLoading = signal<boolean>(true);
  isDownloadingPdf = signal<boolean>(false);
  actionLoading = signal<number | null>(null);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  milestones: WorkflowMilestone[] = WORKFLOW_MILESTONES;
  private pollSub?: Subscription;

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('bookingId');
    if (idParam) {
      this.bookingId.set(Number(idParam));
      this.loadWorkflow();
      // Poll every 10 seconds for real-time status updates as fallback to WS
      this.pollSub = interval(10000).subscribe(() => this.loadWorkflow(true));
    } else {
      this.errorMessage.set('Invalid booking ID specified.');
      this.isLoading.set(false);
    }
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  loadWorkflow(silent = false): void {
    if (!silent) this.isLoading.set(true);
    this.workflowService.getWorkflowForBooking(this.bookingId()).subscribe({
      next: (data) => {
        this.workflow.set(data);
        if (!silent) this.isLoading.set(false);
      },
      error: (err) => {
        if (!silent) {
          this.errorMessage.set(err.error?.message || 'Failed to load service tracker details.');
          this.isLoading.set(false);
        }
      }
    });
  }

  isMilestoneActive(milestoneKey: string): boolean {
    const wf = this.workflow();
    if (!wf) return false;

    // When booking is CANCELLED, no step is actively in progress
    if (wf.bookingStatus === 'CANCELLED') {
      return false;
    }

    // When service is COMPLETED, no step is actively in progress
    if (wf.workflowStatus === 'COMPLETED') {
      return false;
    }

    // If status is AWAITING_APPROVAL, the active operational step is SERVICE_IN_PROGRESS
    const currentStatus = wf.workflowStatus === 'AWAITING_APPROVAL' ? 'SERVICE_IN_PROGRESS' : wf.workflowStatus;

    if (!currentStatus) {
      return milestoneKey === 'CONFIRMED';
    }
    return currentStatus === milestoneKey;
  }

  isMilestoneCompleted(milestoneKey: string): boolean {
    const wf = this.workflow();
    if (!wf) return false;

    // When booking is CANCELLED, milestones are not completed
    if (wf.bookingStatus === 'CANCELLED') {
      return false;
    }

    // When service is COMPLETED, all milestones are completed
    if (wf.workflowStatus === 'COMPLETED') {
      return true;
    }

    const currentStatus = wf.workflowStatus === 'AWAITING_APPROVAL' ? 'SERVICE_IN_PROGRESS' : wf.workflowStatus;
    if (!currentStatus) {
      return false;
    }

    const order = ['CONFIRMED', 'CAR_RECEIVED', 'INSPECTION', 'SERVICE_IN_PROGRESS', 'QUALITY_CHECK', 'READY_FOR_DELIVERY', 'COMPLETED'];
    const currentIndex = order.indexOf(currentStatus);
    const milestoneIndex = order.indexOf(milestoneKey);

    return milestoneIndex < currentIndex;
  }

  getMilestoneTimestamp(milestoneKey: string): string | undefined {
    const wf = this.workflow();
    if (!wf) return undefined;

    switch (milestoneKey) {
      case 'CAR_RECEIVED':
        return wf.carReceivedAt;
      case 'INSPECTION':
        return wf.inspectionStartedAt;
      case 'SERVICE_IN_PROGRESS':
        return wf.serviceStartedAt;
      case 'QUALITY_CHECK':
        return wf.qualityCheckStartedAt;
      case 'READY_FOR_DELIVERY':
        return wf.readyForDeliveryAt;
      case 'COMPLETED':
        return wf.completedAt;
      default:
        return undefined;
    }
  }

  getPendingRepairs(): AdditionalRepairResponse[] {
    return this.workflow()?.additionalRepairs?.filter((r) => r.status === 'PENDING') || [];
  }

  getResolvedRepairs(): AdditionalRepairResponse[] {
    return this.workflow()?.additionalRepairs?.filter((r) => r.status !== 'PENDING') || [];
  }

  approveRepair(repair: AdditionalRepairResponse): void {
    this.actionLoading.set(repair.id);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.workflowService.approveRepair(repair.id).subscribe({
      next: (res) => {
        this.actionLoading.set(null);
        this.successMessage.set(`Repair "${res.description}" approved successfully! Service is resuming.`);
        this.loadWorkflow(true);
      },
      error: (err) => {
        this.actionLoading.set(null);
        this.errorMessage.set(err.error?.message || 'Failed to approve repair request.');
      }
    });
  }

  rejectRepair(repair: AdditionalRepairResponse): void {
    this.actionLoading.set(repair.id);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.workflowService.rejectRepair(repair.id).subscribe({
      next: (res) => {
        this.actionLoading.set(null);
        this.successMessage.set(`Repair "${res.description}" declined.`);
        this.loadWorkflow(true);
      },
      error: (err) => {
        this.actionLoading.set(null);
        this.errorMessage.set(err.error?.message || 'Failed to decline repair request.');
      }
    });
  }

  downloadPassportPdf(): void {
    const wf = this.workflow();
    if (!wf?.vehicleId) return;

    this.isDownloadingPdf.set(true);
    this.errorMessage.set(null);

    this.historyService.downloadVehicleHistoryPdf(wf.vehicleId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const rawReg = wf.vehicleRegistrationNumber || `Vehicle_${wf.vehicleId}`;
        const cleanReg = rawReg.replace(/\s+/g, '_');
        a.download = `ServiceMate_${cleanReg}_Service_History.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.isDownloadingPdf.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to download the service PDF. Please try again.');
        this.isDownloadingPdf.set(false);
      }
    });
  }
}
