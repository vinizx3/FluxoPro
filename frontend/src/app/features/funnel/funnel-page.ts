import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { errorMessage } from '../../core/error-message';
import {
  Board,
  OPEN_STATUSES,
  Opportunity,
  OpportunityApi,
  OpportunityStatus,
  STATUS_LABEL,
  StalledOpportunity,
  allowedTargets,
} from '../../core/opportunity';
import { STALL_DAYS, TICKS, activeTicks, daysSince, isCold } from '../../core/stall';

interface PendingLoss {
  opportunity: Opportunity;
  reason: string;
}

@Component({
  selector: 'app-funnel-page',
  imports: [CurrencyPipe, FormsModule],
  templateUrl: './funnel-page.html',
  styleUrl: './funnel-page.scss',
})
export class FunnelPage {
  private readonly api = inject(OpportunityApi);

  protected readonly label = STATUS_LABEL;
  protected readonly stallDays = STALL_DAYS;
  protected readonly tickIndexes = Array.from({ length: TICKS }, (_, i) => i);

  protected readonly board = signal<Board>({});
  protected readonly stalled = signal<StalledOpportunity[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly movingId = signal<string | null>(null);
  protected readonly pendingLoss = signal<PendingLoss | null>(null);
  private readonly now = signal(Date.now());

  protected readonly columns = computed(() =>
    OPEN_STATUSES.map((status) => {
      const items = this.board()[status] ?? [];
      const total = items.reduce((sum, o) => sum + Number(o.amount ?? 0), 0);
      return { status, items, total };
    }),
  );

  constructor() {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.now.set(Date.now());
    this.api.board().subscribe({
      next: (board) => {
        this.board.set(board);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
    this.loadStalled();
  }

  private loadStalled(): void {
    this.api.stalled(STALL_DAYS).subscribe({
      next: (page) => this.stalled.set(page.content),
      error: () => this.stalled.set([]),
    });
  }

  protected days(o: Opportunity): number {
    return daysSince(o.statusChangedAt, this.now());
  }

  protected cold(o: Opportunity): boolean {
    return isCold(this.days(o));
  }

  protected tickOn(o: Opportunity, index: number): boolean {
    return index < activeTicks(this.days(o));
  }

  protected targets(o: Opportunity): OpportunityStatus[] {
    return allowedTargets(o.status);
  }

  protected showInFunnel(id: string): void {
    const el = document.getElementById(`opp-${id}`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
    el?.focus();
  }

  protected onSelect(o: Opportunity, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const target = select.value as OpportunityStatus | '';
    select.value = '';
    if (!target) {
      return;
    }
    if (target === 'LOST') {
      this.pendingLoss.set({ opportunity: o, reason: '' });
      return;
    }
    this.move(o, target, null);
  }

  protected confirmLoss(): void {
    const pending = this.pendingLoss();
    if (!pending || !pending.reason.trim()) {
      return;
    }
    this.pendingLoss.set(null);
    this.move(pending.opportunity, 'LOST', pending.reason.trim());
  }

  protected cancelLoss(): void {
    this.pendingLoss.set(null);
  }

  protected setReason(value: string): void {
    const pending = this.pendingLoss();
    if (pending) {
      this.pendingLoss.set({ ...pending, reason: value });
    }
  }

  private move(o: Opportunity, status: OpportunityStatus, lostReason: string | null): void {
    this.movingId.set(o.id);
    this.notice.set(null);
    this.api.move(o.id, { status, lostReason, version: o.version }).subscribe({
      next: (updated) => {
        const next: Board = { ...this.board() };
        next[o.status] = (next[o.status] ?? []).filter((x) => x.id !== o.id);
        if (OPEN_STATUSES.includes(updated.status)) {
          next[updated.status] = [updated, ...(next[updated.status] ?? [])];
        }
        this.board.set(next);
        this.movingId.set(null);
        this.notice.set(
          updated.status === 'WON'
            ? `"${o.title}" foi marcada como ganha.`
            : updated.status === 'LOST'
              ? `"${o.title}" foi marcada como perdida.`
              : `"${o.title}" foi para ${STATUS_LABEL[updated.status]}.`,
        );
        this.loadStalled();
      },
      error: (err: unknown) => {
        this.movingId.set(null);
        this.error.set(
          errorMessage(err, {
            400: 'Essa mudança de etapa não é permitida.',
            409: 'Esta negociação foi alterada por outra pessoa. A lista foi atualizada.',
          }),
        );
        this.reload();
      },
    });
  }
}
