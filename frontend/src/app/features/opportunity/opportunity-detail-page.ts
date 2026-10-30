import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../core/error-message';
import { Opportunity, OpportunityApi, STATUS_LABEL } from '../../core/opportunity';
import { Thermometer } from '../../shared/thermometer';

@Component({
  selector: 'app-opportunity-detail-page',
  imports: [CurrencyPipe, ReactiveFormsModule, RouterLink, Thermometer],
  templateUrl: './opportunity-detail-page.html',
  styleUrl: './opportunity-detail-page.scss',
})
export class OpportunityDetailPage {
  private readonly api = inject(OpportunityApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly label = STATUS_LABEL;
  protected readonly opportunity = signal<Opportunity | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly confirmingArchive = signal(false);

  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    amount: this.fb.control<number | null>(null, Validators.min(0)),
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.get(this.id).subscribe({
      next: (o) => {
        this.apply(o);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, { 404: 'Não encontramos essa negociação.' }));
        this.loading.set(false);
      },
    });
  }

  private apply(o: Opportunity): void {
    this.opportunity.set(o);
    this.form.reset({ title: o.title, amount: o.amount });
  }

  protected save(): void {
    const o = this.opportunity();
    if (!o) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api.update(o.id, { title: (value.title ?? '').trim(), amount: value.amount, version: o.version }).subscribe({
      next: (updated) => {
        this.apply(updated);
        this.saving.set(false);
        this.notice.set('Alterações salvas.');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(errorMessage(err, { 409: 'Esta negociação foi alterada por outra pessoa. Os dados foram recarregados.' }));
        this.load();
      },
    });
  }

  protected askArchive(): void {
    this.confirmingArchive.set(true);
  }

  protected cancelArchive(): void {
    this.confirmingArchive.set(false);
  }

  protected archive(): void {
    this.confirmingArchive.set(false);
    this.api.archive(this.id).subscribe({
      next: () => void this.router.navigate(['/funil']),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }
}
