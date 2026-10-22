import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SystemInformation} from './models';
import {FederationTarget} from '../federation/federation-target';

@Injectable({providedIn: 'root'})
export class SystemApi {
  private readonly http = inject(HttpClient);
  private readonly target = inject(FederationTarget);

  info(): Observable<SystemInformation> {
    return this.http.get<SystemInformation>(this.target.apiPath('/system-information'));
  }
}
