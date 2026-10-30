import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { API_BASE } from './api';

export type Role = 'OWNER' | 'SELLER';

/** O que o front precisa saber sobre quem está logado, lido do próprio token. */
export interface Session {
  userId: string;
  companyId: string;
  role: Role;
  /** Instante de expiração, em milissegundos. */
  expiresAt: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  companyName: string;
  name: string;
  email: string;
  password: string;
}

interface AuthResponse {
  accessToken: string;
}

const TOKEN_KEY = 'fluxopro.token';

function decodePayload(token: string): Record<string, unknown> | null {
  const part = token.split('.')[1];
  if (!part) {
    return null;
  }
  try {
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
    const bytes = atob(base64);
    const json = decodeURIComponent(
      Array.from(bytes, (c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join(''),
    );
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/**
 * Lê o papel e a empresa do token só para decidir o que MOSTRAR na tela.
 * Isto NÃO é segurança: quem autoriza de verdade é o backend (403 e 404).
 */
export function parseToken(token: string | null, now: number = Date.now()): Session | null {
  if (!token) {
    return null;
  }
  const payload = decodePayload(token);
  if (!payload) {
    return null;
  }
  const userId = payload['sub'];
  const companyId = payload['companyId'];
  const role = payload['role'];
  const exp = payload['exp'];
  if (
    typeof userId !== 'string' ||
    typeof companyId !== 'string' ||
    (role !== 'OWNER' && role !== 'SELLER') ||
    typeof exp !== 'number'
  ) {
    return null;
  }
  const expiresAt = exp * 1000;
  return expiresAt > now ? { userId, companyId, role, expiresAt } : null;
}

function readStoredToken(): string | null {
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token && !parseToken(token)) {
      localStorage.removeItem(TOKEN_KEY);
      return null;
    }
    return token;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly tokenState = signal<string | null>(readStoredToken());

  readonly token = this.tokenState.asReadonly();
  readonly session = computed(() => parseToken(this.tokenState()));
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isOwner = computed(() => this.session()?.role === 'OWNER');
  readonly roleLabel = computed(() => (this.isOwner() ? 'Dono' : 'Vendedor'));

  login(request: LoginRequest) {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, request)
      .pipe(tap((response) => this.storeToken(response.accessToken)));
  }

  register(request: RegisterRequest) {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/register`, request)
      .pipe(tap((response) => this.storeToken(response.accessToken)));
  }

  logout(): void {
    this.storeToken(null);
    void this.router.navigate(['/entrar']);
  }

  private storeToken(token: string | null): void {
    try {
      if (token) {
        localStorage.setItem(TOKEN_KEY, token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
    } catch {
      // Sem armazenamento (janela privada, por exemplo): a sessão vale só até recarregar.
    }
    this.tokenState.set(token);
  }
}
