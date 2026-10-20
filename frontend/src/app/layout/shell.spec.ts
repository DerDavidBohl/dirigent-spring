import {TestBed} from '@angular/core/testing';
import {provideRouter, Router} from '@angular/router';
import {Observable, of, throwError} from 'rxjs';
import {signal} from '@angular/core';
import {AuthService} from '../core/auth/auth.service';
import {Shell} from './shell';
import {FederationDirectory} from '../core/federation/federation-directory';
import {FederationTarget} from '../core/federation/federation-target';

describe('Shell', () => {
  const logout = vi.fn<() => Observable<void>>();

  function render() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: AuthService, useValue: {user: signal({username: 'Ada Lovelace', apiToken: false}), logout}},
        {provide: FederationDirectory, useValue: {instances: signal([
          {id: 'local', instanceName: 'Local', available: true, deployments: [], error: ''},
          {id: 'office', instanceName: 'Office', available: true, deployments: [], error: ''}
        ]), loading: signal(false), refresh: vi.fn()}},
        FederationTarget
      ]
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    logout.mockReset();
    localStorage.clear();
  });

  it('links every feature area and shows the signed-in identity', () => {
    const el: HTMLElement = render().nativeElement;
    const hrefs = Array.from(el.querySelectorAll('nav a')).map(a => a.getAttribute('href'));
    expect(hrefs).toEqual(['/', '/instances', '/deployments', '/updates', '/secrets', '/settings/tokens']);
    expect(el.querySelector('[data-testid=username]')?.textContent).toContain('Ada Lovelace');
  });

  it('routes resource selection to the chosen instance', () => {
    const fixture = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const select = fixture.nativeElement.querySelector('select[aria-label="Selected instance"]') as HTMLSelectElement;
    select.value = 'office';
    select.dispatchEvent(new Event('change'));
    expect(TestBed.inject(FederationTarget).selectedId()).toBe('office');
    expect(navigate).toHaveBeenCalledWith(['/instances']);
  });

  it('toggles the theme', () => {
    const fixture = render();
    const toggle = Array.from(fixture.nativeElement.querySelectorAll('button')).find(b => (b as HTMLElement).textContent?.includes('theme')) as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-pressed')).not.toBeNull();
  });

  it('returns to the login view after a successful sign out', () => {
    logout.mockReturnValue(of(undefined));
    const fixture = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find(b => (b as HTMLElement).textContent?.includes('Sign out')) as HTMLButtonElement).click();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('reports a failed sign out without leaving the page', () => {
    logout.mockReturnValue(throwError(() => new Error('x')));
    const fixture = render();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    (Array.from(fixture.nativeElement.querySelectorAll('button')).find(b => (b as HTMLElement).textContent?.includes('Sign out')) as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('Sign out failed');
  });
});
