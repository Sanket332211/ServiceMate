import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  ElementRef,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationResponse, NotificationType } from '../../../core/models/workflow.models';

/**
 * NotificationBellComponent
 *
 * Standalone notification bell widget for top navigation.
 * Displays real-time unread badge, interactive popup list,
 * read/unread state management, and role-aware smart navigation.
 */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css']
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  public notificationService = inject(NotificationService);
  public authService = inject(AuthService);
  private router = inject(Router);
  private elementRef = inject(ElementRef);

  isOpen = signal<boolean>(false);
  private pollSub?: Subscription;

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.notificationService.loadMyNotifications().subscribe({
        error: (err) => console.warn('Failed to load initial notifications', err)
      });
      // Synchronize notifications every 15 seconds
      this.pollSub = interval(15000).subscribe(() => {
        if (this.authService.isAuthenticated()) {
          this.notificationService.loadMyNotifications().subscribe({
            error: () => {}
          });
        }
      });
    }
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  toggleDropdown(event: Event): void {
    event.stopPropagation();
    this.isOpen.update((v) => !v);
  }

  closeDropdown(): void {
    this.isOpen.set(false);
  }

  onNotificationClick(notif: NotificationResponse): void {
    if (!notif.isRead) {
      this.notificationService.markAsRead(notif.id).subscribe({
        error: (err) => console.warn('Failed to mark notification as read', err)
      });
    }

    this.closeDropdown();

    if (notif.relatedBookingId) {
      if (this.authService.isCustomer()) {
        this.router.navigate(['/customer/service', notif.relatedBookingId]);
      } else if (this.authService.isServiceCenter()) {
        this.router.navigate(['/service-center/workflow', notif.relatedBookingId]);
      }
    }
  }

  onMarkAllAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe({
      error: (err) => console.warn('Failed to mark all as read', err)
    });
  }

  getIconClass(type: NotificationType): string {
    switch (type) {
      case 'BOOKING_CONFIRMED':
        return 'bi-calendar-check-fill text-success';
      case 'BOOKING_CANCELLED':
        return 'bi-calendar-x-fill text-danger';
      case 'SERVICE_STATUS_UPDATED':
        return 'bi-tools text-primary';
      case 'REPAIR_REQUESTED':
        return 'bi-exclamation-triangle-fill text-warning';
      case 'REPAIR_APPROVED':
        return 'bi-check-circle-fill text-success';
      case 'REPAIR_REJECTED':
        return 'bi-x-circle-fill text-danger';
      case 'VEHICLE_READY':
        return 'bi-car-front-fill text-info';
      case 'SERVICE_COMPLETED':
        return 'bi-shield-fill-check text-success';
      default:
        return 'bi-bell-fill text-secondary';
    }
  }

  formatTimeAgo(dateStr: string): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffSec = Math.floor(diffMs / 1000);
      const diffMin = Math.floor(diffSec / 60);
      const diffHour = Math.floor(diffMin / 60);
      const diffDay = Math.floor(diffHour / 24);

      if (diffSec < 45) return 'Just now';
      if (diffMin < 60) return `${diffMin}m ago`;
      if (diffHour < 24) return `${diffHour}h ago`;
      if (diffDay === 1) return 'Yesterday';
      if (diffDay < 7) return `${diffDay}d ago`;
      return date.toLocaleDateString('en-IN', { month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  }
}
