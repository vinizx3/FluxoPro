import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { PageResponse } from './opportunity';

export type ProjectStatus = 'PLANNING' | 'IN_PROGRESS' | 'DELIVERED' | 'CANCELLED';

export const PROJECT_STATUSES: ProjectStatus[] = [
  'PLANNING',
  'IN_PROGRESS',
  'DELIVERED',
  'CANCELLED',
];

export const PROJECT_STATUS_LABEL: Record<ProjectStatus, string> = {
  PLANNING: 'Planejamento',
  IN_PROGRESS: 'Em andamento',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

/** Classe visual da etiqueta de cada etapa (definida em styles.scss). */
export const PROJECT_STATUS_PILL: Record<ProjectStatus, string> = {
  PLANNING: 'planning',
  IN_PROGRESS: 'progress',
  DELIVERED: 'delivered',
  CANCELLED: 'cancelled',
};

/** Espelha ProjectStatus.canMoveTo do backend. O backend continua sendo quem decide. */
const ALLOWED: Record<ProjectStatus, ProjectStatus[]> = {
  PLANNING: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['DELIVERED', 'CANCELLED'],
  DELIVERED: [],
  CANCELLED: [],
};

export function allowedProjectTargets(status: ProjectStatus): ProjectStatus[] {
  return ALLOWED[status];
}

export function isProjectOpen(status: ProjectStatus): boolean {
  return status === 'PLANNING' || status === 'IN_PROGRESS';
}

/** Hoje no formato yyyy-MM-dd, no fuso do navegador (o mesmo formato do LocalDate do backend). */
export function todayIso(now: Date = new Date()): string {
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** Atrasado: ainda aberto e com a data prevista de entrega já passada. */
export function isOverdue(
  status: ProjectStatus,
  expectedDeliveryDate: string | null,
  today: string,
): boolean {
  return isProjectOpen(status) && !!expectedDeliveryDate && expectedDeliveryDate < today;
}

export interface Project {
  id: string;
  customerId: string;
  customerName: string;
  sourceOpportunityId: string | null;
  ownerId: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  statusChangedAt: string;
  expectedDeliveryDate: string | null;
  createdAt: string;
  archivedAt: string | null;
  version: number;
}

export interface ProjectCreateRequest {
  customerId: string;
  name: string;
  description: string | null;
  expectedDeliveryDate: string | null;
}

export interface ProjectUpdateRequest {
  name: string;
  description: string | null;
  expectedDeliveryDate: string | null;
  version: number;
}

@Injectable({ providedIn: 'root' })
export class ProjectApi {
  private readonly http = inject(HttpClient);

  list(status: ProjectStatus | null, page: number, size = 20) {
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<PageResponse<Project>>(`${API_BASE}/projects`, { params });
  }

  get(id: string) {
    return this.http.get<Project>(`${API_BASE}/projects/${id}`);
  }

  create(body: ProjectCreateRequest) {
    return this.http.post<Project>(`${API_BASE}/projects`, body);
  }

  update(id: string, body: ProjectUpdateRequest) {
    return this.http.put<Project>(`${API_BASE}/projects/${id}`, body);
  }

  move(id: string, body: { status: ProjectStatus; version: number }) {
    return this.http.patch<Project>(`${API_BASE}/projects/${id}/status`, body);
  }

  archive(id: string) {
    return this.http.post<void>(`${API_BASE}/projects/${id}/archive`, {});
  }
}
