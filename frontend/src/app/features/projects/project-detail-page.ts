import { DatePipe } from '@angular/common';
import { DialogDirective } from '../../shared/dialog';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/error-message';
import { blankToNull } from '../../core/forms';
import {
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
  selector: 'app-project-detail-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, DialogDirective],
  templateUrl: './project-detail-page.html',
  styleUrl: './project-detail-page.scss',
})
export class ProjectDetailPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(ProjectApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly label = PROJECT_STATUS_LABEL;
  protected readonly pill = PROJECT_STATUS_PILL;
  protected readonly project = signal<Project | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly confirmingArchive = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.maxLength(2000)],
    expectedDeliveryDate: [''],
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.get(this.id).subscribe({
      next: (p) => {
        this.apply(p);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err, { 404: 'Não encontramos esse projeto.' }));
        this.loading.set(false);
      },
    });
  }

  private apply(p: Project): void {
    this.project.set(p);
    this.form.reset({
      name: p.name,
      description: p.description ?? '',
      expectedDeliveryDate: p.expectedDeliveryDate ?? '',
    });
  }

  protected targets(p: Project): ProjectStatus[] {
    return allowedProjectTargets(p.status);
  }

  protected overdue(p: Project): boolean {
    return isOverdue(p.status, p.expectedDeliveryDate, todayIso());
  }

  protected save(): void {
    const p = this.project();
    if (!p) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.notice.set(null);
    this.api
      .update(p.id, {
        name: v.name.trim(),
        description: blankToNull(v.description),
        expectedDeliveryDate: blankToNull(v.expectedDeliveryDate),
        version: p.version,
      })
      .subscribe({
        next: (updated) => {
          this.apply(updated);
          this.saving.set(false);
          this.notice.set('Alterações salvas.');
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(
            errorMessage(err, {
              409: 'Este projeto foi alterado por outra pessoa. Os dados foram recarregados.',
            }),
          );
          this.load();
        },
      });
  }

  protected onSelect(event: Event): void {
    const p = this.project();
    const select = event.target as HTMLSelectElement;
    const target = select.value as ProjectStatus | '';
    select.value = '';
    if (!p || !target) {
      return;
    }
    this.error.set(null);
    this.notice.set(null);
    this.api.move(p.id, { status: target, version: p.version }).subscribe({
      next: (updated) => {
        this.apply(updated);
        this.notice.set(`Projeto movido para ${this.label[updated.status]}.`);
      },
      error: (err: unknown) => {
        this.error.set(
          errorMessage(err, {
            400: 'Essa mudança de etapa não é permitida.',
            409: 'Este projeto foi alterado por outra pessoa. Os dados foram recarregados.',
          }),
        );
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
      next: () => void this.router.navigate(['/projetos']),
      error: (err: unknown) =>
        this.error.set(
          errorMessage(err, {
            422: 'Este projeto ainda não pode ser arquivado. Marque como entregue ou cancelado primeiro.',
          }),
        ),
    });
  }
}
