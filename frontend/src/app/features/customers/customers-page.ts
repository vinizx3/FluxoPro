import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime } from 'rxjs';
import { Customer, CustomerApi } from '../../core/customer';
import { errorMessage } from '../../core/error-message';
import { blankToNull } from '../../core/forms';
import { PageResponse } from '../../core/opportunity';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-customers-page',
  imports: [ReactiveFormsModule],
  templateUrl: './customers-page.html',
  styleUrl: './customers-page.scss',
})
export class CustomersPage {
  private readonly api = inject(CustomerApi);
  private readonly fb = inject(FormBuilder);
  private readonly search$ = new Subject<string>();

  protected readonly result = signal<PageResponse<Customer> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly search = signal('');

  /** null = fechado; 'new' = cadastrando; Customer = editando. */
  protected readonly editing = signal<Customer | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly archiving = signal<Customer | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(30)],
  });

  constructor() {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed(inject(DestroyRef))).subscribe((term) => {
      this.search.set(term);
      this.pageIndex.set(0);
      this.load();
    });
    this.load();
  }

  protected onSearch(event: Event): void {
    this.search$.next((event.target as HTMLInputElement).value);
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.search(), this.pageIndex(), PAGE_SIZE).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected goTo(index: number): void {
    this.pageIndex.set(index);
    this.load();
  }

  protected openNew(): void {
    this.form.reset({ name: '', email: '', phone: '' });
    this.formError.set(null);
    this.editing.set('new');
  }

  protected openEdit(customer: Customer): void {
    this.form.reset({ name: customer.name, email: customer.email ?? '', phone: customer.phone ?? '' });
    this.formError.set(null);
    this.editing.set(customer);
  }

  protected closeForm(): void {
    this.editing.set(null);
  }

  protected save(): void {
    const target = this.editing();
    if (!target) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = { name: value.name.trim(), email: blankToNull(value.email), phone: blankToNull(value.phone) };
    this.saving.set(true);
    this.formError.set(null);

    const request = target === 'new' ? this.api.create(body) : this.api.update(target.id, { ...body, version: target.version });
    request.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.editing.set(null);
        this.notice.set(target === 'new' ? `Cliente "${saved.name}" cadastrado.` : `Cliente "${saved.name}" atualizado.`);
        this.load();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.formError.set(
          errorMessage(err, { 409: 'Este cliente foi alterado por outra pessoa. Feche, recarregue a lista e tente de novo.' }),
        );
      },
    });
  }

  protected askArchive(customer: Customer): void {
    this.archiving.set(customer);
  }

  protected cancelArchive(): void {
    this.archiving.set(null);
  }

  protected confirmArchive(): void {
    const customer = this.archiving();
    if (!customer) {
      return;
    }
    this.archiving.set(null);
    this.api.archive(customer.id).subscribe({
      next: () => {
        this.notice.set(`Cliente "${customer.name}" arquivado.`);
        this.load();
      },
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }
}
