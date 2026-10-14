import {TestBed} from '@angular/core/testing';
import {Router, provideRouter} from '@angular/router';
import {signal} from '@angular/core';
import {Observable, of, throwError} from 'rxjs';
import {AuthOptions, AuthenticatedUser} from '../../core/api/models';
import {AuthService} from '../../core/auth/auth.service';
import {Login} from './login';

describe('Login', () => {
  const login = vi.fn<(u: string, p: string) => Observable<void>>();
  const initialize = vi.fn<() => Observable<AuthenticatedUser | null>>();

  function render(options: AuthOptions = {oidcEnabled: false, oidcLoginPath: null}) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: {options: signal(options), initialized: signal(true), login, initialize}}
      ]
    });
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  async function submit(fixture: ReturnType<typeof render>['fixture'], el: HTMLElement) {
    await fixture.whenStable();
    for (const [name, value] of [['username', 'ada'], ['password', 'pw']]) {
      const input = el.querySelector(`[name=${name}]`) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  beforeEach(() => {
    login.mockReset();
    initialize.mockReset();
  });

  it('signs in and opens the dashboard', async () => {
    login.mockReturnValue(of(undefined));
    initialize.mockReturnValue(of({username: 'ada', apiToken: false}));
    const {fixture, el} = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    await submit(fixture, el);
    expect(login).toHaveBeenCalledWith('ada', 'pw');
    expect(navigate).toHaveBeenCalledWith(['/']);
  });

  it('shows an error for invalid credentials', async () => {
    login.mockReturnValue(throwError(() => new Error('401')));
    const {fixture, el} = render();
    await submit(fixture, el);
    expect(el.querySelector('[role=alert]')?.textContent).toContain('Invalid username or password');
  });

  it('offers OIDC only when it is enabled', () => {
    expect(render().el.querySelector('[data-testid=oidc]')).toBeNull();
    TestBed.resetTestingModule();
    const {el} = render({oidcEnabled: true, oidcLoginPath: '/oauth2/authorization/idp'});
    expect(el.querySelector('[data-testid=oidc]')?.getAttribute('href')).toBe('/oauth2/authorization/idp');
  });

  it('offers no registration', () => {
    expect(render().el.textContent?.toLowerCase()).not.toContain('register');
  });
});
