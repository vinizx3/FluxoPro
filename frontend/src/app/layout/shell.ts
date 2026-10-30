import { Component, DestroyRef, ElementRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { NotificationState } from '../core/notification';

const REFRESH_MS = 60_000;

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationState);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  constructor() {
    // No celular o menu rola de lado: mantém o item da tela atual visível.
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() =>
        setTimeout(() =>
          this.host
            .querySelector<HTMLElement>('nav a.active')
            ?.scrollIntoView({ inline: 'center', block: 'nearest' }),
        ),
      );

    this.notifications.refresh();
    const timer = setInterval(() => this.notifications.refresh(), REFRESH_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected skipToContent(event: Event): void {
    event.preventDefault();
    this.host.querySelector<HTMLElement>('#conteudo')?.focus();
  }
}
