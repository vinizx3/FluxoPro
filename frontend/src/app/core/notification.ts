import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { map } from 'rxjs';
import { API_BASE } from './api';
import { PageResponse } from './opportunity';

export interface AppNotification {
  id: string;
  type: string;
  opportunityId: string;
  opportunityTitle: string;
  customerName: string;
  stalledSince: string;
  daysStalled: number;
  createdAt: string;
  readAt: string | null;
}

/**
 * O backend devolve o contador dentro de um objeto de um campo só.
 * Em vez de depender do nome exato do campo, pegamos o primeiro valor numérico.
 */
export function extractCount(response: unknown): number {
  if (typeof response === 'number') {
    return response;
  }
  if (response && typeof response === 'object') {
    const found = Object.values(response as Record<string, unknown>).find((v) => typeof v === 'number');
    if (typeof found === 'number') {
      return found;
    }
  }
  return 0;
}

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly http = inject(HttpClient);

  list(unreadOnly: boolean, page: number, size: number) {
    return this.http.get<PageResponse<AppNotification>>(`${API_BASE}/notifications`, {
      params: { unreadOnly, page, size },
    });
  }

  unreadCount() {
    return this.http.get<unknown>(`${API_BASE}/notifications/unread-count`).pipe(map(extractCount));
  }

  markRead(id: string) {
    return this.http.post<AppNotification>(`${API_BASE}/notifications/${id}/read`, {});
  }

  markAllRead() {
    return this.http.post<unknown>(`${API_BASE}/notifications/read-all`, {});
  }
}

/** Contador de alertas não lidos, compartilhado entre o menu e a tela de alertas. */
@Injectable({ providedIn: 'root' })
export class NotificationState {
  private readonly api = inject(NotificationApi);
  readonly unread = signal(0);

  refresh(): void {
    this.api.unreadCount().subscribe({
      next: (count) => this.unread.set(count),
      error: () => undefined, // o contador é um enfeite: se falhar, a tela continua funcionando
    });
  }
}
