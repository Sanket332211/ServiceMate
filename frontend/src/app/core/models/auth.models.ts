export type Role = 'CUSTOMER' | 'SERVICE_CENTER';

export interface User {
  id: number;
  name: string;
  email: string;
  phone?: string;
  role: Role;
  createdAt?: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone?: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
  role: Role;
  message: string;
}

export interface UserProfileResponse {
  id: number;
  name: string;
  email: string;
  phone?: string;
  role: Role;
  createdAt: string;
}

export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data?: T;
  status?: number;
}
