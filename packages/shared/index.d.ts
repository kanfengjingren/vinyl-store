// ═══════════════════════════════════════════════
// @vinyl-store/shared — TypeScript 类型声明
// ═══════════════════════════════════════════════

// ── Axios instance ──────────────────────────
import type { AxiosInstance } from 'axios';

export const api: AxiosInstance;

// ── Data Models ─────────────────────────────

export interface User {
  id: number;
  email: string;
  name?: string;
  role: 'CUSTOMER' | 'SELLER' | 'ADMIN';
  defaultAddress?: string;
  balance: number;
  seller?: Seller;
}

export interface Seller {
  id: number;
  userId: number;
  storeName: string;
  contactEmail?: string;
  contactPhone?: string;
  description?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  balance: number;
  createdAt: string;
  updatedAt: string;
}

export interface Album {
  id: number;
  artist: string;
  title: string;
  year?: number;
  label?: string;
  country?: string;
  price: number;
  badge?: string;
  description?: string;
  coverUrl?: string;
  gradient?: string;
  slug: string;
  stock: number;
  status: 'ACTIVE' | 'DELISTED';
  sellerId?: number;
  categories?: Category[];
  trackCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface Track {
  id: number;
  albumId: number;
  title: string;
  duration?: string;
  position: number;
  isSection: boolean;
}

export interface Order {
  id: number;
  userId: number;
  status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  shippingAddress?: string;
  expiresAt?: string;
  user?: Pick<User, 'id' | 'name' | 'email'>;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: number;
  orderId: number;
  albumId?: number;
  quantity: number;
  unitPrice: number;
  status: 'ACTIVE' | 'SHIPPED' | 'REFUNDED';
  album?: Pick<Album, 'id' | 'artist' | 'title' | 'coverUrl' | 'gradient' | 'slug' | 'status'>;
}

// ── Pagination ──────────────────────────────

export interface Pagination {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export interface PaginatedResponse<T = Album> {
  data: T[];
  pagination: Pagination;
}

// ── API Inputs / Outputs ────────────────────

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput {
  email: string;
  password: string;
  name?: string;
  role?: 'CUSTOMER' | 'SELLER';
  storeName?: string;
  contactEmail?: string;
  contactPhone?: string;
  description?: string;
}

export interface AuthResponse {
  user: User;
  token: string;
}

export interface CreateAlbumInput {
  artist: string;
  title: string;
  price: number;
  slug: string;
  year?: number;
  country?: string;
  badge?: string;
  description?: string;
  coverUrl?: string;
  gradient?: string;
  stock?: number;
}

export interface UpdateAlbumInput {
  artist?: string;
  title?: string;
  price?: number;
  slug?: string;
  year?: number;
  country?: string;
  badge?: string;
  description?: string;
  coverUrl?: string;
  gradient?: string;
  stock?: number;
}

export interface QueryAlbumsParams {
  category?: string;
  search?: string;
  sort?: string;
  order?: 'asc' | 'desc';
  page?: number;
  limit?: number;
}

// ── API Functions ───────────────────────────

// auth
export function login(data: LoginInput): Promise<AuthResponse>;
export function register(data: RegisterInput): Promise<AuthResponse>;
export function getMe(): Promise<User>;
export function fetchProfile(): Promise<User>;
export function updateProfile(data: { defaultAddress: string }): Promise<User>;
export function changePassword(data: { oldPassword: string; newPassword: string }): Promise<{ message: string }>;

// albums
export function fetchAlbums(params?: QueryAlbumsParams): Promise<PaginatedResponse<Album>>;
export function fetchAlbumBySlug(slug: string): Promise<Album & { tracks: Track[] }>;
export function createAlbum(data: CreateAlbumInput): Promise<Album>;
export function updateAlbum(id: number, data: UpdateAlbumInput): Promise<Album>;
export function deleteAlbum(id: number): Promise<void>;

// orders
export function checkout(address?: string): Promise<Order>;
export function fetchOrders(): Promise<Order[]>;
export function fetchOrderById(id: number): Promise<Order>;
export function cancelOrder(id: number): Promise<Order>;
export function payOrder(id: number): Promise<Order>;
export function fetchSellerOrders(): Promise<Order[]>;
export function shipOrder(id: number): Promise<Order>;
export function refundOrderItem(orderId: number, itemId: number): Promise<{ message: string; refundAmount: number }>;

// cart
export function fetchCart(): Promise<any>;
export function addToCart(albumId: number, quantity?: number): Promise<any>;
export function updateCartItem(itemId: number, quantity: number): Promise<any>;
export function removeCartItem(id: number): Promise<any>;

// categories
export function fetchCategories(): Promise<Category[]>;

// upload
export function uploadCover(file: File): Promise<string>;

// admin
export function fetchSellers(status?: string): Promise<Seller[]>;
export function approveSeller(id: number): Promise<Seller>;
export function rejectSeller(id: number): Promise<Seller>;

// wallet
export function recharge(amount: number): Promise<{ balance: number }>;

// ── UI Components ───────────────────────────

declare const AppModal: import('vue').DefineComponent<{}, {}, any>;
declare const ToastNotification: import('vue').DefineComponent<{}, {}, any>;

export { AppModal, ToastNotification };

// ── Stores ──────────────────────────────────

export interface ModalStore {
  visible: import('vue').Ref<boolean>;
  message: import('vue').Ref<string>;
  confirmText: import('vue').Ref<string>;
  cancelText: import('vue').Ref<string>;
  showCancel: import('vue').Ref<boolean>;
  open(options?: {
    message?: string;
    confirmText?: string;
    cancelText?: string;
    showCancel?: boolean;
  }): Promise<boolean>;
  confirm(): void;
  cancel(): void;
}

export function useModalStore(): ModalStore;
