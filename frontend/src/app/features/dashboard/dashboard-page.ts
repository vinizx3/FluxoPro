import { CurrencyPipe, PercentPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  Dashboard,
  DashboardApi,
  PERIODS,
  barWidth,
  conversionNote,
  formatDays,
} from '../../core/dashboard';
import { errorMessage } from '../../core/error-message';
import { STATUS_LABEL } from '../../core/opportunity';

@Component({
  selector: 'app-dashboard-page',
  imports: [CurrencyPipe, PercentPipe, RouterLink],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss',
})
export class DashboardPage {
  private readonly api = inject(DashboardApi);

  protected readonly periods = PERIODS;
  protected readonly statusLabel = STATUS_LABEL;
  protected readonly days = signal(90);
  protected readonly data = signal<Dashboard | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly maxStage = computed(() =>
    Math.max(0, ...(this.data()?.stages.map((s) => s.count) ?? [])),
  );
  protected readonly maxReason = computed(() =>
    Math.max(0, ...(this.data()?.lostReasons.map((r) => r.count) ?? [])),
  );
  protected readonly empty = computed(() => {
    const d = this.data();
    return !!d && d.openCount === 0 && d.wonCount === 0 && d.lostCount === 0;
  });

  protected readonly width = barWidth;
  protected readonly daysText = formatDays;
  protected readonly note = conversionNote;

  protected periodLabel(days: number): string {
    return PERIODS.find((p) => p.days === days)?.label ?? `${days} dias`;
  }

  constructor() {
    this.load();
  }

  protected setDays(days: number): void {
    if (days === this.days()) {
      return;
    }
    this.days.set(days);
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.get(this.days()).subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, { 403: 'Somente o dono da empresa pode ver o painel.' }));
        this.loading.set(false);
      },
    });
  }
}
