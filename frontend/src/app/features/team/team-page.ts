import { DatePipe } from '@angular/common';
import { DialogDirective } from '../../shared/dialog';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { errorMessage } from '../../core/error-message';
import { TeamApi, TeamMember } from '../../core/team';

@Component({
  selector: 'app-team-page',
  imports: [DatePipe, ReactiveFormsModule, DialogDirective],
  templateUrl: './team-page.html',
  styleUrl: './team-page.scss',
})
export class TeamPage {
  private readonly api = inject(TeamApi);
  private readonly fb = inject(FormBuilder);

  protected readonly members = signal<TeamMember[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly addOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly deactivating = signal<TeamMember | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().subscribe({
      next: (members) => {
        this.members.set(members);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(
          errorMessage(err, { 403: 'Somente o dono da empresa pode gerenciar a equipe.' }),
        );
        this.loading.set(false);
      },
    });
  }

  protected openAdd(): void {
    this.form.reset({ name: '', email: '', password: '' });
    this.formError.set(null);
    this.addOpen.set(true);
  }

  protected closeAdd(): void {
    this.addOpen.set(false);
  }

  protected create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);
    this.api
      .createSeller({ name: v.name.trim(), email: v.email.trim(), password: v.password })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.addOpen.set(false);
          this.notice.set(
            `Vendedor "${created.name}" adicionado. Passe o e-mail e a senha para ele entrar.`,
          );
          this.load();
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.formError.set(errorMessage(err, { 409: 'Já existe uma conta com este e-mail.' }));
        },
      });
  }

  protected askDeactivate(member: TeamMember): void {
    this.deactivating.set(member);
  }

  protected cancelDeactivate(): void {
    this.deactivating.set(null);
  }

  protected confirmDeactivate(): void {
    const member = this.deactivating();
    this.deactivating.set(null);
    if (member) {
      this.setActive(member, false);
    }
  }

  protected setActive(member: TeamMember, active: boolean): void {
    this.notice.set(null);
    this.api.setActive(member.id, active).subscribe({
      next: () => {
        this.notice.set(
          active
            ? `"${member.name}" foi reativado.`
            : `"${member.name}" foi desativado e não consegue mais entrar.`,
        );
        this.load();
      },
      error: (err: unknown) =>
        this.error.set(errorMessage(err, { 422: 'Esta conta não pode ser desativada.' })),
    });
  }
}
