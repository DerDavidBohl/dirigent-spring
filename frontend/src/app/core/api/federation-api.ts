import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {FederatedInstance} from './models';

@Injectable({providedIn: 'root'})
export class FederationApi {
  private readonly http = inject(HttpClient);

  list(): Observable<FederatedInstance[]> {
    return this.http.get<FederatedInstance[]>('api/v1/federation/instances');
  }
}