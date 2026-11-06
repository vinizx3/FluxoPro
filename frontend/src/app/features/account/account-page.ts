import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  AccountApi,
  PASSWORD_MAX,
  PASSWORD_MIN,
  passwordChangeValidator,
} from '../../core/account';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/error-message';
import { PasswordField } from '../../shared/password-field';

@Component({
  selector: 'app-account-page',
  imports: [PasswordField, ReactiveFormsModule],
  templateUrl: './account-page.html',
  styleUrl: './account-page.scss',
})
export class AccountPage {
  private readonly api = inject(AccountApi);
  private readonly fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);

  protected readonly passwordMin = PASSWORD_MIN;
  protected readonly passwordMax = PASSWORD_MAX;
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required, Validators.maxLength(PASSWORD_MAX)]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(PASSWORD_MIN),
          Validators.maxLength(PASSWORD_MAX),
        ],
      ],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordChangeValidator },
  );

  protected submit(): void {
    this.notice.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    this.api
      .changePassword({ currentPassword: v.currentPassword, newPassword: v.newPassword })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.form.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
          this.notice.set('Senha alterada. Use a nova senha na próxima vez que entrar.');
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(
            errorMessage(err, {
              422: 'A senha atual está incorreta, ou a nova senha é igual à atual.',
            }),
          );
        },
      });
  }
}
