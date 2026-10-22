import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DeploymentState} from './models';
import {FederationTarget} from '../federation/federation-target';

// COD-001: lifecycle actions are executed by the backend; the UI only requests them.
@Injectable({providedIn: 'root'})
export class DeploymentsApi {
  private readonly http = inject(HttpClient);
  private readonly target = inject(FederationTarget);

  list(): Observable<DeploymentState[]> {
    return this.http.get<DeploymentState[]>(this.target.apiPath('/deployments'));
  }

  start(name: string, forceRecreate: boolean): Observable<void> {
    return this.http.post<void>(this.target.apiPath(`/deployments/${encodeURIComponent(name)}/start?forceRecreate=${forceRecreate}`), {});
  }

  stop(name: string): Observable<void> {
    return this.http.post<void>(this.target.apiPath(`/deployments/${encodeURIComponent(name)}/stop`), {});
  }
}
