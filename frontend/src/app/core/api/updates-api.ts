import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DeploymentUpdate} from './models';
import {FederationTarget} from '../federation/federation-target';

@Injectable({providedIn: 'root'})
export class UpdatesApi {
  private readonly http = inject(HttpClient);
  private readonly target = inject(FederationTarget);

  list(): Observable<DeploymentUpdate[]> {
    return this.http.get<DeploymentUpdate[]>(this.target.apiPath('/deployment-updates'));
  }

  check(): Observable<void> {
    return this.http.post<void>(this.target.apiPath('/deployment-updates/check'), {});
  }

  run(update: DeploymentUpdate): Observable<void> {
    return this.http.post<void>(this.target.apiPath('/deployment-updates/run'), update);
  }
}
