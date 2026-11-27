import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/error-message';
import { STATUS_LABEL } from '../../core/opportunity';
import { TodayApi, TodayItem, TodayResponse, daysLabel } from '../../core/today';
import { followUpUrl } from '../../core/whatsapp';

interface Section {
  key: 'overdue' | 'today' | 'missing';
  title: string;
  hint: string;
  items: TodayItem[];
}

@Component({
  selector: 'app-today-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  templateUrl: './today-page.html',
  styleUrl: './today-page.scss',
})
export class TodayPage {
  private readonly api = inject(TodayApi);
  protected readonly auth = inject(AuthService);

  protected readonly label = STATUS_LABEL;
  protected readonly data = signal<TodayResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly sections = computed<Section[]>(() => {
    const d = this.data();
    if (!d) {
      return [];
    }
    return [
      {
        key: 'overdue',
        title: 'Atrasadas',
        hint: 'Combinados que passaram do dia. Resolva primeiro.',
        items: d.overdue,
      },
      {
        key: 'today',
        title: 'Para hoje',
        hint: 'O que você combinou de fazer hoje.',
        items: d.dueToday,
      },
      {
        key: 'missing',
        title: 'Sem próximo passo',
        hint: 'Negociações abertas sem data combinada. É assim que vendas se perdem.',
        items: d.withoutNextStep,
      },
    ];
  });

  protected readonly hasDueWork = computed(() => {
    const d = this.data();
    return !!d && d.overdue.length + d.dueToday.length > 0;
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.get().subscribe({
      next: (d) => {
        this.data.set(d);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  protected wa(item: TodayItem): string | null {
    return followUpUrl(item.customerPhone, item.title);
  }

  protected info(section: Section['key'], item: TodayItem): string {
    if (section === 'overdue') {
      return `Atrasada ${daysLabel(item.daysOverdue)}`;
    }
    if (section === 'missing') {
      return `Último movimento ${daysLabel(item.daysSinceLastActivity)}`;
    }
    return 'Para hoje';
  }
}