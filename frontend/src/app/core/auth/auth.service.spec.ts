import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {AuthService} from './auth.service';

describe('AuthService', () => {
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  function initialize(meResponse: 'user' | 'denied') {
    auth.initialize().subscribe();
    http.expectOne('api/v1/auth/csrf').flush('');
    http.expectOne('api/v1/auth/options').flush({oidcEnabled: true, oidcLoginPath: '/oauth2/authorization/idp'});
    const me = http.expectOne('api/v1/auth/me');
    if (meResponse === 'user') {
      me.flush({username: 'ada', apiToken: false});
    } else {
      me.flush('', {status: 401, statusText: 'Unauthorized'});
    }
  }

  it('loads options and the signed-in user', () => {
    initialize('user');
    expect(auth.initialized()).toBe(true);
    expect(auth.user()?.username).toBe('ada');
    expect(auth.options().oidcEnabled).toBe(true);
  });

  it('treats a rejected session as anonymous', () => {
    initialize('denied');
    expect(auth.initialized()).toBe(true);
    expect(auth.user()).toBeNull();
  });

  it('posts credentials form-encoded on login', () => {
    auth.login('ada', 'p&ss').subscribe();
    const req = http.expectOne('api/v1/auth/login');
    expect(req.request.body).toBe('username=ada&password=p%26ss');
    req.flush(null);
  });

  it('clears the user only after logout succeeds', () => {
    initialize('user');
    auth.logout().subscribe({error: () => undefined});
    http.expectOne('api/v1/auth/logout').flush('', {status: 500, statusText: 'Error'});
    expect(auth.user()?.username).toBe('ada');

    auth.logout().subscribe();
    http.expectOne('api/v1/auth/logout').flush(null);
    http.expectOne('api/v1/auth/csrf').flush('');
    expect(auth.user()).toBeNull();
  });
});
