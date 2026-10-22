import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {Observable, of, throwError} from 'rxjs';
import {CreatedMachineToken, MachineToken} from '../../core/api/models';
import {TokensApi} from '../../core/api/tokens-api';
import {FederationDirectory} from '../../core/federation/federation-directory';
import {FederationTarget} from '../../core/federation/federation-target';
import {Tokens, expiryToIso, tokenStatus} from './tokens';

describe('token helpers', () => {
  it('maps a calendar date to 00:00 UTC', () => {
    expect(expiryToIso('2030-03-05')).toBe('2030-03-05T00:00:00.000Z');
  });

  it('derives the token status', () => {
    const base: MachineToken = {id: 1, name: 't', permissions: [], createdBy: 'x', createdAt: '', expiresAt: '2030-01-01T00:00:00Z', revokedAt: null};
    const now = Date.parse('2029-01-01T00:00:00Z');
    expect(tokenStatus(base, now)).toBe('Active');
    expect(tokenStatus({...base, expiresAt: '2028-01-01T00:00:00Z'}, now)).toBe('Expired');
    expect(tokenStatus({...base, expiresAt: null}, now)).toBe('Inactive: no expiry');
    expect(tokenStatus({...base, revokedAt: '2028-01-01T00:00:00Z'}, now)).toBe('Revoked');
  });
});

describe('Tokens', () => {
  const list = vi.fn<() => Observable<MachineToken[]>>();
  const create = vi.fn<(n: string, p: string[], e: string | null) => Observable<CreatedMachineToken>>();
  const revoke = vi.fn<(id: number) => Observable<void>>();

  const token: MachineToken = {id: 7, name: 'gitea', permissions: ['webhooks:trigger'], createdBy: 'ada', createdAt: '2030-01-01T00:00:00Z', expiresAt: '2099-06-01T00:00:00Z', revokedAt: null};

  function render() {
    list.mockReturnValue(of([token]));
    TestBed.configureTestingModule({providers: [
      {provide: TokensApi, useValue: {list, create, revoke}},
      {provide: FederationDirectory, useValue: {instances: signal([{id: 'office', instanceName: 'Office', available: true, deployments: [], error: ''}])}}
    ]});
    const fixture = TestBed.createComponent(Tokens);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  async function fill(fixture: ReturnType<typeof render>['fixture'], el: HTMLElement) {
    await fixture.whenStable();
    for (const [name, value] of [['name', 'ci'], ['expiry', '2031-02-03']]) {
      const input = el.querySelector(`[name=${name}]`) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    (el.querySelector('[name="webhooks:trigger"]') as HTMLInputElement).click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    create.mockReset();
    revoke.mockReset().mockReturnValue(of(undefined));
  });

  it('lists tokens with UTC expiry date and status, without credentials', () => {
    const {el} = render();
    const row = el.querySelector('[data-testid=token]')!;
    expect(row.textContent).toContain('gitea');
    expect(row.textContent).toContain('Jun 1, 2099');
    expect(row.querySelector('[data-testid=status]')?.textContent).toBe('Active');
  });

  it('states that tokens expire at 00:00 UTC', () => {
    expect(render().el.textContent).toContain('Expires at 00:00 UTC');
  });

  it('states that tokens are managed only on the main instance without a remote warning', () => {
    const {el} = render();
    expect(el.textContent).toContain('managed only on the main instance');
    expect(el.querySelector('[data-testid=remote-notice]')).toBeNull();
  });

  it('warns that a selected remote does not apply to the tokens shown', () => {
    const {fixture, el} = render();
    TestBed.inject(FederationTarget).select('office');
    fixture.detectChanges();
    expect(el.querySelector('[data-testid=remote-notice]')?.textContent).toContain('Office');
  });

  it('requires name, expiry and a permission before creating', async () => {
    const {fixture, el} = render();
    await fixture.whenStable();
    expect((el.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(true);
    await fill(fixture, el);
    expect((el.querySelector('button[type=submit]') as HTMLButtonElement).disabled).toBe(false);
  });

  it('creates a token with the expiry at 00:00 UTC and shows it once', async () => {
    create.mockReturnValue(of({id: 8, name: 'ci', permissions: ['webhooks:trigger'], createdAt: '', expiresAt: null, token: 'secret-token'}));
    const {fixture, el} = render();
    await fill(fixture, el);
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(create).toHaveBeenCalledWith('ci', ['webhooks:trigger'], '2031-02-03T00:00:00.000Z');
    expect(el.querySelector('[data-testid=issued-token]')?.textContent).toBe('secret-token');
    expect(list).toHaveBeenCalledTimes(2);

    (Array.from(el.querySelectorAll('[data-testid=issued] button')).find(b => b.textContent === 'Dismiss') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('[data-testid=issued]')).toBeNull();
  });

  it('reports a failed creation', async () => {
    create.mockReturnValue(throwError(() => new Error('x')));
    const {fixture, el} = render();
    await fill(fixture, el);
    (el.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('could not be created');
  });

  it('revokes a token and reloads', () => {
    const {el} = render();
    (el.querySelector('[data-testid=token] button') as HTMLButtonElement).click();
    expect(revoke).toHaveBeenCalledWith(7);
    expect(list).toHaveBeenCalledTimes(2);
  });
});
