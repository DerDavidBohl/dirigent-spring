import {Injector, runInInjectionContext} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {Observable, Subject, of, throwError} from 'rxjs';
import {polledResource} from './polled-resource';

describe('polledResource', () => {
  let injector: Injector;

  function setHidden(hidden: boolean) {
    Object.defineProperty(document, 'hidden', {configurable: true, get: () => hidden});
    document.dispatchEvent(new Event('visibilitychange'));
  }

  function create<T>(fetch: () => Observable<T>, invalidations = new Subject<void>()) {
    return {resource: runInInjectionContext(injector, () => polledResource(fetch, 2000, invalidations)), invalidations};
  }

  beforeEach(() => {
    vi.useFakeTimers();
    injector = TestBed.inject(Injector);
  });

  afterEach(() => {
    setHidden(false);
    vi.useRealTimers();
  });

  it('starts loading and then exposes the data and refresh time', () => {
    const {resource} = create(() => of('a'));
    expect(resource.loading()).toBe(true);
    vi.advanceTimersByTime(0);
    expect(resource.data()).toBe('a');
    expect(resource.loading()).toBe(false);
    expect(resource.error()).toBe(false);
    expect(resource.lastUpdated()).toBeInstanceOf(Date);
  });

  it('polls every interval', () => {
    const fetch = vi.fn(() => of('a'));
    create(fetch);
    vi.advanceTimersByTime(0);
    vi.advanceTimersByTime(4000);
    expect(fetch).toHaveBeenCalledTimes(3);
  });

  it('reports errors, keeps the last data, and recovers', () => {
    let fail = false;
    const {resource} = create(() => (fail ? throwError(() => new Error('down')) : of('a')));
    vi.advanceTimersByTime(0);
    fail = true;
    vi.advanceTimersByTime(2000);
    expect(resource.error()).toBe(true);
    expect(resource.data()).toBe('a');
    fail = false;
    vi.advanceTimersByTime(2000);
    expect(resource.error()).toBe(false);
  });

  it('pauses while the tab is hidden and resumes when visible', () => {
    const fetch = vi.fn(() => of('a'));
    create(fetch);
    vi.advanceTimersByTime(0);
    setHidden(true);
    vi.advanceTimersByTime(10000);
    expect(fetch).toHaveBeenCalledTimes(1);
    setHidden(false);
    vi.advanceTimersByTime(0);
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('refreshes on demand', () => {
    const fetch = vi.fn(() => of('a'));
    const {resource} = create(fetch);
    vi.advanceTimersByTime(0);
    resource.refresh();
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('clears stale data and reloads on invalidation while visible', () => {
    const invalidations = new Subject<void>();
    const fetch = vi.fn(() => of('a'));
    const {resource} = create(fetch, invalidations);
    vi.advanceTimersByTime(0);
    invalidations.next();
    expect(resource.data()).toBe('a');
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it('clears stale data when invalidated while hidden and reloads when visible', () => {
    const invalidations = new Subject<void>();
    const fetch = vi.fn(() => of('a'));
    const {resource} = create(fetch, invalidations);
    vi.advanceTimersByTime(0);
    setHidden(true);
    invalidations.next();
    expect(resource.data()).toBeNull();
    expect(resource.loading()).toBe(true);
    expect(fetch).toHaveBeenCalledTimes(1);
    setHidden(false);
    vi.advanceTimersByTime(0);
    expect(resource.data()).toBe('a');
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
