export const Role = {
  ROLE_USER: 'ROLE_USER',
  ROLE_MANAGER: 'ROLE_MANAGER',
  ROLE_ADMIN: 'ROLE_ADMIN',
} as const;

export type Role = (typeof Role)[keyof typeof Role];

export const OrderStatus = {
  CREATED: 'CREATED',
  VALIDATED: 'VALIDATED',
  PAID: 'PAID',
  PROCESSING: 'PROCESSING',
  SHIPPED: 'SHIPPED',
  DELIVERED: 'DELIVERED',
  CANCELLED: 'CANCELLED',
  FAILED: 'FAILED',
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

export const PaymentStatus = {
  PENDING: 'PENDING',
  CAPTURED: 'CAPTURED',
  DECLINED: 'DECLINED',
  REFUNDED: 'REFUNDED',
} as const;

export type PaymentStatus = (typeof PaymentStatus)[keyof typeof PaymentStatus];

export interface User {
  username: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: Role;
}

export interface Product {
  id: number;
  name: string;
  price: number;
  quantity: number;
  reservedQuantity: number;
  availableQuantity: number;
  version: number;
}

export interface OrderItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface OrderEvent {
  eventType: string;
  fromStatus?: OrderStatus;
  toStatus: OrderStatus;
  actor: string;
  correlationId?: string;
  note?: string;
  createdAt: string;
}

export interface Order {
  id: number;
  customer: string;
  totalAmount: number;
  status: OrderStatus;
  paymentStatus: PaymentStatus;
  idempotencyKey?: string;
  correlationId?: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  items: OrderItem[];
  history: OrderEvent[];
}

export interface Page<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
