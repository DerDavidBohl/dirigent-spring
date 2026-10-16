import {Injectable, inject, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, catchError, map, of, switchMap, tap} from 'rxjs';
import {AuthOptions, AuthenticatedUser} from '../api/models';

// ADR-007: the backend owns browser sessions; the UI only mirrors their state.
@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly http = inject(HttpClient);

  readonly options = signal<AuthOptions>({oidcEnabled: false, oidcLoginPath: null});
  readonly user = signal<AuthenticatedUser | null>(null);
  readonly initialized = signal(false);

  initialize(): Observable<AuthenticatedUser | null> {
    return this.refreshCsrf().pipe(
      switchMap(() => this.http.get<AuthOptions>('api/v1/auth/options').pipe(
        catchError(() => of({oidcEnabled: false, oidcLoginPath: null}))
      )),
      tap(options => this.options.set(options)),
      switchMap(() => this.http.get<AuthenticatedUser>('api/v1/auth/me').pipe(catchError(() => of(null)))),
      tap(user => {
        this.user.set(user);
        this.initialized.set(true);
      })
    );
  }

  login(username: string, password: string): Observable<void> {
    const body = new URLSearchParams({username, password});
    return this.http.post<void>('api/v1/auth/login', body.toString(), {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'}
    });
  }

  // UX-006: the session is only considered ended once the backend confirms it.
  logout(): Observable<void> {
    return this.http.post<void>('api/v1/auth/logout', {}).pipe(
      tap(() => this.user.set(null)),
      switchMap(() => this.refreshCsrf()),
      map(() => undefined)
    );
  }

  private refreshCsrf(): Observable<string> {
    return this.http.get('api/v1/auth/csrf', {responseType: 'text'}).pipe(catchError(() => of('')));
  }
}
