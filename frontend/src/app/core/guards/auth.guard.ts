import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.models';

/**
 * authGuard
 *
 * Prevents unauthenticated access to protected routes.
 * If user is not logged in, redirects immediately to /login.
 * If route specifies expected role in data: { role: 'CUSTOMER' | 'SERVICE_CENTER' },
 * validates that user possesses the expected role, or redirects to their authorized dashboard.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Check if user is authenticated
  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  // 2. Check if route requires a specific role or list of allowed roles
  const requiredRole = route.data?.['role'] as Role | undefined;
  const allowedRoles = route.data?.['roles'] as Role[] | undefined;

  if (requiredRole && !authService.hasRole(requiredRole)) {
    // Role mismatch: redirect user to their permitted role dashboard
    authService.navigateToRoleDashboard();
    return false;
  }

  if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.some((r) => authService.hasRole(r))) {
    authService.navigateToRoleDashboard();
    return false;
  }

  return true;
};
