import { CurrencyPipe, DatePipe } from '@angular/common';
import { DialogDirective } from '../../shared/dialog';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/error-message';
import {
  CONTACT_KINDS,
  CONTACT_LABEL,
  Contact,
  Opportunity,
  OpportunityApi,
  STATUS_LABEL,
  activityDate,
  allowedTargets,
  nextStepOverdue,
} from '../../core/opportunity';
import { blankToNull } from '../../core/forms';
import { TeamApi, TeamMember, transferTargets } from '../../core/team';
import { todayIso } from '../../core/project';
import { Thermometer } from '../../shared/thermometer';
import { followUpUrl } from '../../core/whatsapp';

@Component({
  selector: 'app-opportunity-detail-page',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, RouterLink, Thermometer, DialogDirective],
  templateUrl: './opportunity-detail-page.html',
  styleUrl: './opportunity-detail-page.scss',
})
export class OpportunityDetailPage {
  protected readonly auth = inject(AuthService);
  private readonly api = inject(OpportunityApi);
  private readonly teamApi = inject(TeamApi);
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

  protected readonly contactKinds = CONTACT_KINDS;
  protected readonly contactLabel = CONTACT_LABEL;
  protected readonly contacts = signal<Contact[]>([]);
  protected readonly contactsLoading = signal(true);
  protected readonly contactsError = signal<string | null>(null);
  protected readonly contactOpen = signal(false);
  protected readonly contactSaving = signal(false);
  protected readonly contactFormError = signal<string | null>(null);
  protected readonly today = todayIso();

  protected readonly transferOpen = signal(false);
  protected readonly transferSaving = signal(false);
  protected readonly transferLoading = signal(false);
  protected readonly transferError = signal<string | null>(null);
  protected readonly members = signal<TeamMember[]>([]);
  protected readonly transferForm = this.fb.nonNullable.group({
    ownerId: ['', Validators.required],
  });

  protected readonly contactForm = this.fb.nonNullable.group({
    kind: ['CALL' as (typeof CONTACT_KINDS)[number], Validators.required],
    note: ['', [Validators.required, Validators.maxLength(1000)]],
    nextStepAt: [''],
    nextStepNote: ['', Validators.maxLength(255)],
  });

  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    amount: this.fb.control<number | null>(null, Validators.min(0)),
  });

  constructor() {
    this.load();
    this.loadContacts();
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

  private loadContacts(): void {
    this.contactsLoading.set(true);
    this.contactsError.set(null);
    this.api.contacts(this.id).subscribe({
      next: (list) => {
        this.contacts.set(list);
        this.contactsLoading.set(false);
      },
      error: () => {
        this.contactsError.set('Não foi possível carregar os contatos.');
        this.contactsLoading.set(false);
      },
    });
  }

  protected activity(o: Opportunity): string {
    return activityDate(o);
  }

  protected isOpen(o: Opportunity): boolean {
    return allowedTargets(o.status).length > 0;
  }

  protected overdue(o: Opportunity): boolean {
    return nextStepOverdue(o.nextStepAt, this.today);
  }

  protected canTransfer(o: Opportunity): boolean {
    return this.auth.isOwner() && this.isOpen(o);
  }

  protected targets(o: Opportunity): TeamMember[] {
    return transferTargets(this.members(), o.ownerId);
  }

  protected openTransfer(): void {
    this.transferForm.reset({ ownerId: '' });
    this.transferError.set(null);
    this.transferOpen.set(true);
    this.transferLoading.set(true);
    this.teamApi.list().subscribe({
      next: (list) => {
        this.members.set(list);
        this.transferLoading.set(false);
      },
      error: (err: unknown) => {
        this.transferLoading.set(false);
        this.transferError.set(
          errorMessage(err, { 403: 'Somente o dono da empresa pode transferir negociações.' }),
        );
      },
    });
  }

  protected closeTransfer(): void {
    this.transferOpen.set(false);
  }

  protected confirmTransfer(): void {
    const o = this.opportunity();
    if (!o) {
      return;
    }
    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }
    this.transferSaving.set(true);
    this.transferError.set(null);
    this.api
      .reassign(o.id, { ownerId: this.transferForm.getRawValue().ownerId, version: o.version })
      .subscribe({
        next: (updated) => {
          // Só atualiza o resumo: o formulário de edição ao lado mantém o que a pessoa digitou.
          this.opportunity.set(updated);
          this.transferSaving.set(false);
          this.transferOpen.set(false);
          this.notice.set(`Negociação transferida para ${updated.ownerName}.`);
        },
        error: (err: unknown) => {
          this.transferSaving.set(false);
          this.transferError.set(
            errorMessage(err, {
              403: 'Somente o dono da empresa pode transferir negociações.',
              422: 'Não foi possível transferir: a negociação está encerrada ou a pessoa está desativada.',
              409: 'Esta negociação foi alterada por outra pessoa. Feche e tente de novo.',
            }),
          );
        },
      });
  }

  protected openContact(): void {
    this.contactForm.reset({ kind: 'CALL', note: '', nextStepAt: '', nextStepNote: '' });
    this.contactFormError.set(null);
    this.contactOpen.set(true);
  }

  protected closeContact(): void {
    this.contactOpen.set(false);
  }

  protected saveContact(): void {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }
    const v = this.contactForm.getRawValue();
    const nextStepAt = blankToNull(v.nextStepAt);
    if (nextStepAt && nextStepAt < this.today) {
      this.contactFormError.set('A data do próximo passo não pode ser no passado.');
      return;
    }
    this.contactSaving.set(true);
    this.contactFormError.set(null);
    this.api
      .registerContact(this.id, {
        kind: v.kind,
        note: v.note.trim(),
        nextStepAt,
        nextStepNote: nextStepAt ? blankToNull(v.nextStepNote) : null,
      })
      .subscribe({
        next: (updated) => {
          // Só atualiza o resumo: o formulário de edição ao lado mantém o que a pessoa digitou.
          this.opportunity.set(updated);
          this.contactSaving.set(false);
          this.contactOpen.set(false);
          this.notice.set('Contato registrado. O termômetro recomeçou a contar a partir de agora.');
          this.loadContacts();
        },
        error: (err: unknown) => {
          this.contactSaving.set(false);
          this.contactFormError.set(
            errorMessage(err, {
              422: 'Não é possível registrar contato nesta negociação (ela está encerrada).',
              404: 'Não encontramos essa negociação.',
            }),
          );
        },
      });
  }

  protected wa(o: Opportunity): string | null {
    return followUpUrl(o.customerPhone, o.title);
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
    this.api
      .update(o.id, { title: (value.title ?? '').trim(), amount: value.amount, version: o.version })
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
              409: 'Esta negociação foi alterada por outra pessoa. Os dados foram recarregados.',
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
      next: () => void this.router.navigate(['/funil']),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }
}
