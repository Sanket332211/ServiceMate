import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkflowService } from '../../../core/services/workflow.service';
import { ServiceHistoryService } from '../../../core/services/service-history.service';
import {
  AdditionalRepairResponse,
  WORKFLOW_MILESTONES,
  WorkflowMilestone,
  WorkflowResponse,
  WorkflowStatus
} from '../../../core/models/workflow.models';
import {
  InspectionFindingDto,
  ServiceCompletionRequest,
  ServiceItemDto,
  ServiceRecordResponse
} from '../../../core/models/service-history.models';

@Component({
  selector: 'app-workflow-cockpit',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './workflow-cockpit.component.html',
  styleUrls: ['./workflow-cockpit.component.css']
})
export class ServiceCenterCockpitComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private workflowService = inject(WorkflowService);
  private historyService = inject(ServiceHistoryService);

  bookingId = signal<number>(0);
  workflow = signal<WorkflowResponse | null>(null);
  completionRecord = signal<ServiceRecordResponse | null>(null);
  isLoading = signal<boolean>(true);
  isTransitioning = signal<boolean>(false);
  isSubmittingRepair = signal<boolean>(false);
  isSavingCompletion = signal<boolean>(false);
  isDownloadingPdf = signal<boolean>(false);
  isDownloadingVisitPdf = signal<boolean>(false);
  showRepairModal = signal<boolean>(false);

  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  notes = signal<string>('');
  repairForm: FormGroup;
  completionForm: FormGroup;

  milestones: WorkflowMilestone[] = WORKFLOW_MILESTONES;

  constructor() {
    this.repairForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(3)]],
      reason: ['', [Validators.required, Validators.minLength(5)]],
      estimatedAmount: [null, [Validators.required, Validators.min(1)]]
    });

    this.completionForm = this.fb.group({
      mileage: [0, [Validators.required, Validators.min(0)]],
      serviceSummary: ['', [Validators.required, Validators.minLength(5)]],
      items: this.fb.array([]),
      inspectionFindings: this.fb.array([])
    });
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('bookingId');
    if (idParam) {
      this.bookingId.set(Number(idParam));
      this.loadWorkflow();
      this.loadCompletionDetails();
    } else {
      this.errorMessage.set('Invalid booking ID provided.');
      this.isLoading.set(false);
    }
  }

  loadWorkflow(): void {
    this.isLoading.set(true);
    this.workflowService.getWorkflowForBooking(this.bookingId()).subscribe({
      next: (data) => {
        this.workflow.set(data);
        if (data.notes) {
          this.notes.set(data.notes);
        }
        if (this.completionForm.get('mileage')?.value === 0 && data.vehicleCurrentMileage) {
          this.completionForm.patchValue({ mileage: data.vehicleCurrentMileage });
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to load booking workflow cockpit.');
        this.isLoading.set(false);
      }
    });
  }

  loadCompletionDetails(): void {
    this.historyService.getServiceCompletionDetails(this.bookingId()).subscribe({
      next: (record) => {
        this.completionRecord.set(record);
        this.populateCompletionForm(record);
      },
      error: () => {
        // No completion details entered yet; set up sensible defaults
        this.setupDefaultCompletionForm();
      }
    });
  }

  get itemsArray(): FormArray {
    return this.completionForm.get('items') as FormArray;
  }

  get findingsArray(): FormArray {
    return this.completionForm.get('inspectionFindings') as FormArray;
  }

  setupDefaultCompletionForm(): void {
    if (this.itemsArray.length === 0) {
      this.addItem('Full Synthetic Engine Oil (5W-30)', 'FLUIDS', 1, 800);
      this.addItem('OEM Oil Filter Replacement', 'PARTS', 1, 350);
      this.addItem('General Service Labour & Tuning', 'LABOUR', 1, 349);
    }
    if (this.findingsArray.length === 0) {
      this.addFinding('Brakes', 'Good', 'Pads and discs inspected and calibrated.');
      this.addFinding('Battery', 'Good', 'Resting voltage 12.6V, healthy load test.');
      this.addFinding('Tyres', 'Good', 'Tread depth checked across all tyres.');
      this.addFinding('Engine & Transmission', 'Good', 'No leaks detected; fluid levels optimal.');
    }
    if (!this.completionForm.get('serviceSummary')?.value) {
      this.completionForm.patchValue({
        serviceSummary: 'Comprehensive multi-point service and vehicle inspection completed satisfactorily.'
      });
    }
  }

  populateCompletionForm(record: ServiceRecordResponse): void {
    this.completionForm.patchValue({
      mileage: record.mileage,
      serviceSummary: record.serviceSummary
    });

    this.itemsArray.clear();
    if (record.items && record.items.length > 0) {
      for (const item of record.items) {
        this.addItem(item.description, item.category || 'PARTS', item.quantity, item.unitPrice);
      }
    }

    this.findingsArray.clear();
    if (record.inspectionFindings && record.inspectionFindings.length > 0) {
      for (const finding of record.inspectionFindings) {
        this.addFinding(finding.component, finding.conditionStatus, finding.notes || '');
      }
    }
  }

  addItem(description = '', category = 'PARTS', quantity = 1, unitPrice = 0): void {
    this.itemsArray.push(
      this.fb.group({
        description: [description, [Validators.required, Validators.minLength(2)]],
        category: [category],
        quantity: [quantity, [Validators.required, Validators.min(1)]],
        unitPrice: [unitPrice, [Validators.required, Validators.min(0)]]
      })
    );
  }

  removeItem(index: number): void {
    this.itemsArray.removeAt(index);
  }

  addFinding(component = '', conditionStatus = 'Good', notes = ''): void {
    this.findingsArray.push(
      this.fb.group({
        component: [component, [Validators.required, Validators.minLength(2)]],
        conditionStatus: [conditionStatus, [Validators.required]],
        notes: [notes]
      })
    );
  }

  removeFinding(index: number): void {
    this.findingsArray.removeAt(index);
  }

  get baseItemsSubtotal(): number {
    let total = 0;
    for (const ctrl of this.itemsArray.controls) {
      const q = Number(ctrl.get('quantity')?.value) || 0;
      const p = Number(ctrl.get('unitPrice')?.value) || 0;
      total += q * p;
    }
    return total;
  }

  get approvedRepairsTotal(): number {
    const repairs = this.workflow()?.additionalRepairs || [];
    return repairs
      .filter(r => r.status === 'APPROVED')
      .reduce((sum, r) => sum + (r.estimatedAmount || 0), 0);
  }

  get grandTotalCalculated(): number {
    const pickupCharge = this.workflow()?.pickupDropRequired ? (this.workflow()?.pickupDropCharge || 0) : 0;
    return this.baseItemsSubtotal + this.approvedRepairsTotal + pickupCharge;
  }

  saveCompletionDetails(callback?: () => void): void {
    if (this.completionForm.invalid) {
      this.completionForm.markAllAsTouched();
      this.errorMessage.set('Please fill in all required completion fields, work items, and inspection findings.');
      return;
    }

    this.isSavingCompletion.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formVal = this.completionForm.value;
    const req: ServiceCompletionRequest = {
      mileage: Number(formVal.mileage),
      serviceSummary: formVal.serviceSummary,
      items: formVal.items.map((i: any) => ({
        description: i.description,
        category: i.category,
        quantity: Number(i.quantity),
        unitPrice: Number(i.unitPrice)
      })),
      inspectionFindings: formVal.inspectionFindings.map((f: any) => ({
        component: f.component,
        conditionStatus: f.conditionStatus,
        notes: f.notes
      }))
    };

    this.historyService.saveServiceCompletionDetails(this.bookingId(), req).subscribe({
      next: (res) => {
        this.completionRecord.set(res);
        this.isSavingCompletion.set(false);
        this.successMessage.set('Service completion details successfully recorded!');
        if (callback) {
          callback();
        }
      },
      error: (err) => {
        this.isSavingCompletion.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to save completion details.');
      }
    });
  }

  get currentStatus(): WorkflowStatus | undefined {
    return this.workflow()?.workflowStatus;
  }

  get hasPendingRepairs(): boolean {
    return (this.workflow()?.additionalRepairs?.filter((r) => r.status === 'PENDING').length || 0) > 0;
  }

  // Milestone Actions
  receiveVehicle(): void {
    this.executeTransition(this.workflowService.receiveVehicle(this.bookingId(), this.notes()), 'Vehicle marked as received at workshop.');
  }

  startInspection(): void {
    this.executeTransition(this.workflowService.startInspection(this.bookingId(), this.notes()), '40-Point vehicle inspection started.');
  }

  startService(): void {
    this.executeTransition(this.workflowService.startService(this.bookingId(), this.notes()), 'Service & maintenance work commenced.');
  }

  startQualityCheck(): void {
    this.executeTransition(this.workflowService.startQualityCheck(this.bookingId(), this.notes()), 'Service completed. Quality check and road test started.');
  }

  markReadyForDelivery(): void {
    // If completion form is valid but record not saved yet, save then advance
    if (!this.completionRecord()) {
      this.saveCompletionDetails(() => {
        this.executeTransition(this.workflowService.markReadyForDelivery(this.bookingId(), this.notes()), 'Vehicle marked as READY for customer delivery.');
      });
    } else {
      this.executeTransition(this.workflowService.markReadyForDelivery(this.bookingId(), this.notes()), 'Vehicle marked as READY for customer delivery.');
    }
  }

  completeService(): void {
    this.executeTransition(this.workflowService.completeService(this.bookingId(), this.notes()), 'Service completed and closed! Customer booking marked COMPLETED.');
  }

  private executeTransition(obs$: any, successText: string): void {
    this.isTransitioning.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    obs$.subscribe({
      next: (res: WorkflowResponse) => {
        this.workflow.set(res);
        this.successMessage.set(successText);
        this.isTransitioning.set(false);
        this.loadCompletionDetails();
      },
      error: (err: any) => {
        this.errorMessage.set(err.error?.message || 'Action failed.');
        this.isTransitioning.set(false);
      }
    });
  }

  openRepairModal(): void {
    this.repairForm.reset();
    this.showRepairModal.set(true);
  }

  closeRepairModal(): void {
    this.showRepairModal.set(false);
  }

  fillSampleRepair(sampleNumber: 1 | 2): void {
    if (sampleNumber === 1) {
      this.repairForm.patchValue({
        description: 'Brake Pad Replacement',
        reason: 'Brake pads show significant wear and should be replaced for safe braking.',
        estimatedAmount: 2000
      });
    } else {
      this.repairForm.patchValue({
        description: 'Battery Replacement',
        reason: 'Battery health is low and replacement is recommended to avoid starting problems.',
        estimatedAmount: 4500
      });
    }
  }

  submitAdditionalRepair(): void {
    if (this.repairForm.invalid) {
      this.repairForm.markAllAsTouched();
      return;
    }

    this.isSubmittingRepair.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.workflowService.createAdditionalRepair(this.bookingId(), this.repairForm.value).subscribe({
      next: () => {
        this.isSubmittingRepair.set(false);
        this.closeRepairModal();
        this.successMessage.set('Additional repair estimate created and sent for customer authorization!');
        this.loadWorkflow();
      },
      error: (err) => {
        this.isSubmittingRepair.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to submit additional repair.');
      }
    });
  }

  isMilestoneActive(key: string): boolean {
    if (this.currentStatus === 'COMPLETED') return false;
    const status = this.currentStatus === 'AWAITING_APPROVAL' ? 'SERVICE_IN_PROGRESS' : this.currentStatus;
    if (!status) return key === 'CONFIRMED';
    return status === key;
  }

  isMilestoneCompleted(key: string): boolean {
    if (this.currentStatus === 'COMPLETED') return true;
    const status = this.currentStatus === 'AWAITING_APPROVAL' ? 'SERVICE_IN_PROGRESS' : this.currentStatus;
    if (!status) return false;
    const order = ['CONFIRMED', 'CAR_RECEIVED', 'INSPECTION', 'SERVICE_IN_PROGRESS', 'QUALITY_CHECK', 'READY_FOR_DELIVERY', 'COMPLETED'];
    return order.indexOf(key) < order.indexOf(status);
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

  downloadVisitPdf(): void {
    const rec = this.completionRecord();
    const wf = this.workflow();
    if (!rec?.id) {
      this.downloadPassportPdf();
      return;
    }

    this.isDownloadingVisitPdf.set(true);
    this.errorMessage.set(null);

    this.historyService.downloadSingleServicePdf(rec.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const rawReg = rec.vehicleRegistrationNumber || wf?.vehicleRegistrationNumber || `Vehicle_${wf?.vehicleId || '0'}`;
        const cleanReg = rawReg.replace(/\s+/g, '_');
        a.download = `ServiceMate_${cleanReg}_Service_Visit_${rec.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.isDownloadingVisitPdf.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to download the service PDF. Please try again.');
        this.isDownloadingVisitPdf.set(false);
      }
    });
  }
}
