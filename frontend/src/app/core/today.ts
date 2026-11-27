import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { API_BASE } from './api';
import { OpportunityStatus } from './opportunity';

export interface TodayItem {
  id: string;
  title: string;
  customerId: string;
  customerName: string;
  customerPhone: string | null;
  ownerId: string;
  ownerName: string;
  amount: number | null;
  status: OpportunityStatus;
  nextStepAt: string | null;
  nextStepNote: string | null;
  daysOverdue: number;
  daysSinceLastActivity: number;
  version: number;
}

export interface TodayResponse {
  date: string;
  overdue: TodayItem[];
  dueToday: TodayItem[];
  withoutNextStep: TodayItem[];
}

export function daysLabel(days: number): string {
  return days === 0 ? 'hoje' : days === 1 ? 'há 1 dia' : `há ${days} dias`;
}

@Injectable({ providedIn: 'root' })
export class TodayApi {
  private readonly http = inject(HttpClient);

  get() {
    return this.http.get<TodayResponse>(`${API_BASE}/today`);
  }
}