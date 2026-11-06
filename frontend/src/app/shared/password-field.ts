import { Component, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Campo de senha com botão "Mostrar/Ocultar".
 * Funciona com formControlName/formControl, como um input comum.
 * O `<label for="...">` da tela aponta para `fieldId`, que é o id do input de verdade.
 */
@Component({
  selector: 'app-password-field',
  templateUrl: './password-field.html',
  styleUrl: './password-field.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PasswordField),
      multi: true,
    },
  ],
})
export class PasswordField implements ControlValueAccessor {
  readonly fieldId = input.required<string>();
  readonly autocompleteHint = input<string>('current-password');
  readonly placeholder = input<string>('');
  readonly maxLength = input<number>(72);
  readonly invalid = input<boolean>(false);

  protected readonly visible = signal(false);
  protected readonly value = signal('');
  protected readonly disabled = signal(false);

  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: string | null): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled.set(disabled);
  }

  protected toggle(): void {
    this.visible.update((v) => !v);
  }

  protected onInput(event: Event): void {
    const next = (event.target as HTMLInputElement).value;
    this.value.set(next);
    this.onChange(next);
  }

  protected touched(): void {
    this.onTouched();
  }
}
