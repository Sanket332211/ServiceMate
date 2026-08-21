import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { WorkflowService } from '../../core/services/workflow.service';
import { WorkflowResponse, WorkflowStatus } from '../../core/models/workflow.models';

/**
 * ServiceCenterDashboardComponent
 *
 * Operational workshop dashboard displaying real-time service KPIs,
 * live workshop bay previews, and administrative navigation.
 */
@Component({
  selector: 'app-service-center-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './service-center-dashboard.component.html',
  styleUrls: ['./service-center-dashboard.component.css']
})
export class ServiceCenterDashboardComponent implements OnInit {
  public authService = inject(AuthService);
  private workflowService = inject(WorkflowService);

  workflows = signal<WorkflowResponse[]>([]);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // 1. Total Bookings
  totalBookingsCount = computed(() => this.workflows().length);

  // 2. Cars Received (strictly exclude cancelled bookings)
  carsReceivedCount = computed(() =>
    this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'CAR_RECEIVED').length
  );

  // 3. In Service (strictly exclude cancelled bookings)
  inServiceCount = computed(() =>
    this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'SERVICE_IN_PROGRESS').length
  );

  // 4. Awaiting Approval (strictly exclude cancelled bookings)
  awaitingApprovalCount = computed(() =>
    this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'AWAITING_APPROVAL').length
  );

  // 5. Ready for Delivery (strictly exclude cancelled bookings)
  readyForDeliveryCount = computed(() =>
    this.workflows().filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus === 'READY_FOR_DELIVERY').length
  );

  // Active Workshop Vehicles: Vehicles currently in the bays (not completed and not cancelled)
  activeWorkshopVehicles = computed(() => {
    return this.workflows()
      .filter((w) => w.bookingStatus !== 'CANCELLED' && w.workflowStatus && w.workflowStatus !== 'COMPLETED')
      .slice(0, 5);
  });

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
        this.errorMessage.set(err.error?.message || 'Failed to load workshop operations data.');
        this.isLoading.set(false);
      }
    });
  }

  onLogout(): void {
    this.authService.logout();
  }

  getStatusBadgeClass(status?: WorkflowStatus): string {
    switch (status) {
      case 'CAR_RECEIVED':
        return 'bg-info text-dark';
      case 'INSPECTION':
        return 'bg-primary text-white';
      case 'SERVICE_IN_PROGRESS':
        return 'bg-accent text-white';
      case 'AWAITING_APPROVAL':
        return 'bg-warning text-dark';
      case 'QUALITY_CHECK':
        return 'bg-secondary text-white';
      case 'READY_FOR_DELIVERY':
        return 'bg-success text-white';
      case 'COMPLETED':
        return 'bg-dark text-white';
      default:
        return 'bg-light text-dark border';
    }
  }

  getStatusDisplayName(status?: WorkflowStatus): string {
    switch (status) {
      case 'CAR_RECEIVED':
        return 'Car Received';
      case 'INSPECTION':
        return 'Inspection';
      case 'SERVICE_IN_PROGRESS':
        return 'In Service';
      case 'AWAITING_APPROVAL':
        return 'Awaiting Approval';
      case 'QUALITY_CHECK':
        return 'Quality Check';
      case 'READY_FOR_DELIVERY':
        return 'Ready for Delivery';
      case 'COMPLETED':
        return 'Completed';
      default:
        return 'Not Received';
    }
  }
}
