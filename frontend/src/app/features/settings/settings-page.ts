import { Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { formatDays } from '../../core/dashboard';
import { errorMessage } from '../../core/error-message';
import { CompanySettingsState, MAX_STALL_DAYS, MIN_STALL_DAYS } from '../../core/settings';

@Component({
  selector: 'app-settings-page',
  imports: [ReactiveFormsModule],
  templateUrl: './settings-page.html',
  styleUrl: './settings-page.scss',
})
export class SettingsPage {
  private readonly settings = inject(CompanySettingsState);
  private readonly fb = inject(FormBuilder);

  protected readonly min = MIN_STALL_DAYS;
  protected readonly max = MAX_STALL_DAYS;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    days: [
      this.settings.stalledAfterDays(),
      [
        Validators.required,
        Validators.min(MIN_STALL_DAYS),
        Validators.max(MAX_STALL_DAYS),
        Validators.pattern(/^\d+$/),
      ],
    ],
  });

  constructor() {
    this.settings.refresh();
    effect(() => {
      const current = this.settings.stalledAfterDays();
      if (!this.form.dirty) {
        this.form.controls.days.setValue(current);
      }
    });
  }

  protected submit(): void {
    this.notice.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const days = this.form.getRawValue().days;
    this.saving.set(true);
    this.error.set(null);
    this.settings.update(days).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.form.markAsPristine();
        this.notice.set(
          `Pronto: uma negociação passa a esfriar depois de ${formatDays(saved.stalledAfterDays)} sem atividade.`,
        );
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.error.set(
          errorMessage(err, {
            400: 'Informe um número inteiro entre 1 e 90.',
            403: 'Somente o dono da empresa pode mudar isso.',
          }),
        );
      },
    });
  }
}
