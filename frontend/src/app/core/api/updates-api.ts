import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {DeploymentUpdate} from './models';

@Injectable({providedIn: 'root'})
export class UpdatesApi {
  private readonly http = inject(HttpClient);

  list(): Observable<DeploymentUpdate[]> {
    return this.http.get<DeploymentUpdate[]>('api/v1/deployment-updates');
  }

  check(): Observable<void> {
    return this.http.post<void>('api/v1/deployment-updates/check', {});
  }

  run(update: DeploymentUpdate): Observable<void> {
    return this.http.post<void>('api/v1/deployment-updates/run', update);
  }
}
