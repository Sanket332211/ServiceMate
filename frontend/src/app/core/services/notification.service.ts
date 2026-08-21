import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { NotificationResponse } from '../models/workflow.models';
import { environment } from '../../../environments/environment';

/**
 * NotificationService
 *
 * Manages user in-app notifications and real-time unread badge counts.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly API_URL = `${environment.apiUrl}/notifications`;

  notificationsSignal = signal<NotificationResponse[]>([]);
  unreadCountSignal = signal<number>(0);
  isLoading = signal(false);

  constructor(private http: HttpClient) {}

  /**
   * Fetches all notifications for the authenticated user.
   */
  loadMyNotifications(): Observable<NotificationResponse[]> {
    this.isLoading.set(true);
    return this.http.get<NotificationResponse[]>(`${this.API_URL}/my`).pipe(
      tap({
        next: (list) => {
          this.notificationsSignal.set(list);
          this.unreadCountSignal.set(list.filter((n) => !n.isRead).length);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  /**
   * Marks a notification as read and decrements unread count.
   */
  markAsRead(id: number): Observable<NotificationResponse> {
    return this.http.patch<NotificationResponse>(`${this.API_URL}/${id}/read`, {}).pipe(
      tap((updated) => {
        this.notificationsSignal.update((list) =>
          list.map((n) => (n.id === id ? updated : n))
        );
        this.unreadCountSignal.update((count) => Math.max(0, count - 1));
      })
    );
  }

  /**
   * Marks all notifications for the authenticated user as read.
   */
  markAllAsRead(): Observable<any> {
    return this.http.patch(`${this.API_URL}/read-all`, {}).pipe(
      tap(() => {
        this.notificationsSignal.update((list) =>
          list.map((n) => ({ ...n, isRead: true }))
        );
        this.unreadCountSignal.set(0);
      })
    );
  }

  /**
   * Adds an incoming real-time notification to the local list and updates unread badge.
   */
  addIncomingNotification(notification: NotificationResponse): void {
    this.notificationsSignal.update((list) => {
      if (list.some((n) => n.id === notification.id)) {
        return list;
      }
      return [notification, ...list];
    });
    if (!notification.isRead) {
      this.unreadCountSignal.update((count) => count + 1);
    }
  }

  /**
   * Fetches the current unread notifications count.
   */
  fetchUnreadCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.API_URL}/unread-count`).pipe(
      tap((res) => this.unreadCountSignal.set(res.unreadCount))
    );
  }

  /**
   * Cleans up notification state on user sign out.
   */
  clearOnLogout(): void {
    this.notificationsSignal.set([]);
    this.unreadCountSignal.set(0);
    this.isLoading.set(false);
  }
}
