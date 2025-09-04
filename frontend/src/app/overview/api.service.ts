import {Injectable} from '@angular/core';
import {Observable, ReplaySubject} from 'rxjs';
import {Deployment} from './deploymentState';
import {HttpClient} from '@angular/common/http';
import { SystemInformation } from './system-information';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private _deploymentStates: ReplaySubject<Array<Deployment>> = new ReplaySubject<Array<Deployment>>(1);

  constructor(private http: HttpClient) {
  }

  get deploymentStates$(): Observable<Array<Deployment>> {
    return this._deploymentStates.asObservable();
  }

  updateDeploymentStates(): void {
    this.getAllDeploymentStates().subscribe(r => this._deploymentStates.next(r));
  }

  getAllDeploymentStates(): Observable<Array<Deployment>> {
    return this.http.get<Array<Deployment>>('api/v1/deployments');
  }

  getSystemInformation(): Observable<SystemInformation> {
    return this.http.get<SystemInformation>('api/v1/system-information');
  }

  stopDeployment(deploymentState: Deployment): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/stop`, {});
  }

  startDeployment(deploymentState: Deployment, force: boolean): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/start?force=${force}`, {});
  }
}
