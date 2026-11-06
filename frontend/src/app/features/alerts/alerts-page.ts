import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { errorMessage } from '../../core/error-message';
import { AppNotification, NotificationApi, NotificationState } from '../../core/notification';
import { PageResponse } from '../../core/opportunity';

type Filter = 'all' | 'unread';

@Component({
  selector: 'app-alerts-page',
  imports: [DatePipe, RouterLink],
  templateUrl: './alerts-page.html',
  styleUrl: './alerts-page.scss',
})
export class AlertsPage {
  private readonly api = inject(NotificationApi);
  protected readonly state = inject(NotificationState);

  protected readonly filter = signal<Filter>('unread');
  protected readonly pageIndex = signal(0);
  protected readonly result = signal<PageResponse<AppNotification> | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected setFilter(filter: Filter): void {
    this.filter.set(filter);
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
    this.api.list(this.filter() === 'unread', this.pageIndex(), 20).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
    this.state.refresh();
  }

  protected markRead(n: AppNotification): void {
    if (n.readAt) {
      return;
    }
    this.api.markRead(n.id).subscribe({
      next: () => this.load(),
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }

  protected markAll(): void {
    this.notice.set(null);
    this.api.markAllRead().subscribe({
      next: () => {
        this.notice.set('Todos os alertas foram marcados como lidos.');
        this.load();
      },
      error: (err: unknown) => this.error.set(errorMessage(err)),
    });
  }
}
