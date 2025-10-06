import {Injectable} from '@angular/core';
import {Observable, ReplaySubject, tap} from 'rxjs';
import {Deployment} from './deployment';
import {HttpClient} from '@angular/common/http';
import {Secret} from './secret';
import {SystemInformation} from './system-information';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private _deploymentStates: ReplaySubject<Array<Deployment>> = new ReplaySubject<Array<Deployment>>(1);
  private _secrets: ReplaySubject<Array<Secret>> = new ReplaySubject<Array<Secret>>(1);

  constructor(private http: HttpClient) {
  }

  get deploymentStates$(): Observable<Array<Deployment>> {
    return this._deploymentStates.asObservable();
  }

  get secrets$(): Observable<Array<Secret>> {
    return this._secrets.asObservable();
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
