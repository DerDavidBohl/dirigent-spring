import {TestBed} from '@angular/core/testing';
import {Router, provideRouter} from '@angular/router';
import {Observable, Subject, of, throwError} from 'rxjs';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {DeploymentState} from '../../core/api/models';
import {ConfirmResult} from '../../ui/confirm-dialog';
import {ConfirmService} from '../../ui/confirm.service';
import {FederationTarget} from '../../core/federation/federation-target';
import {Deployments} from './deployments';

describe('Deployments', () => {
  const list = vi.fn<() => Observable<DeploymentState[]>>();
  const start = vi.fn<(name: string, force: boolean) => Observable<void>>();
  const stop = vi.fn<(name: string) => Observable<void>>();
  const confirm = vi.fn<() => Observable<ConfirmResult | undefined>>();

  const dep = (name: string, state: string, source = 'https://git.example/' + name): DeploymentState => ({name, state, message: `${name} msg`, source});

  async function render(items: DeploymentState[], url = '/deployments') {
    list.mockReturnValue(of(items));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{path: 'deployments', component: Deployments}]),
        {provide: DeploymentsApi, useValue: {list, start, stop}},
        {provide: ConfirmService, useValue: {confirm}}
      ]
    });
    const router = TestBed.inject(Router);
    await router.navigateByUrl(url);
    vi.useFakeTimers();
    const fixture = TestBed.createComponent(Deployments);
    vi.advanceTimersByTime(0);
    fixture.detectChanges();
    return {fixture, router, el: fixture.nativeElement as HTMLElement};
  }

  const names = (el: HTMLElement) => Array.from(el.querySelectorAll('[data-testid=row]')).map(r => r.querySelector('td')?.textContent?.trim());
  const rowButton = (el: HTMLElement, label: string) =>
    Array.from(el.querySelectorAll('[data-testid=row] button')).find(b => b.textContent?.trim().startsWith(label)) as HTMLButtonElement;

  beforeEach(() => {
    start.mockReset().mockReturnValue(of(undefined));
    stop.mockReset().mockReturnValue(of(undefined));
    confirm.mockReset();
  });
  afterEach(() => vi.useRealTimers());

  it('lists deployments sorted by name with source, state and message', async () => {
    const {el} = await render([dep('b', 'RUNNING'), dep('a', 'FAILED')]);
    expect(names(el)).toEqual(['a', 'b']);
    expect(el.querySelector('[data-testid=row] a')?.getAttribute('href')).toBe('https://git.example/a');
    expect(el.textContent).toContain('a msg');
  });

  it('filters by name and state from the URL', async () => {
    const {el} = await render([dep('web', 'RUNNING'), dep('db', 'RUNNING'), dep('web2', 'FAILED')], '/deployments?q=web&state=RUNNING');
    expect(names(el)).toEqual(['web']);
  });

  it('sorts descending from the URL', async () => {
    const {el} = await render([dep('a', 'RUNNING'), dep('b', 'RUNNING')], '/deployments?sort=name&dir=desc');
    expect(names(el)).toEqual(['b', 'a']);
  });

  it('writes search, filter and sort into the URL', async () => {
    const {fixture, router, el} = await render([dep('a', 'RUNNING')]);
    const input = el.querySelector('input[type=search]') as HTMLInputElement;
    input.value = 'x';
    input.dispatchEvent(new Event('input'));
    await vi.advanceTimersByTimeAsync(0);
    expect(router.url).toContain('q=x');

    (el.querySelector('[data-testid=sort-name]') as HTMLButtonElement).click();
    await vi.advanceTimersByTimeAsync(0);
    expect(router.url).toContain('dir=desc');
    fixture.detectChanges();
    expect(el.querySelector('th[aria-sort=descending]')).not.toBeNull();
  });

  it('shows an empty state when nothing matches', async () => {
    const {el} = await render([dep('a', 'RUNNING')], '/deployments?q=zzz');
    expect(el.querySelector('[data-testid=empty]')).not.toBeNull();
  });

  it('starts after confirmation with the force-recreate choice', async () => {
    confirm.mockReturnValue(of({option: true}));
    const {el} = await render([dep('a', 'STOPPED')]);
    rowButton(el, 'Start').click();
    expect(start).toHaveBeenCalledWith('a', true);
  });

  it('does not start when the dialog is cancelled', async () => {
    confirm.mockReturnValue(of(undefined));
    const {el} = await render([dep('a', 'STOPPED')]);
    rowButton(el, 'Start').click();
    expect(start).not.toHaveBeenCalled();
  });

  it('invalidates deployment rows and pending actions when the selected instance changes', async () => {
    const answer = new Subject<ConfirmResult | undefined>();
    confirm.mockReturnValue(answer);
    const {fixture, el} = await render([dep('a', 'STOPPED')]);
    rowButton(el, 'Start').click();

    const remoteResponse = new Subject<DeploymentState[]>();
    list.mockReturnValue(remoteResponse);
    TestBed.inject(FederationTarget).select('office');
    fixture.detectChanges();
    expect(el.querySelectorAll('[data-testid=row]')).toHaveLength(0);

    answer.next({option: false});
    expect(start).not.toHaveBeenCalled();
  });

  it('stops only after the operator confirms', async () => {
    const answer = new Subject<ConfirmResult | undefined>();
    confirm.mockReturnValue(answer);
    const {el} = await render([dep('a', 'RUNNING')]);
    rowButton(el, 'Stop').click();
    expect(stop).not.toHaveBeenCalled();
    answer.next({option: false});
    expect(stop).toHaveBeenCalledWith('a');
  });

  it('sends no stop request when the stop dialog is cancelled', async () => {
    confirm.mockReturnValue(of(undefined));
    const {el} = await render([dep('a', 'RUNNING')]);
    rowButton(el, 'Stop').click();
    expect(stop).not.toHaveBeenCalled();
  });

  it('reports a failed action', async () => {
    confirm.mockReturnValue(of({option: false}));
    stop.mockReturnValue(throwError(() => new Error('x')));
    const {fixture, el} = await render([dep('a', 'RUNNING')]);
    rowButton(el, 'Stop').click();
    fixture.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('Stopping a failed');
  });
});
