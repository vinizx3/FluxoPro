import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { DialogDirective } from './dialog';

@Component({
  imports: [DialogDirective],
  template: `
    <button id="opener" type="button">Abrir</button>
    <div role="dialog" appDialog (dialogClose)="closed = closed + 1">
      <input id="first" />
      <button id="last" type="button">Ok</button>
    </div>
  `,
})
class HostComponent {
  closed = 0;
}

describe('DialogDirective', () => {
  function setup() {
    const fixture = TestBed.createComponent(HostComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    return fixture;
  }

  it('moves focus to the first field when it opens', () => {
    const fixture = setup();
    expect(document.activeElement?.id).toBe('first');
    fixture.nativeElement.remove();
  });

  it('closes on Escape', () => {
    const fixture = setup();
    const dialog: HTMLElement = fixture.nativeElement.querySelector('[role="dialog"]');
    dialog.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    expect(fixture.componentInstance.closed).toBe(1);
    fixture.nativeElement.remove();
  });

  it('keeps Tab inside the dialog', () => {
    const fixture = setup();
    const dialog: HTMLElement = fixture.nativeElement.querySelector('[role="dialog"]');
    (fixture.nativeElement.querySelector('#last') as HTMLElement).focus();
    const tab = new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true });
    dialog.dispatchEvent(tab);
    expect(tab.defaultPrevented).toBe(true);
    expect(document.activeElement?.id).toBe('first');
    fixture.nativeElement.remove();
  });
});
