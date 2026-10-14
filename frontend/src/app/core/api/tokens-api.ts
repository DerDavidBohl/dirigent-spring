import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {CreatedMachineToken, MachineToken} from './models';

@Injectable({providedIn: 'root'})
export class TokensApi {
  private readonly http = inject(HttpClient);

  list(): Observable<MachineToken[]> {
    return this.http.get<MachineToken[]>('api/v1/auth/tokens');
  }

  create(name: string, permissions: string[], expiresAt: string | null): Observable<CreatedMachineToken> {
    return this.http.post<CreatedMachineToken>('api/v1/auth/tokens', {name, permissions, expiresAt});
  }

  revoke(id: number): Observable<void> {
    return this.http.delete<void>(`api/v1/auth/tokens/${id}`);
  }
}
