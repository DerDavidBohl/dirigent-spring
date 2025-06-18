import {Injectable} from '@angular/core';
import {Observable, ReplaySubject} from 'rxjs';
import {DeploymentState} from './deploymentState';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private _deploymentStates: ReplaySubject<Array<DeploymentState>> = new ReplaySubject<Array<DeploymentState>>(1);

  constructor(private http: HttpClient) {
  }

  get deploymentStates$(): Observable<Array<DeploymentState>> {
    return this._deploymentStates.asObservable();
  }

  updateDeploymentStates(): void {
    this.getAllDeploymentStates().subscribe(r => this._deploymentStates.next(r));
  }

  getAllDeploymentStates(): Observable<Array<DeploymentState>> {
    return this.http.get<Array<DeploymentState>>('api/v1/deployment-states');
  }

  stopDeployment(deploymentState: DeploymentState): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/stop`, {});
  }

  startDeployment(deploymentState: DeploymentState, force: boolean): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/start?force=${force}`, {});
  }
}
