import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, Role, User, UserProfileResponse } from '../models/auth.models';
import { environment } from '../../../environments/environment';

/**
 * AuthService
 *
 * Manages user authentication state, JWT storage, login, registration, and logout operations.
 */
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'servicemate_jwt_token';
  private readonly USER_KEY = 'servicemate_user';

  // Signals for reactive auth state
  currentUserSignal = signal<User | null>(this.getStoredUser());
  tokenSignal = signal<string | null>(this.getStoredToken());

  // Computed signals
  isAuthenticated = computed(() => !!this.tokenSignal());
  isCustomer = computed(() => this.currentUserSignal()?.role === 'CUSTOMER');
  isServiceCenter = computed(() => this.currentUserSignal()?.role === 'SERVICE_CENTER');

  constructor(
    private http: HttpClient,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  /**
   * Registers a new customer account.
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, request).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  /**
   * Logs in an existing user and stores the session token.
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, request).pipe(
      tap((response) => this.handleAuthSuccess(response))
    );
  }

  /**
   * Fetches current authenticated profile from backend.
   */
  getProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.API_URL}/me`);
  }

  /**
   * Logs out the user, clears storage, and redirects to login.
   */
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.tokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.notificationService.clearOnLogout();
    this.router.navigate(['/login']);
  }

  /**
   * Returns current JWT token string or null.
   */
  getToken(): string | null {
    return this.tokenSignal();
  }

  /**
   * Returns current authenticated user object or null.
   */
  getCurrentUser(): User | null {
    return this.currentUserSignal();
  }

  /**
   * Checks if user possesses a specific role.
   */
  hasRole(role: Role): boolean {
    return this.currentUserSignal()?.role === role;
  }

  /**
   * Navigates to appropriate dashboard based on user role.
   */
  navigateToRoleDashboard(role?: Role): void {
    const targetRole = role || this.currentUserSignal()?.role;
    if (targetRole === 'SERVICE_CENTER') {
      this.router.navigate(['/service-center/dashboard']);
    } else {
      this.router.navigate(['/customer/dashboard']);
    }
  }

  private handleAuthSuccess(response: AuthResponse): void {
    const user: User = {
      id: response.userId,
      name: response.name,
      email: response.email,
      role: response.role
    };

    localStorage.setItem(this.TOKEN_KEY, response.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));

    this.tokenSignal.set(response.token);
    this.currentUserSignal.set(user);
  }

  private getStoredToken(): string | null {
    return typeof localStorage !== 'undefined' ? localStorage.getItem(this.TOKEN_KEY) : null;
  }

  private getStoredUser(): User | null {
    if (typeof localStorage === 'undefined') return null;
    const stored = localStorage.getItem(this.USER_KEY);
    if (!stored) return null;
    try {
      return JSON.parse(stored);
    } catch {
      return null;
    }
  }
}
