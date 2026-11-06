import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';
import { API_BASE } from './api';

export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** Valida, no grupo do formulário, se a confirmação é igual à nova senha e se ela mudou. */
export function passwordChangeValidator(group: AbstractControl): ValidationErrors | null {
  const current = group.get('currentPassword')?.value as string | undefined;
  const next = group.get('newPassword')?.value as string | undefined;
  const confirm = group.get('confirmPassword')?.value as string | undefined;
  const errors: ValidationErrors = {};
  if (next && confirm && next !== confirm) {
    errors['mismatch'] = true;
  }
  if (current && next && current === next) {
    errors['unchanged'] = true;
  }
  return Object.keys(errors).length > 0 ? errors : null;
}

@Injectable({ providedIn: 'root' })
export class AccountApi {
  private readonly http = inject(HttpClient);

  changePassword(body: ChangePasswordRequest) {
    return this.http.put<void>(`${API_BASE}/me/password`, body);
  }
}
