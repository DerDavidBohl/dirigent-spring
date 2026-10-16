import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Secret} from './models';

@Injectable({providedIn: 'root'})
export class SecretsApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Secret[]> {
    return this.http.get<Secret[]>('api/v1/secrets');
  }

  put(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.put<void>(`api/v1/secrets/${encodeURIComponent(secret.key ?? '')}?restartDeployments=${restartDeployments}`, secret);
  }

  remove(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.delete<void>(`api/v1/secrets/${encodeURIComponent(secret.key ?? '')}?restartDeployments=${restartDeployments}`);
  }
}
