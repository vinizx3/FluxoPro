import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  ACTION_LABEL,
  AuditApi,
  AuditChange,
  AuditEntityType,
  AuditEntry,
  ENTITY_LABEL,
  fieldLabel,
  formatValue,
} from '../../core/audit';
import { errorMessage } from '../../core/error-message';
import { PageResponse } from '../../core/opportunity';

const TYPES: AuditEntityType[] = ['CUSTOMER', 'OPPORTUNITY', 'PROJECT'];

@Component({
  selector: 'app-audit-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './audit-page.html',
  styleUrl: './audit-page.scss',
})
export class AuditPage {
  private readonly api = inject(AuditApi);
  private readonly route = inject(ActivatedRoute);

  protected readonly types = TYPES;
  protected readonly entityLabel = ENTITY_LABEL;
  protected readonly actionLabel = ACTION_LABEL;
  protected readonly article: Record<AuditEntityType, string> = {
    CUSTOMER: 'um',
    OPPORTUNITY: 'uma',
    PROJECT: 'um',
  };
  protected readonly field = fieldLabel;
  protected readonly value = formatValue;

  private readonly query = this.route.snapshot.queryParamMap;
  protected readonly type = signal<AuditEntityType | null>(this.initialType());
  protected readonly entityId = signal<string | null>(this.query.get('id'));
  protected readonly pageIndex = signal(0);
  protected readonly result = signal<PageResponse<AuditEntry> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  private initialType(): AuditEntityType | null {
    const raw = this.route.snapshot.queryParamMap.get('tipo');
    return TYPES.find((t) => t === raw) ?? null;
  }

  protected setType(type: AuditEntityType | null): void {
    this.type.set(type);
    this.entityId.set(null);
    this.pageIndex.set(0);
    this.load();
  }

  protected clearEntity(): void {
    this.entityId.set(null);
    this.pageIndex.set(0);
    this.load();
  }

  protected goTo(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.type(), this.entityId(), this.pageIndex()).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(
          errorMessage(err, { 403: 'Somente o dono da empresa pode ver o histórico.' }),
        );
        this.loading.set(false);
      },
    });
  }

  protected link(entry: AuditEntry): string[] | null {
    switch (entry.entityType) {
      case 'OPPORTUNITY':
        return ['/negociacoes', entry.entityId];
      case 'PROJECT':
        return ['/projetos', entry.entityId];
      default:
        return null;
    }
  }

  protected showChanges(entry: AuditEntry): AuditChange[] {
    return entry.changes;
  }
}
