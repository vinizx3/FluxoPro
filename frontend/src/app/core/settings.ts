import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { API_BASE } from './api';
import { STALL_DAYS } from './stall';

export const MIN_STALL_DAYS = 1;
export const MAX_STALL_DAYS = 90;

export interface CompanySettings {
  stalledAfterDays: number;
}

@Injectable({ providedIn: 'root' })
export class CompanySettingsState {
  private readonly http = inject(HttpClient);
  private readonly days = signal(STALL_DAYS);

  readonly stalledAfterDays = this.days.asReadonly();

  refresh(): void {
    this.http.get<CompanySettings>(`${API_BASE}/company/settings`).subscribe({
      next: (settings) => this.days.set(settings.stalledAfterDays),
      error: () => {
      },
    });
  }

  update(stalledAfterDays: number) {
    return this.http
      .put<CompanySettings>(`${API_BASE}/company/settings`, { stalledAfterDays })
      .pipe(tap((settings) => this.days.set(settings.stalledAfterDays)));
  }
}
