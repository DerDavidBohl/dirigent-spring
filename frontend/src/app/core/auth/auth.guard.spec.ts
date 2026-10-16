import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {Router, UrlTree, provideRouter} from '@angular/router';
import {Observable, of} from 'rxjs';
import {AuthenticatedUser} from '../api/models';
import {AuthService} from './auth.service';
import {authGuard, guestGuard} from './auth.guard';

describe('auth guards', () => {
  const user: AuthenticatedUser = {username: 'ada', apiToken: false};
  const initialized = signal(true);
  const current = signal<AuthenticatedUser | null>(user);
  const initialize = vi.fn(() => of<AuthenticatedUser | null>(user));

  function run(guard: typeof authGuard) {
    return TestBed.runInInjectionContext(() => guard({} as never, {} as never));
  }

  beforeEach(() => {
    initialized.set(true);
    current.set(user);
    initialize.mockClear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), {provide: AuthService, useValue: {initialized, user: current, initialize}}]
    });
  });

  it('lets an authenticated operator through', () => {
    expect(run(authGuard)).toBe(true);
  });

  it('redirects an anonymous operator to the login route', () => {
    current.set(null);
    const result = run(authGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login');
  });

  it('initializes the session before deciding', () => {
    initialized.set(false);
    let result: unknown;
    (run(authGuard) as Observable<unknown>).subscribe(value => (result = value));
    expect(initialize).toHaveBeenCalled();
    expect(result).toBe(true);
  });

  it('sends signed-in operators away from the login route', () => {
    const result = run(guestGuard) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/');
  });

  it('lets anonymous visitors reach the login route', () => {
    current.set(null);
    expect(run(guestGuard)).toBe(true);
  });
});
