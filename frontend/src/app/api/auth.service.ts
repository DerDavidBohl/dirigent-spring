import {Injectable, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, catchError, map, of, switchMap, tap} from 'rxjs';

export interface AuthenticatedUser {
  username: string;
  apiToken: boolean;
}

export interface AuthOptions {
  oidcEnabled: boolean;
  oidcLoginPath: string | null;
}

export interface MachineToken {
  id: number;
  name: string;
  permissions: string[];
  createdBy: string;
  createdAt: string;
  expiresAt: string | null;
  revokedAt: string | null;
}

export interface CreatedMachineToken extends Omit<MachineToken, 'createdBy' | 'revokedAt'> {
  token: string;
}

@Injectable({providedIn: 'root'})
export class AuthService {
  constructor(private http: HttpClient) {}

  initialize(): Observable<AuthenticatedUser | null> {
    return this.http.get<string>('api/v1/auth/csrf', {responseType: 'text' as 'json'}).pipe(
      catchError(() => of('')),
      switchMap(() => this.http.get<AuthOptions>('api/v1/auth/options').pipe(
        catchError(() => of({oidcEnabled: false, oidcLoginPath: null}))
      )),
      tap(options => this.options.set(options)),
      switchMap(() => this.http.get<AuthenticatedUser>('api/v1/auth/me').pipe(
        map(user => user),
        catchError(() => of(null))
      )),
      tap(user => this.user.set(user))
    );
  }

  readonly options = signal<AuthOptions>({oidcEnabled: false, oidcLoginPath: null});
  readonly user = signal<AuthenticatedUser | null>(null);

  login(username: string, password: string): Observable<void> {
    const body = new URLSearchParams();
    body.set('username', username);
    body.set('password', password);
    return this.http.post<void>('api/v1/auth/login', body.toString(), {
      headers: {'Content-Type': 'application/x-www-form-urlencoded'}
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>('api/v1/auth/logout', {}).pipe(
      tap(() => this.user.set(null)),
      switchMap(() => this.http.get<string>('api/v1/auth/csrf', {responseType: 'text' as 'json'}).pipe(catchError(() => of('')))),
      map(() => void 0)
    );
  }

  listTokens(): Observable<MachineToken[]> {
    return this.http.get<MachineToken[]>('api/v1/auth/tokens');
  }

  createToken(name: string, permissions: string[], expiresAt: string | null): Observable<CreatedMachineToken> {
    return this.http.post<CreatedMachineToken>('api/v1/auth/tokens', {name, permissions, expiresAt});
  }

  revokeToken(id: number): Observable<void> {
    return this.http.delete<void>(`api/v1/auth/tokens/${id}`);
  }
}