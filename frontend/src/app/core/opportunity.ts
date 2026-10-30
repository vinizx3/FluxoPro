import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';

export type OpportunityStatus = 'LEAD' | 'QUALIFICATION' | 'PROPOSAL' | 'NEGOTIATION' | 'WON' | 'LOST';

/** Etapas abertas, na ordem das colunas do funil. */
export const OPEN_STATUSES: OpportunityStatus[] = ['LEAD', 'QUALIFICATION', 'PROPOSAL', 'NEGOTIATION'];

export const STATUS_LABEL: Record<OpportunityStatus, string> = {
  LEAD: 'Lead',
  QUALIFICATION: 'Qualificação',
  PROPOSAL: 'Proposta',
  NEGOTIATION: 'Negociação',
  WON: 'Ganha',
  LOST: 'Perdida',
};

/** Espelha as transições permitidas do backend (OpportunityStatus.canMoveTo). O backend continua sendo quem decide. */
const ALLOWED: Record<OpportunityStatus, OpportunityStatus[]> = {
  LEAD: ['QUALIFICATION', 'LOST'],
  QUALIFICATION: ['LEAD', 'PROPOSAL', 'LOST'],
  PROPOSAL: ['QUALIFICATION', 'NEGOTIATION', 'LOST'],
  NEGOTIATION: ['PROPOSAL', 'WON', 'LOST'],
  WON: [],
  LOST: [],
};

export function allowedTargets(status: OpportunityStatus): OpportunityStatus[] {
  return ALLOWED[status];
}

export interface Opportunity {
  id: string;
  customerId: string;
  customerName: string;
  ownerId: string;
  ownerName: string;
  title: string;
  amount: number | null;
  status: OpportunityStatus;
  lostReason: string | null;
  statusChangedAt: string;
  archivedAt: string | null;
  version: number;
}

export interface StalledOpportunity {
  id: string;
  title: string;
  customerId: string;
  customerName: string;
  amount: number | null;
  status: OpportunityStatus;
  statusChangedAt: string;
  daysStalled: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MoveStatusRequest {
  status: OpportunityStatus;
  lostReason: string | null;
  version: number;
}

export interface OpportunityRequest {
  customerId: string;
  title: string;
  amount: number | null;
}

export interface OpportunityUpdateRequest {
  title: string;
  amount: number | null;
  version: number;
}

export type Board = Partial<Record<OpportunityStatus, Opportunity[]>>;

@Injectable({ providedIn: 'root' })
export class OpportunityApi {
  private readonly http = inject(HttpClient);

  board() {
    return this.http.get<Board>(`${API_BASE}/opportunities/board`);
  }

  stalled(days: number, size = 50) {
    return this.http.get<PageResponse<StalledOpportunity>>(`${API_BASE}/opportunities/stalled`, {
      params: { days, size },
    });
  }

  move(id: string, body: MoveStatusRequest) {
    return this.http.patch<Opportunity>(`${API_BASE}/opportunities/${id}/status`, body);
  }

  closed(page: number, size = 20) {
    return this.http.get<PageResponse<Opportunity>>(`${API_BASE}/opportunities/closed`, {
      params: { page, size },
    });
  }

  get(id: string) {
    return this.http.get<Opportunity>(`${API_BASE}/opportunities/${id}`);
  }

  create(body: OpportunityRequest) {
    return this.http.post<Opportunity>(`${API_BASE}/opportunities`, body);
  }

  update(id: string, body: OpportunityUpdateRequest) {
    return this.http.put<Opportunity>(`${API_BASE}/opportunities/${id}`, body);
  }

  archive(id: string) {
    return this.http.post<void>(`${API_BASE}/opportunities/${id}/archive`, {});
  }
}
