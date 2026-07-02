export interface AuthResponse {
  token: string;
  userId: string;
  role: 'USER' | 'TRAVEL_MANAGER' | 'ADMIN' | 'MANAGER' | 'TRAVELER';
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
  managerId?: string;
}

export interface Feedback {
  id: string;
  tripId: string;
  tripTitle?: string;
  userId: string;
  userEmail?: string;
  userFirstName?: string;
  userLastName?: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface Booking {
  id: string;
  tripId: string;
  tripTitle?: string;
  userId: string;
  status: string;
  paymentId?: string;
  clientSecret?: string;
  createdAt: string;
  tripDepartureDate?: string;
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
  paymentMethod: string;
  createdAt: string;
}

export type PaymentMethod = 'CARD' | 'PAYPAL' | 'BANK_TRANSFER';

export const PAYMENT_METHODS: { value: PaymentMethod; label: string; icon: string; description: string }[] = [
  { value: 'CARD', label: 'Carte bancaire', icon: '💳', description: 'Paiement sécurisé par carte de crédit ou débit' },
  { value: 'PAYPAL', label: 'PayPal', icon: '💰', description: 'Paiement rapide via votre compte PayPal' },
  { value: 'BANK_TRANSFER', label: 'Virement bancaire', icon: '🏦', description: 'Transfert bancaire direct depuis votre compte' }
];

export interface UpdatePaymentPayload {
  amount: number;
  status: string;
}

export interface ManagerStats {
  totalTrips: number;
  totalTravelers: number;
  totalIncome: number;
}

export interface TripAnalytics {
  id: string;
  title: string;
  originCity: string;
  destinationCity: string;
  departureDate: string;
  price: number;
  seatsAvailable: number;
  status: string;
  confirmedBookings: number;
  revenue: number;
  occupancyRate: number;
  averageRating: number;
  feedbackCount: number;
}

export interface ManagerTripSummary {
  id: string;
  title: string;
  originCity: string;
  destinationCity: string;
  departureDate: string;
  price: number;
  status: string;
  confirmedBookings: number;
  averageRating: number;
  feedbackCount: number;
}

export interface ManagerProfile {
  managerId: string;
  firstName: string;
  lastName: string;
  totalTrips: number;
  totalTravelers: number;
  averageRating: number;
  reportCount: number;
  trips: ManagerTripSummary[];
}

export interface AdminReportView {
  id: string;
  reporterId: string;
  reporterFirstName: string;
  reporterLastName: string;
  reporterEmail: string;
  reportedId: string;
  reportedFirstName: string;
  reportedLastName: string;
  reportedEmail: string;
  reportedRole: string;
  tripId?: string;
  reason: string;
  status: string;
  createdAt: string;
}

export interface AdminManagerReportView {
  id: string;
  managerId: string;
  managerFirstName: string;
  managerLastName: string;
  managerEmail: string;
  reporterId: string;
  reporterFirstName: string;
  reporterLastName: string;
  reporterEmail: string;
  reason: string;
  status: string;
  createdAt: string;
}

export interface ManagerPerformance {
  managerId: string;
  name: string;
  email: string;
  tripsCount: number;
  income: number;
  averageRating: number;
  feedbackCount: number;
  performanceScore: number;
}

export interface ReportDetail {
  id: string;
  reporterId: string;
  reporterFirstName: string;
  reporterLastName: string;
  reason: string;
  createdAt: string;
}
