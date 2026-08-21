import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { BookingRequest, BookingResponse, SlotAvailabilityResponse } from '../models/booking.models';
import { environment } from '../../../environments/environment';

/**
 * BookingService
 *
 * Manages customer service booking operations, real-time slot capacity checks,
 * cancellation, and reactive state management.
 */
@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private readonly API_URL = `${environment.apiUrl}/bookings`;

  bookingsSignal = signal<BookingResponse[]>([]);
  isLoading = signal(false);

  constructor(private http: HttpClient) {}

  /**
   * Fetches real-time capacity and availability details for all 4 fixed slots on a requested date.
   */
  getAvailability(date: string): Observable<SlotAvailabilityResponse[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<SlotAvailabilityResponse[]>(`${this.API_URL}/availability`, { params });
  }

  /**
   * Submits a new capacity-controlled service booking for the authenticated customer.
   */
  createBooking(request: BookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(this.API_URL, request).pipe(
      tap((newBooking) => {
        this.bookingsSignal.update((list) => [newBooking, ...list]);
      })
    );
  }

  /**
   * Loads all bookings belonging to the authenticated customer.
   */
  loadMyBookings(): Observable<BookingResponse[]> {
    this.isLoading.set(true);
    return this.http.get<BookingResponse[]>(`${this.API_URL}/my`).pipe(
      tap({
        next: (bookings) => {
          this.bookingsSignal.set(bookings);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  /**
   * Fetches a specific booking by ID.
   */
  getBookingById(id: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${this.API_URL}/${id}`);
  }

  /**
   * Cancels a customer's booking and updates reactive signals.
   */
  cancelBooking(id: number): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(`${this.API_URL}/${id}/cancel`, {}).pipe(
      tap((updated) => {
        this.bookingsSignal.update((list) =>
          list.map((b) => (b.id === id ? updated : b))
        );
      })
    );
  }
}
