import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DeploymentState} from './models';

// COD-001: lifecycle actions are executed by the backend; the UI only requests them.
@Injectable({providedIn: 'root'})
export class DeploymentsApi {
  private readonly http = inject(HttpClient);

  list(): Observable<DeploymentState[]> {
    return this.http.get<DeploymentState[]>('api/v1/deployments');
  }

  start(name: string, forceRecreate: boolean): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${encodeURIComponent(name)}/start?forceRecreate=${forceRecreate}`, {});
  }

  stop(name: string): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${encodeURIComponent(name)}/stop`, {});
  }
}
