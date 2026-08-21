import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/auth.models';
import { Vehicle, VehicleRequest } from '../models/vehicle.models';
import { environment } from '../../../environments/environment';

/**
 * VehicleService
 *
 * Manages customer vehicle CRUD operations and reactive state.
 */
@Injectable({
  providedIn: 'root'
})
export class VehicleService {
  private readonly API_URL = `${environment.apiUrl}/vehicles`;

  vehiclesSignal = signal<Vehicle[]>([]);
  isLoading = signal(false);

  constructor(private http: HttpClient) {}

  /**
   * Fetches all vehicles owned by the authenticated customer.
   */
  loadMyVehicles(): Observable<Vehicle[]> {
    this.isLoading.set(true);
    return this.http.get<Vehicle[]>(this.API_URL).pipe(
      tap({
        next: (vehicles) => {
          this.vehiclesSignal.set(vehicles);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      })
    );
  }

  /**
   * Creates a new vehicle for the authenticated customer.
   */
  createVehicle(request: VehicleRequest): Observable<Vehicle> {
    return this.http.post<Vehicle>(this.API_URL, request).pipe(
      tap((newVehicle) => {
        this.vehiclesSignal.update((list) => [newVehicle, ...list]);
      })
    );
  }

  /**
   * Updates an existing vehicle.
   */
  updateVehicle(id: number, request: VehicleRequest): Observable<Vehicle> {
    return this.http.put<Vehicle>(`${this.API_URL}/${id}`, request).pipe(
      tap((updated) => {
        this.vehiclesSignal.update((list) =>
          list.map((v) => (v.id === id ? updated : v))
        );
      })
    );
  }

  /**
   * Deletes a vehicle by ID.
   */
  deleteVehicle(id: number): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(`${this.API_URL}/${id}`).pipe(
      tap(() => {
        this.vehiclesSignal.update((list) => list.filter((v) => v.id !== id));
      })
    );
  }
}
