import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ServiceHistoryService } from '../../../core/services/service-history.service';
import { AuthService } from '../../../core/services/auth.service';
import { AiService } from '../../../core/services/ai.service';
import {
  ServiceRecordResponse,
  VehicleServiceHistoryResponse
} from '../../../core/models/service-history.models';

@Component({
  selector: 'app-vehicle-service-history',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './vehicle-service-history.component.html',
  styleUrls: ['./vehicle-service-history.component.css']
})
export class VehicleServiceHistoryComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private historyService = inject(ServiceHistoryService);
  private authService = inject(AuthService);
  private aiService = inject(AiService);

  vehicleId = signal<number>(0);
  history = signal<VehicleServiceHistoryResponse | null>(null);
  isLoading = signal<boolean>(true);
  isDownloadingFullPdf = signal<boolean>(false);
  downloadingRecordId = signal<number | null>(null);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  // AI Service Summary State
  aiSummaryMap = signal<Map<number, string>>(new Map());
  loadingSummaryRecordId = signal<number | null>(null);
  summaryErrorMap = signal<Map<number, string>>(new Map());

  get isServiceCenter(): boolean {
    return this.authService.isServiceCenter();
  }

  get backLink(): string {
    return this.isServiceCenter ? '/service-center/bookings' : '/customer/vehicles';
  }

  get backLabel(): string {
    return this.isServiceCenter ? 'Back to Workshop Queue' : 'Back to My Vehicles';
  }

  searchTerm = signal<string>('');
  expandedRecordIds = signal<Set<number>>(new Set());

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('vehicleId');
    if (idParam) {
      this.vehicleId.set(Number(idParam));
      this.loadHistory();
    } else {
      this.errorMessage.set('Invalid vehicle ID specified.');
      this.isLoading.set(false);
    }
  }

  loadHistory(): void {
    this.isLoading.set(true);
    this.historyService.getVehicleServiceHistory(this.vehicleId()).subscribe({
      next: (data) => {
        this.history.set(data);
        // Expand the first (most recent) record by default
        if (data.records && data.records.length > 0) {
          this.expandedRecordIds.update(set => {
            const next = new Set(set);
            next.add(data.records[0].id);
            return next;
          });
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to load vehicle service history.');
        this.isLoading.set(false);
      }
    });
  }

  toggleRecord(recordId: number): void {
    this.expandedRecordIds.update(set => {
      const next = new Set(set);
      if (next.has(recordId)) {
        next.delete(recordId);
      } else {
        next.add(recordId);
      }
      return next;
    });
  }

  isExpanded(recordId: number): boolean {
    return this.expandedRecordIds().has(recordId);
  }

  expandAll(): void {
    const data = this.history();
    if (data?.records) {
      this.expandedRecordIds.set(new Set(data.records.map(r => r.id)));
    }
  }

  collapseAll(): void {
    this.expandedRecordIds.set(new Set());
  }

  filteredRecords(): ServiceRecordResponse[] {
    const data = this.history();
    if (!data?.records) return [];
    const term = this.searchTerm().toLowerCase().trim();
    if (!term) return data.records;

    return data.records.filter(r =>
      r.serviceTypeDisplayName?.toLowerCase().includes(term) ||
      r.serviceSummary?.toLowerCase().includes(term) ||
      r.serviceDate?.toLowerCase().includes(term) ||
      r.items?.some(i => i.description?.toLowerCase().includes(term)) ||
      r.inspectionFindings?.some(f => f.component?.toLowerCase().includes(term) || f.notes?.toLowerCase().includes(term))
    );
  }

  downloadFullPdf(): void {
    this.isDownloadingFullPdf.set(true);
    this.errorMessage.set(null);

    this.historyService.downloadVehicleHistoryPdf(this.vehicleId()).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const rawReg = this.history()?.vehicleRegistrationNumber || `Vehicle_${this.vehicleId()}`;
        const cleanReg = rawReg.replace(/\s+/g, '_');
        a.download = `ServiceMate_${cleanReg}_Service_History.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.isDownloadingFullPdf.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to download the service PDF. Please try again.');
        this.isDownloadingFullPdf.set(false);
      }
    });
  }

  downloadSinglePdf(record: ServiceRecordResponse, event: MouseEvent): void {
    event.stopPropagation();
    this.downloadingRecordId.set(record.id);
    this.errorMessage.set(null);

    this.historyService.downloadSingleServicePdf(record.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const rawReg = record.vehicleRegistrationNumber || this.history()?.vehicleRegistrationNumber || `Vehicle_${this.vehicleId()}`;
        const cleanReg = rawReg.replace(/\s+/g, '_');
        a.download = `ServiceMate_${cleanReg}_Service_Visit_${record.id}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.downloadingRecordId.set(null);
      },
      error: () => {
        this.errorMessage.set('Unable to download the service PDF. Please try again.');
        this.downloadingRecordId.set(null);
      }
    });
  }

  generateAiSummary(recordId: number, event?: MouseEvent): void {
    if (event) event.stopPropagation();
    this.loadingSummaryRecordId.set(recordId);
    this.summaryErrorMap.update(m => {
      const next = new Map(m);
      next.delete(recordId);
      return next;
    });

    this.aiService.getServiceSummary(recordId).subscribe({
      next: (res) => {
        this.aiSummaryMap.update(m => {
          const next = new Map(m);
          next.set(recordId, res.summary);
          return next;
        });
        this.loadingSummaryRecordId.set(null);
      },
      error: (err) => {
        let msg = err.error?.message;
        if (!msg) {
          if (err.status === 503) {
            msg = 'AI Service Summary is temporarily unavailable. Please try again later.';
          } else if (err.status === 403) {
            msg = 'You do not have permission to generate an AI summary for this service record.';
          } else if (err.status === 400) {
            msg = 'AI summary is only available for finalized service visits.';
          } else {
            msg = 'Unable to generate AI service summary. Please try again later.';
          }
        }
        this.summaryErrorMap.update(m => {
          const next = new Map(m);
          next.set(recordId, msg);
          return next;
        });
        this.loadingSummaryRecordId.set(null);
      }

    });
  }

  getAiSummary(recordId: number): string | undefined {
    return this.aiSummaryMap().get(recordId);
  }

  getAiSummaryError(recordId: number): string | undefined {
    return this.summaryErrorMap().get(recordId);
  }

  isGeneratingSummary(recordId: number): boolean {
    return this.loadingSummaryRecordId() === recordId;
  }

  getConditionBadgeClass(condition: string): string {
    const c = (condition || '').toLowerCase();
    if (c.includes('good') || c.includes('pass') || c.includes('ok')) {
      return 'badge-condition-good';
    }
    if (c.includes('fair') || c.includes('moderate')) {
      return 'badge-condition-fair';
    }
    if (c.includes('attention') || c.includes('warn') || c.includes('replace') || c.includes('fail')) {
      return 'badge-condition-warn';
    }
    return 'badge-condition-neutral';
  }
}

