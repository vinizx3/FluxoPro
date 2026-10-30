import { CurrencyPipe } from '@angular/common';
import { DialogDirective } from '../../shared/dialog';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Customer, CustomerApi } from '../../core/customer';
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
import { STALL_DAYS, daysSince, isCold } from '../../core/stall';
import { Thermometer } from '../../shared/thermometer';

interface PendingLoss {
  opportunity: Opportunity;
  reason: string;
}

@Component({
  selector: 'app-funnel-page',
  imports: [
    CurrencyPipe,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    Thermometer,
    DialogDirective,
  ],
  templateUrl: './funnel-page.html',
  styleUrl: './funnel-page.scss',
})
export class FunnelPage {
  private readonly api = inject(OpportunityApi);
  private readonly customerApi = inject(CustomerApi);
  private readonly fb = inject(FormBuilder);

  protected readonly label = STATUS_LABEL;
  protected readonly stallDays = STALL_DAYS;

  protected readonly board = signal<Board>({});
  protected readonly stalled = signal<StalledOpportunity[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly movingId = signal<string | null>(null);
  protected readonly pendingLoss = signal<PendingLoss | null>(null);
  private readonly now = signal(Date.now());

  protected readonly closed = signal<Opportunity[]>([]);
  protected readonly closedTotal = signal(0);
  protected readonly closedPage = signal(0);
  protected readonly closedHasMore = signal(false);
  protected readonly closedError = signal(false);

  protected readonly newOpen = signal(false);
  protected readonly customers = signal<Customer[]>([]);
  protected readonly customersLoaded = signal(false);
  protected readonly saving = signal(false);
  protected readonly newError = signal<string | null>(null);
  protected readonly newForm = this.fb.group({
    customerId: ['', Validators.required],
    title: ['', [Validators.required, Validators.maxLength(200)]],
    amount: this.fb.control<number | null>(null, Validators.min(0)),
  });

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
    this.loadClosed(true);
  }

  /** Ganhas e perdidas, mais recentes primeiro. Se falhar, o funil continua funcionando. */
  protected loadClosed(reset: boolean): void {
    const page = reset ? 0 : this.closedPage() + 1;
    this.api.closed(page).subscribe({
      next: (result) => {
        this.closed.set(reset ? result.content : [...this.closed(), ...result.content]);
        this.closedTotal.set(result.totalElements);
        this.closedPage.set(result.page);
        this.closedHasMore.set(result.page + 1 < result.totalPages);
        this.closedError.set(false);
      },
      error: () => this.closedError.set(true),
    });
  }

  private loadStalled(): void {
    this.api.stalled(STALL_DAYS).subscribe({
      next: (page) => this.stalled.set(page.content),
      error: () => this.stalled.set([]),
    });
  }

  protected cold(o: Opportunity): boolean {
    return isCold(daysSince(o.statusChangedAt, this.now()));
  }

  protected targets(o: Opportunity): OpportunityStatus[] {
    return allowedTargets(o.status);
  }

  protected showInFunnel(id: string): void {
    const el = document.getElementById(`opp-${id}`);
    el?.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
    el?.focus();
  }

  // ---- nova negociação ----
  protected openNew(): void {
    this.newForm.reset({ customerId: '', title: '', amount: null });
    this.newError.set(null);
    this.newOpen.set(true);
    if (!this.customersLoaded()) {
      this.customerApi.list('', 0, 100).subscribe({
        next: (page) => {
          this.customers.set(page.content);
          this.customersLoaded.set(true);
        },
        error: (err: unknown) => this.newError.set(errorMessage(err)),
      });
    }
  }

  protected closeNew(): void {
    this.newOpen.set(false);
  }

  protected createOpportunity(): void {
    if (this.newForm.invalid) {
      this.newForm.markAllAsTouched();
      return;
    }
    const value = this.newForm.getRawValue();
    this.saving.set(true);
    this.newError.set(null);
    this.api
      .create({
        customerId: value.customerId ?? '',
        title: (value.title ?? '').trim(),
        amount: value.amount,
      })
      .subscribe({
        next: (created) => {
          const next: Board = { ...this.board() };
          if (OPEN_STATUSES.includes(created.status)) {
            next[created.status] = [created, ...(next[created.status] ?? [])];
          }
          this.board.set(next);
          this.saving.set(false);
          this.newOpen.set(false);
          this.notice.set(`"${created.title}" foi criada em ${STATUS_LABEL[created.status]}.`);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.newError.set(
            errorMessage(err, { 404: 'Cliente não encontrado. Atualize a página.' }),
          );
        },
      });
  }

  // ---- mover ----
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
        this.loadClosed(true);
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
