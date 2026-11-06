import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { OpportunityStatus } from './opportunity';

export interface StageStats {
  status: OpportunityStatus;
  count: number;
  amount: number;
}

export interface LostReasonStats {
  reason: string;
  count: number;
}

export interface SellerStats {
  userId: string;
  name: string;
  openCount: number;
  openAmount: number;
  stalledCount: number;
  wonCount: number;
  wonAmount: number;
  lostCount: number;
}

export interface Dashboard {
  periodDays: number;
  openCount: number;
  openAmount: number;
  stalledCount: number;
  stalledAmount: number;
  wonCount: number;
  wonAmount: number;
  lostCount: number;
  lostAmount: number;
  /** De 0 a 1; nulo quando nada foi fechado no período. */
  conversionRate: number | null;
  averageDaysToWin: number | null;
  stages: StageStats[];
  lostReasons: LostReasonStats[];
  sellers: SellerStats[];
}

export const PERIODS: { days: number; label: string }[] = [
  { days: 30, label: '30 dias' },
  { days: 90, label: '90 dias' },
  { days: 365, label: '12 meses' },
];

@Injectable({ providedIn: 'root' })
export class DashboardApi {
  private readonly http = inject(HttpClient);

  get(days: number) {
    return this.http.get<Dashboard>(`${API_BASE}/dashboard`, {
      params: new HttpParams().set('days', days),
    });
  }
}

/** Largura (0 a 100) de uma barra em relação ao maior valor da lista. */
export function barWidth(value: number, max: number): number {
  if (max <= 0 || value <= 0) {
    return 0;
  }
  return Math.max(4, Math.round((value / max) * 100));
}

/** "1 dia", "2,5 dias": o tempo médio até ganhar, no jeito que se fala. */
export function formatDays(days: number): string {
  const rounded = Math.round(days * 10) / 10;
  const text = String(rounded).replace('.', ',');
  return rounded === 1 ? '1 dia' : `${text} dias`;
}

/** Texto curto que explica a taxa de conversão. */
export function conversionNote(won: number, lost: number): string {
  const closed = won + lost;
  if (closed === 0) {
    return 'Nenhuma negociação fechada no período';
  }
  return `${won} ganha${won === 1 ? '' : 's'} de ${closed} fechada${closed === 1 ? '' : 's'}`;
}
