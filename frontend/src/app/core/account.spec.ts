import { FormControl, FormGroup } from '@angular/forms';
import { passwordChangeValidator } from './account';

function group(currentPassword: string, newPassword: string, confirmPassword: string) {
  return new FormGroup(
    {
      currentPassword: new FormControl(currentPassword),
      newPassword: new FormControl(newPassword),
      confirmPassword: new FormControl(confirmPassword),
    },
    { validators: passwordChangeValidator },
  );
}

describe('passwordChangeValidator', () => {
  it('accepts a new password that was confirmed', () => {
    expect(group('old-password', 'new-password', 'new-password').errors).toBeNull();
  });

  it('flags a confirmation that differs from the new password', () => {
    expect(group('old-password', 'new-password', 'other-password').errors).toEqual({
      mismatch: true,
    });
  });

  it('flags a new password equal to the current one', () => {
    expect(group('same-password', 'same-password', 'same-password').errors).toEqual({
      unchanged: true,
    });
  });

  it('stays quiet while fields are still empty', () => {
    expect(group('', '', '').errors).toBeNull();
  });
});
