import { Component, computed, input } from '@angular/core';
import { TICKS, activeTicks, daysSince, isCold } from '../core/stall';

/** Termômetro de 10 tracinhos: laranja enquanto a negociação anda, azul-gelo conforme ela esfria. */
@Component({
  selector: 'app-thermometer',
  template: `
    <div class="thermo" role="img" [attr.aria-label]="days() + ' dias nesta etapa'">
      <div class="ticks" [class.cold]="cold()">
        @for (i of indexes; track i) {
          <i [class.on]="i < active()"></i>
        }
      </div>
      <span>{{ days() }} {{ days() === 1 ? 'dia' : 'dias' }}</span>
    </div>
  `,
  styles: `
    .thermo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
    .ticks {
      flex: 1;
    }
    .ticks i:not(.on) {
      background: var(--cold-muted);
    }
    .ticks.cold i.on {
      background: var(--cold);
    }
  `,
})
export class Thermometer {
  readonly since = input.required<string>();
  protected readonly indexes = Array.from({ length: TICKS }, (_, i) => i);
  private readonly now = Date.now();
  protected readonly days = computed(() => daysSince(this.since(), this.now));
  protected readonly active = computed(() => activeTicks(this.days()));
  protected readonly cold = computed(() => isCold(this.days()));
}
