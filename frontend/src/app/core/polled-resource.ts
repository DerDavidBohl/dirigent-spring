import {DOCUMENT, DestroyRef, Signal, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {EMPTY, Observable, Subject, catchError, distinctUntilChanged, fromEvent, map, merge, startWith, switchMap, tap, timer} from 'rxjs';

export interface PolledResource<T> {
  readonly data: Signal<T | null>;
  readonly loading: Signal<boolean>;
  readonly error: Signal<boolean>;
  readonly lastUpdated: Signal<Date | null>;
  refresh(): void;
}

// UX-005: periodic polling with explicit loading, error, and last-refreshed state; pauses in hidden tabs.
export function polledResource<T>(fetch: () => Observable<T>, intervalMs = 2000): PolledResource<T> {
  const document = inject(DOCUMENT);
  const data = signal<T | null>(null);
  const loading = signal(true);
  const error = signal(false);
  const lastUpdated = signal<Date | null>(null);
  const manualRefresh = new Subject<void>();

  fromEvent(document, 'visibilitychange').pipe(
    startWith(null),
    map(() => !document.hidden),
    distinctUntilChanged(),
    switchMap(visible => (visible ? merge(timer(0, intervalMs), manualRefresh) : EMPTY)),
    switchMap(() => fetch().pipe(
      tap(value => {
        data.set(value);
        error.set(false);
        lastUpdated.set(new Date());
        loading.set(false);
      }),
      catchError(() => {
        error.set(true);
        loading.set(false);
        return EMPTY;
      })
    )),
    takeUntilDestroyed(inject(DestroyRef))
  ).subscribe();

  return {
    data: data.asReadonly(),
    loading: loading.asReadonly(),
    error: error.asReadonly(),
    lastUpdated: lastUpdated.asReadonly(),
    refresh: () => manualRefresh.next()
  };
}
