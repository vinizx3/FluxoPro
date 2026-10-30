import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Customer, CustomerApi } from '../../core/customer';
import { errorMessage } from '../../core/error-message';
import { blankToNull } from '../../core/forms';
import { PageResponse } from '../../core/opportunity';
import {
  PROJECT_STATUSES,
  PROJECT_STATUS_LABEL,
  PROJECT_STATUS_PILL,
  Project,
  ProjectApi,
  ProjectStatus,
  allowedProjectTargets,
  isOverdue,
  todayIso,
} from '../../core/project';

@Component({
  selector: 'app-projects-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './projects-page.html',
  styleUrl: './projects-page.scss',
})
export class ProjectsPage {
  private readonly api = inject(ProjectApi);
  private readonly customerApi = inject(CustomerApi);
  private readonly fb = inject(FormBuilder);

  protected readonly label = PROJECT_STATUS_LABEL;
  protected readonly pill = PROJECT_STATUS_PILL;
  protected readonly statuses = PROJECT_STATUSES;
  protected readonly today = todayIso();

  protected readonly filter = signal<ProjectStatus | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly result = signal<PageResponse<Project> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly movingId = signal<string | null>(null);

  protected readonly newOpen = signal(false);
  protected readonly customers = signal<Customer[]>([]);
  protected readonly customersLoaded = signal(false);
  protected readonly saving = signal(false);
  protected readonly newError = signal<string | null>(null);
  protected readonly newForm = this.fb.nonNullable.group({
    customerId: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.maxLength(2000)],
    expectedDeliveryDate: [''],
  });

  constructor() {
    this.load();
  }

  protected setFilter(status: ProjectStatus | null): void {
    this.filter.set(status);
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
    this.api.list(this.filter(), this.pageIndex()).subscribe({
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

  protected targets(p: Project): ProjectStatus[] {
    return allowedProjectTargets(p.status);
  }

  protected overdue(p: Project): boolean {
    return isOverdue(p.status, p.expectedDeliveryDate, this.today);
  }

  protected onSelect(p: Project, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const target = select.value as ProjectStatus | '';
    select.value = '';
    if (!target) {
      return;
    }
    this.movingId.set(p.id);
    this.notice.set(null);
    this.api.move(p.id, { status: target, version: p.version }).subscribe({
      next: (updated) => {
        this.movingId.set(null);
        this.notice.set(`"${updated.name}" agora está em ${this.label[updated.status]}.`);
        this.load();
      },
      error: (err: unknown) => {
        this.movingId.set(null);
        this.error.set(
          errorMessage(err, {
            400: 'Essa mudança de etapa não é permitida.',
            409: 'Este projeto foi alterado por outra pessoa. A lista foi atualizada.',
          }),
        );
        this.load();
      },
    });
  }

  // ---- novo projeto ----
  protected openNew(): void {
    this.newForm.reset({ customerId: '', name: '', description: '', expectedDeliveryDate: '' });
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

  protected create(): void {
    if (this.newForm.invalid) {
      this.newForm.markAllAsTouched();
      return;
    }
    const v = this.newForm.getRawValue();
    this.saving.set(true);
    this.newError.set(null);
    this.api
      .create({
        customerId: v.customerId,
        name: v.name.trim(),
        description: blankToNull(v.description),
        expectedDeliveryDate: blankToNull(v.expectedDeliveryDate),
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.newOpen.set(false);
          this.notice.set(`Projeto "${created.name}" criado.`);
          this.load();
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.newError.set(errorMessage(err, { 404: 'Cliente não encontrado. Atualize a página.' }));
        },
      });
  }
}
