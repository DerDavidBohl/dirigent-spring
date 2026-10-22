import {TestBed} from '@angular/core/testing';
import {Observable, Subject, of, throwError} from 'rxjs';
import {UpdatesApi} from '../../core/api/updates-api';
import {DeploymentUpdate} from '../../core/api/models';
import {FederationTarget} from '../../core/federation/federation-target';
import {Updates} from './updates';

describe('Updates', () => {
  const list = vi.fn<() => Observable<DeploymentUpdate[]>>();
  const check = vi.fn<() => Observable<void>>();
  const run = vi.fn<(u: DeploymentUpdate) => Observable<void>>();

  const update = (name: string, isRunning = false): DeploymentUpdate => ({deploymentName: name, service: 'app', image: `img-${name}`, isRunning});

  function render(items: DeploymentUpdate[], nextItems?: DeploymentUpdate[]) {
    list.mockReset().mockReturnValueOnce(of(items));
    if (nextItems) {
      list.mockReturnValue(of(nextItems));
    }
    TestBed.configureTestingModule({providers: [{provide: UpdatesApi, useValue: {list, check, run}}]});
    const fixture = TestBed.createComponent(Updates);
    vi.advanceTimersByTime(0);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  beforeEach(() => {
    vi.useFakeTimers();
    check.mockReset();
    run.mockReset();
  });
  afterEach(() => vi.useRealTimers());

  it('lists available updates', () => {
    const {el} = render([update('a'), update('b')]);
    expect(el.querySelectorAll('[data-testid=update]').length).toBe(2);
  });

  it('shows an empty state', () => {
    const {el} = render([]);
    expect(el.querySelector('[data-testid=empty]')).not.toBeNull();
  });

  it('replaces rows immediately when the selected instance changes', () => {
    const {fixture, el} = render([update('local')], [update('remote')]);
    expect(el.textContent).toContain('local');

    TestBed.inject(FederationTarget).select('office');
    vi.advanceTimersByTime(0);
    fixture.detectChanges();

    expect(el.textContent).not.toContain('local');
    expect(el.textContent).toContain('remote');
  });

  it('disables the row action while an update is running', () => {
    const {el} = render([update('a', true)]);
    const button = el.querySelector('[data-testid=update] button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  it('runs an update', () => {
    run.mockReturnValue(of(undefined));
    const {el} = render([update('a')]);
    (el.querySelector('[data-testid=update] button') as HTMLButtonElement).click();
    expect(run).toHaveBeenCalledWith(update('a'));
  });

  it('changes the check label while the request is pending', () => {
    const pending = new Subject<void>();
    check.mockReturnValue(pending);
    const {fixture, el} = render([]);
    const button = el.querySelector('button') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(button.textContent).toContain('Checking');
    pending.next();
    fixture.detectChanges();
    expect(button.textContent).toContain('Check for updates');
  });

  it('reports a failed check', () => {
    check.mockReturnValue(throwError(() => new Error('x')));
    const {fixture, el} = render([]);
    (el.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('could not be started');
  });
});
