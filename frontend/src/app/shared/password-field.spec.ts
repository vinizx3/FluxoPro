import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { PasswordField } from './password-field';

@Component({
  imports: [ReactiveFormsModule, PasswordField],
  template: `<label for="pw">Senha</label>
    <app-password-field fieldId="pw" [formControl]="control" />`,
})
class Host {
  control = new FormControl('segredo123', { nonNullable: true });
}

describe('PasswordField', () => {
  function setup() {
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return {
      fixture,
      input: () => el.querySelector('input') as HTMLInputElement,
      toggle: () => el.querySelector('button') as HTMLButtonElement,
    };
  }

  it('starts hidden and shows the form value', async () => {
    const t = setup();
    await t.fixture.whenStable();
    t.fixture.detectChanges();
    expect(t.input().type).toBe('password');
    expect(t.input().id).toBe('pw');
    expect(t.input().value).toBe('segredo123');
  });

  it('toggles between hidden and visible', () => {
    const t = setup();
    t.toggle().click();
    t.fixture.detectChanges();
    expect(t.input().type).toBe('text');
    expect(t.toggle().getAttribute('aria-pressed')).toBe('true');
    t.toggle().click();
    t.fixture.detectChanges();
    expect(t.input().type).toBe('password');
  });

  it('writes typed text back to the form control', () => {
    const t = setup();
    t.input().value = 'nova-senha-1';
    t.input().dispatchEvent(new Event('input'));
    expect(t.fixture.componentInstance.control.value).toBe('nova-senha-1');
  });
});
