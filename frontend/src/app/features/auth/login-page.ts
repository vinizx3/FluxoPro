import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/error-message';

type Mode = 'login' | 'register';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly mode = signal<Mode>('login');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly loginForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  protected readonly registerForm = this.fb.nonNullable.group({
    companyName: ['', [Validators.required, Validators.maxLength(120)]],
    name: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  protected submit(): void {
    const form = this.mode() === 'login' ? this.loginForm : this.registerForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const request =
      this.mode() === 'login'
        ? this.auth.login(this.loginForm.getRawValue())
        : this.auth.register(this.registerForm.getRawValue());

    request.subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigate(['/funil']);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(
          errorMessage(err, {
            401: 'E-mail ou senha incorretos.',
            409: 'Já existe uma conta com este e-mail.',
          }),
        );
      },
    });
  }
}
