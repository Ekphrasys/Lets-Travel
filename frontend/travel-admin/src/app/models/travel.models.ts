export interface AuthResponse {
  token: string;
  userId: string;
  role: 'USER' | 'ADMIN';
}

export interface Trip {
  id: string;
  title: string;
  originCity: string;
  destinationCity: string;
  departureDate: string;
  price: number;
  seatsAvailable: number;
  status: string;
}

export interface Booking {
  id: string;
  tripId: string;
  userId: string;
  status: string;
  paymentId?: string;
  createdAt: string;
}

export interface RoutePath {
  cities: string[];
  totalDurationMin: number;
  totalPrice: number;
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface CreateUserPayload {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface UpdateUserPayload {
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface Payment {
  id: string;
  bookingId: string;
  userId: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface UpdatePaymentPayload {
  amount: number;
  status: string;
}
