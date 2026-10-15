import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Observable, of, throwError} from 'rxjs';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {UpdatesApi} from '../../core/api/updates-api';
import {SystemApi} from '../../core/api/system-api';
import {DeploymentState, DeploymentUpdate} from '../../core/api/models';
import {Dashboard} from './dashboard';

describe('Dashboard', () => {
  const list = vi.fn<() => Observable<DeploymentState[]>>();
  const updatesList = vi.fn<() => Observable<DeploymentUpdate[]>>();
  const check = vi.fn<() => Observable<void>>();

  function render(deployments: DeploymentState[], updates: DeploymentUpdate[] = []) {
    list.mockReturnValue(of(deployments));
    updatesList.mockReturnValue(of(updates));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: DeploymentsApi, useValue: {list, start: vi.fn(), stop: vi.fn()}},
        {provide: UpdatesApi, useValue: {list: updatesList, check}},
        {provide: SystemApi, useValue: {info: () => of({instanceName: 'home-lab', gitUrl: 'https://git.example/repo.git'})}}
      ]
    });
    const fixture = TestBed.createComponent(Dashboard);
    vi.advanceTimersByTime(0);
    fixture.detectChanges();
    return {fixture, el: fixture.nativeElement as HTMLElement};
  }

  const dep = (name: string, state: string, message: string | null = null): DeploymentState => ({name, state, message, source: 'src'});

  beforeEach(() => {
    vi.useFakeTimers();
    check.mockReset();
  });
  afterEach(() => vi.useRealTimers());

  it('shows nonzero deployment counts and links tiles to the filtered deployments page', () => {
    const {el} = render([dep('a', 'RUNNING'), dep('b', 'RUNNING'), dep('c', 'FAILED', 'boom')]);
    const tiles = Array.from(el.querySelectorAll('[data-testid=state-tile]'));
    expect(tiles.length).toBe(2);
    const running = tiles.find(t => t.textContent?.includes('RUNNING'))!;
    expect(running.querySelector('[data-testid=tile-count]')?.textContent?.trim()).toBe('2');
    expect(running.getAttribute('href')).toBe('/deployments?state=RUNNING');
    expect(tiles.find(t => t.textContent?.includes('FAILED'))?.getAttribute('href')).toBe('/deployments?state=FAILED');
    expect(tiles.some(t => t.textContent?.includes('STOPPED'))).toBe(false);
  });

  it('lists failed deployments with their message', () => {
    const {el} = render([dep('a', 'RUNNING'), dep('c', 'FAILED', 'compose exited 1')]);
    const items = el.querySelectorAll('[data-testid=failed-item]');
    expect(items.length).toBe(1);
    expect(items[0].textContent).toContain('compose exited 1');
  });

  it('says so when nothing needs attention', () => {
    const {el} = render([dep('a', 'RUNNING')]);
    expect(el.querySelector('[data-testid=no-failures]')).not.toBeNull();
  });

  it('shows pending updates and system information', () => {
    const {el} = render([], [{deploymentName: 'web', service: 'app', image: 'nginx:1', isRunning: false}]);
    expect(el.querySelector('[data-testid=update-item]')?.textContent).toContain('nginx:1');
    expect(el.querySelector('[data-testid=instance-name]')?.textContent).toBe('home-lab');
    expect(el.querySelector('[data-testid=git-url]')?.textContent).toContain('git.example');
    const gitLink = el.querySelector('[data-testid=git-url] a');
    expect(gitLink?.getAttribute('href')).toBe('https://git.example/repo.git');
    expect(gitLink?.getAttribute('target')).toBe('_blank');
    expect(gitLink?.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('offers no lifecycle actions', () => {
    const {el} = render([dep('a', 'RUNNING')]);
    const labels = Array.from(el.querySelectorAll('button')).map(b => b.textContent?.trim());
    expect(labels).toEqual(['Check for updates']);
  });

  it('starts an update check and refreshes the updates', () => {
    check.mockReturnValue(of(undefined));
    const {fixture, el} = render([]);
    (el.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(check).toHaveBeenCalledTimes(1);
    expect(updatesList).toHaveBeenCalledTimes(2);
  });

  it('reports a failed update check', () => {
    check.mockReturnValue(throwError(() => new Error('x')));
    const {fixture, el} = render([]);
    (el.querySelector('button') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(el.querySelector('[role=alert]')?.textContent).toContain('could not be started');
  });

  it('shows an error state when deployments cannot be loaded', () => {
    list.mockReturnValue(throwError(() => new Error('down')));
    updatesList.mockReturnValue(of([]));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {provide: DeploymentsApi, useValue: {list}},
        {provide: UpdatesApi, useValue: {list: updatesList, check}},
        {provide: SystemApi, useValue: {info: () => of({instanceName: 'x', gitUrl: 'y'})}}
      ]
    });
    const fixture = TestBed.createComponent(Dashboard);
    vi.advanceTimersByTime(0);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Could not refresh');
  });
});
