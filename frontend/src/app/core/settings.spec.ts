import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_BASE } from './api';
import { CompanySettingsState } from './settings';
import { STALL_DAYS } from './stall';

describe('CompanySettingsState', () => {
  const url = `${API_BASE}/company/settings`;
  let state: CompanySettingsState;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    state = TestBed.inject(CompanySettingsState);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts with the default threshold', () => {
    expect(state.stalledAfterDays()).toBe(STALL_DAYS);
  });

  it('uses the company value once the API answers', () => {
    state.refresh();
    http.expectOne(url).flush({ stalledAfterDays: 15 });

    expect(state.stalledAfterDays()).toBe(15);
  });

  it('keeps the current value when the request fails', () => {
    state.refresh();
    http.expectOne(url).flush('boom', { status: 500, statusText: 'Server Error' });

    expect(state.stalledAfterDays()).toBe(STALL_DAYS);
  });

  it('saves the new value and applies it right away', () => {
    let saved: number | null = null;
    state.update(3).subscribe((s) => (saved = s.stalledAfterDays));

    const req = http.expectOne(url);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ stalledAfterDays: 3 });
    req.flush({ stalledAfterDays: 3 });

    expect(saved).toBe(3);
    expect(state.stalledAfterDays()).toBe(3);
  });

  it('does not change the value when saving fails', () => {
    state.update(3).subscribe({ error: () => undefined });
    http.expectOne(url).flush('nope', { status: 403, statusText: 'Forbidden' });

    expect(state.stalledAfterDays()).toBe(STALL_DAYS);
  });
});
