import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {DeploymentsApi} from './deployments-api';
import {UpdatesApi} from './updates-api';
import {SecretsApi} from './secrets-api';
import {SystemApi} from './system-api';
import {TokensApi} from './tokens-api';
import {FederationTarget} from '../federation/federation-target';

describe('API services', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists deployments', () => {
    TestBed.inject(DeploymentsApi).list().subscribe();
    expect(http.expectOne('api/v1/deployments').request.method).toBe('GET');
  });

  it('starts a deployment with the force flag and an encoded name', () => {
    TestBed.inject(DeploymentsApi).start('my app', true).subscribe();
    const req = http.expectOne('api/v1/deployments/my%20app/start?forceRecreate=true');
    expect(req.request.method).toBe('POST');
  });

  it('stops a deployment', () => {
    TestBed.inject(DeploymentsApi).stop('app').subscribe();
    expect(http.expectOne('api/v1/deployments/app/stop').request.method).toBe('POST');
  });

  it('lists, checks and runs updates', () => {
    const api = TestBed.inject(UpdatesApi);
    api.list().subscribe();
    api.check().subscribe();
    const update = {deploymentName: 'a', service: 's', image: 'i', isRunning: false};
    api.run(update).subscribe();
    expect(http.expectOne('api/v1/deployment-updates').request.method).toBe('GET');
    expect(http.expectOne('api/v1/deployment-updates/check').request.method).toBe('POST');
    expect(http.expectOne('api/v1/deployment-updates/run').request.body).toEqual(update);
  });

  it('manages secrets with the restart flag', () => {
    const api = TestBed.inject(SecretsApi);
    const secret = {key: 'db', value: 'x', environmentVariable: 'DB', deployments: ['a']};
    api.list().subscribe();
    api.put(secret, true).subscribe();
    api.remove(secret, false).subscribe();
    expect(http.expectOne('api/v1/secrets').request.method).toBe('GET');
    expect(http.expectOne('api/v1/secrets/db?restartDeployments=true').request.method).toBe('PUT');
    expect(http.expectOne('api/v1/secrets/db?restartDeployments=false').request.method).toBe('DELETE');
  });

  it('reads system information', () => {
    TestBed.inject(SystemApi).info().subscribe();
    expect(http.expectOne('api/v1/system-information').request.method).toBe('GET');
  });

  it('routes operational API calls through the selected remote instance', () => {
    TestBed.inject(FederationTarget).select('office');
    const deployments = TestBed.inject(DeploymentsApi);
    const updates = TestBed.inject(UpdatesApi);
    const secrets = TestBed.inject(SecretsApi);
    const system = TestBed.inject(SystemApi);
    deployments.start('web app', false).subscribe();
    deployments.stop('web app').subscribe();
    updates.list().subscribe();
    updates.check().subscribe();
    updates.run({deploymentName: 'app', service: 'web', image: 'example/app', isRunning: false}).subscribe();
    secrets.list().subscribe();
    const secret = {key: 'db', value: 'sensitive', environmentVariable: 'DB', deployments: ['app']};
    secrets.put(secret, false).subscribe();
    secrets.remove(secret, true).subscribe();
    system.info().subscribe();
    expect(http.expectOne('api/v1/federation/instances/office/deployments/web%20app/start?forceRecreate=false').request.method).toBe('POST');
    expect(http.expectOne('api/v1/federation/instances/office/deployments/web%20app/stop').request.method).toBe('POST');
    expect(http.expectOne('api/v1/federation/instances/office/deployment-updates').request.method).toBe('GET');
    expect(http.expectOne('api/v1/federation/instances/office/deployment-updates/check').request.method).toBe('POST');
    expect(http.expectOne('api/v1/federation/instances/office/deployment-updates/run').request.method).toBe('POST');
    expect(http.expectOne('api/v1/federation/instances/office/secrets').request.method).toBe('GET');
    expect(http.expectOne('api/v1/federation/instances/office/secrets/db?restartDeployments=false').request.method).toBe('PUT');
    expect(http.expectOne('api/v1/federation/instances/office/secrets/db?restartDeployments=true').request.method).toBe('DELETE');
    expect(http.expectOne('api/v1/federation/instances/office/system-information').request.method).toBe('GET');
  });

  it('manages machine tokens', () => {
    TestBed.inject(FederationTarget).select('office');
    const api = TestBed.inject(TokensApi);
    api.list().subscribe();
    api.create('ci', ['webhooks:trigger'], '2030-01-01T00:00:00.000Z').subscribe();
    api.revoke(3).subscribe();
    expect(http.expectOne(r => r.method === 'GET' && r.url === 'api/v1/auth/tokens').request.method).toBe('GET');
    const create = http.expectOne(r => r.method === 'POST' && r.url === 'api/v1/auth/tokens');
    expect(create.request.body).toEqual({name: 'ci', permissions: ['webhooks:trigger'], expiresAt: '2030-01-01T00:00:00.000Z'});
    expect(http.expectOne('api/v1/auth/tokens/3').request.method).toBe('DELETE');
  });
});
