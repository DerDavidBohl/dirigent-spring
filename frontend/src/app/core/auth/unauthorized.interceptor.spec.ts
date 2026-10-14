import {TestBed} from '@angular/core/testing';
import {HttpClient, provideHttpClient, withInterceptors} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {Router, provideRouter} from '@angular/router';
import {AuthService} from './auth.service';
import {unauthorizedInterceptor} from './unauthorized.interceptor';

describe('unauthorizedInterceptor', () => {
  let http: HttpTestingController;
  let client: HttpClient;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([unauthorizedInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    auth = TestBed.inject(AuthService);
    auth.user.set({username: 'ada', apiToken: false});
  });

  it('ends the local session and goes to login on a 401 from a feature API', () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    client.get('api/v1/deployments').subscribe({error: () => undefined});
    http.expectOne('api/v1/deployments').flush('', {status: 401, statusText: 'Unauthorized'});
    expect(auth.user()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });

  it('leaves the session alone for auth endpoints', () => {
    client.post('api/v1/auth/login', {}).subscribe({error: () => undefined});
    http.expectOne('api/v1/auth/login').flush('', {status: 401, statusText: 'Unauthorized'});
    expect(auth.user()).not.toBeNull();
  });
});
