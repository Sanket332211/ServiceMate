import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { CustomerDashboardComponent } from './customer/dashboard/customer-dashboard.component';
import { VehicleListComponent } from './customer/vehicles/vehicle-list.component';
import { VehicleServiceHistoryComponent } from './features/customer/vehicle-history/vehicle-service-history.component';
import { BookingListComponent } from './customer/bookings/booking-list/booking-list.component';
import { BookingWizardComponent } from './customer/bookings/booking-wizard/booking-wizard.component';
import { ServiceTrackerComponent } from './features/customer/service-tracker/service-tracker.component';
import { ServiceCenterDashboardComponent } from './service-center/dashboard/service-center-dashboard.component';
import { ServiceCenterBookingsComponent } from './features/service-center/bookings/service-center-bookings.component';
import { ServiceCenterCockpitComponent } from './features/service-center/workflow-cockpit/workflow-cockpit.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'login',
    component: LoginComponent,
    title: 'Sign In — ServiceMate'
  },
  {
    path: 'register',
    component: RegisterComponent,
    title: 'Create Account — ServiceMate'
  },
  {
    path: 'customer/dashboard',
    component: CustomerDashboardComponent,
    canActivate: [authGuard],
    data: { role: 'CUSTOMER' },
    title: 'Customer Dashboard — ServiceMate'
  },
  {
    path: 'customer/vehicles',
    component: VehicleListComponent,
    canActivate: [authGuard],
    data: { role: 'CUSTOMER' },
    title: 'My Vehicles — ServiceMate'
  },
  {
    path: 'customer/vehicles/:vehicleId/history',
    component: VehicleServiceHistoryComponent,
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER', 'SERVICE_CENTER'] },
    title: 'Vehicle Service Passport — ServiceMate'
  },
  {
    path: 'service-center/vehicles/:vehicleId/history',
    component: VehicleServiceHistoryComponent,
    canActivate: [authGuard],
    data: { role: 'SERVICE_CENTER' },
    title: 'Vehicle Service Passport — ServiceMate'
  },
  {
    path: 'customer/bookings',
    component: BookingListComponent,
    canActivate: [authGuard],
    data: { role: 'CUSTOMER' },
    title: 'My Service Bookings — ServiceMate'
  },
  {
    path: 'customer/bookings/new',
    component: BookingWizardComponent,
    canActivate: [authGuard],
    data: { role: 'CUSTOMER' },
    title: 'Book a Service — ServiceMate'
  },
  {
    path: 'customer/service/:bookingId',
    component: ServiceTrackerComponent,
    canActivate: [authGuard],
    data: { role: 'CUSTOMER' },
    title: 'Live Service Tracker — ServiceMate'
  },
  {
    path: 'service-center/dashboard',
    component: ServiceCenterDashboardComponent,
    canActivate: [authGuard],
    data: { role: 'SERVICE_CENTER' },
    title: 'Workshop Administration — ServiceMate'
  },
  {
    path: 'service-center/bookings',
    component: ServiceCenterBookingsComponent,
    canActivate: [authGuard],
    data: { role: 'SERVICE_CENTER' },
    title: 'Workshop Queue — ServiceMate'
  },
  {
    path: 'service-center/workflow/:bookingId',
    component: ServiceCenterCockpitComponent,
    canActivate: [authGuard],
    data: { role: 'SERVICE_CENTER' },
    title: 'Service Bay Cockpit — ServiceMate'
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
