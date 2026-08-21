import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { WorkflowService } from '../../../core/services/workflow.service';
import { WorkflowResponse, WorkflowStatus } from '../../../core/models/workflow.models';

@Component({
  selector: 'app-service-center-bookings',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './service-center-bookings.component.html',
  styleUrls: ['./service-center-bookings.component.css']
})
export class ServiceCenterBookingsComponent implements OnInit {
  private workflowService = inject(WorkflowService);

  workflows = signal<WorkflowResponse[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);
  filterStatus = signal<string>('ALL');
  searchQuery = signal<string>('');

  filteredWorkflows = computed(() => {
    let list = [...this.workflows()];
    list.sort((a, b) => {
      if (a.createdAt && b.createdAt) {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }
      return b.bookingId - a.bookingId;
    });

    const status = this.filterStatus();
    const query = this.searchQuery().toLowerCase().trim();

    if (status !== 'ALL') {
      if (status === 'CANCELLED') {
        list = list.filter((w) => w.bookingStatus === 'CANCELLED');
      } else if (status === 'NOT_RECEIVED') {
        list = list.filter((w) => !w.workflowStatus && w.bookingStatus !== 'CANCELLED');
      } else {
        list = list.filter((w) => w.workflowStatus === status && w.bookingStatus !== 'CANCELLED');
      }
    }

    if (query) {
      list = list.filter(
        (w) =>
          w.customerName.toLowerCase().includes(query) ||
          w.vehicleRegistrationNumber.toLowerCase().includes(query) ||
          w.vehicleMake.toLowerCase().includes(query) ||
          w.vehicleModel.toLowerCase().includes(query) ||
          String(w.bookingId).includes(query)
      );
    }

    return list;
  });

  // Metrics (strictly exclude cancelled bookings from active operational bay metrics)
  totalActiveCount = computed(() => this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus !== 'COMPLETED').length);
  inProgressCount = computed(() => this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'SERVICE_IN_PROGRESS').length);
  awaitingApprovalCount = computed(() => this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'AWAITING_APPROVAL').length);
  readyForDeliveryCount = computed(() => this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'READY_FOR_DELIVERY').length);

  ngOnInit(): void {
    this.loadWorkflows();
  }

  loadWorkflows(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.workflowService.loadServiceCenterWorkflows().subscribe({
      next: (data) => {
        this.workflows.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Failed to load workshop operations queue.');
        this.isLoading.set(false);
      }
    });
  }

  getStatusBadgeClass(status?: WorkflowStatus): string {
    switch (status) {
      case 'CAR_RECEIVED':
        return 'bg-info text-dark';
      case 'INSPECTION':
        return 'bg-primary';
      case 'SERVICE_IN_PROGRESS':
        return 'bg-accent';
      case 'AWAITING_APPROVAL':
        return 'bg-warning text-dark';
      case 'QUALITY_CHECK':
        return 'bg-secondary';
      case 'READY_FOR_DELIVERY':
        return 'bg-success';
      case 'COMPLETED':
        return 'bg-dark';
      default:
        return 'bg-light text-dark border';
    }
  }
}
