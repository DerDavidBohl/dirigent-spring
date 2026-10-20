import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Secret} from './models';
import {FederationTarget} from '../federation/federation-target';

@Injectable({providedIn: 'root'})
export class SecretsApi {
  private readonly http = inject(HttpClient);
  private readonly target = inject(FederationTarget);

  list(): Observable<Secret[]> {
    return this.http.get<Secret[]>(this.target.apiPath('/secrets'));
  }

  put(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.put<void>(this.target.apiPath(`/secrets/${encodeURIComponent(secret.key ?? '')}?restartDeployments=${restartDeployments}`), secret);
  }

  remove(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.delete<void>(this.target.apiPath(`/secrets/${encodeURIComponent(secret.key ?? '')}?restartDeployments=${restartDeployments}`));
  }
}
