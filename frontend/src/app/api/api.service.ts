import {Injectable} from '@angular/core';
import {Observable, ReplaySubject, tap} from 'rxjs';
import {DeploymentState} from './deployment-state';
import {HttpClient} from '@angular/common/http';
import {Secret} from './secret';
import {SystemInformation} from './system-information';
import { DeploymentUpdate } from './deployment-update';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private _deploymentStates: ReplaySubject<Array<DeploymentState>> = new ReplaySubject<Array<DeploymentState>>(1);
  private _secrets: ReplaySubject<Array<Secret>> = new ReplaySubject<Array<Secret>>(1);
  private _deploymentUpdates: ReplaySubject<Array<DeploymentUpdate>> = new ReplaySubject<Array<DeploymentUpdate>>;

  constructor(private http: HttpClient) {
  }

  get deploymentStates$(): Observable<Array<DeploymentState>> {
    return this._deploymentStates.asObservable();
  }

  get secrets$(): Observable<Array<Secret>> {
    return this._secrets.asObservable();
  }

  get deploymentUpdates$(): Observable<Array<DeploymentUpdate>> {
    return this._deploymentUpdates.asObservable();
  }

  reloadDeployementUpdates(): void {
    this.getDeploymentUpdates().subscribe(r => this._deploymentUpdates.next(r))
  }

  updateDeployment(deploymentName: string): Observable<void> {
    return this.http.post<void>(`api/v1/deployment-updates/${deploymentName}/run`, {});
  }

  checkForUpdates() {
    return this.http.post<void>(`api/v1/deployment-updates/check`, {});
  }

  getDeploymentUpdates(): Observable<Array<DeploymentUpdate>> {
    return this.http.get<Array<DeploymentUpdate>>('api/v1/deployment-updates');
  }

  reloadDeploymentStates(): void {
    this.getAllDeploymentStates().subscribe(r => this._deploymentStates.next(r));
  }

  getAllDeploymentStates(): Observable<Array<DeploymentState>> {
    return this.http.get<Array<DeploymentState>>('api/v1/deployments');
  }

  getSystemInformation(): Observable<SystemInformation> {
    return this.http.get<SystemInformation>('api/v1/system-information');
  }

  stopDeployment(deploymentState: DeploymentState): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/stop`, {});
  }

  startDeployment(deploymentState: DeploymentState, force: boolean): Observable<void> {
    return this.http.post<void>(`api/v1/deployments/${deploymentState.name}/start?forceRecreate=${force}`, {});
  }

  getSecrets(): Observable<Array<Secret>> {
    return this.http.get<Array<Secret>>('api/v1/secrets');
  }

  reloadSecrets(): void {
    this.getSecrets().subscribe((r) => this._secrets.next(r));
  }

  putSecret(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.put<void>(`api/v1/secrets/${secret.key}?restartDeployments=${restartDeployments}`, secret).pipe(
      tap(() => this.reloadSecrets())
    );
  }

  deleteSecret(secret: Secret, restartDeployments: boolean): Observable<void> {
    return this.http.delete<void>(`api/v1/secrets/${secret.key}?restartDeployments=${restartDeployments}`).pipe(
      tap(() => this.reloadSecrets())
    );
  }
}
