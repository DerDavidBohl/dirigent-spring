import {Injectable, inject} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {SystemInformation} from './models';

@Injectable({providedIn: 'root'})
export class SystemApi {
  private readonly http = inject(HttpClient);

  info(): Observable<SystemInformation> {
    return this.http.get<SystemInformation>('api/v1/system-information');
  }
}
