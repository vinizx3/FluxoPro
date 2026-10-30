import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { authGuard, guestGuard, ownerGuard } from './auth.guards';
import { AuthService } from './auth.service';

function run(guard: typeof authGuard, state: { authenticated: boolean; owner: boolean }) {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      {
        provide: AuthService,
        useValue: { isAuthenticated: () => state.authenticated, isOwner: () => state.owner },
      },
    ],
  });
  return TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
}

function path(result: unknown): string {
  return TestBed.inject(Router).serializeUrl(result as UrlTree);
}

describe('route guards', () => {
  it('authGuard sends visitors to the login page', () => {
    expect(path(run(authGuard, { authenticated: false, owner: false }))).toBe('/entrar');
  });

  it('authGuard lets signed-in users through', () => {
    expect(run(authGuard, { authenticated: true, owner: false })).toBe(true);
  });

  it('guestGuard sends signed-in users to the funnel', () => {
    expect(path(run(guestGuard, { authenticated: true, owner: true }))).toBe('/funil');
  });

  it('ownerGuard blocks sellers', () => {
    expect(path(run(ownerGuard, { authenticated: true, owner: false }))).toBe('/funil');
  });

  it('ownerGuard allows owners', () => {
    expect(run(ownerGuard, { authenticated: true, owner: true })).toBe(true);
  });
});
