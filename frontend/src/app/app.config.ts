import {ApplicationConfig} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';
import {provideHttpClient, withInterceptors, withXsrfConfiguration} from '@angular/common/http';
import {routes} from './app.routes';
import {unauthorizedInterceptor} from './core/auth/unauthorized.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withXsrfConfiguration({cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN'}),
      withInterceptors([unauthorizedInterceptor])
    ),
    provideRouter(routes, withComponentInputBinding())
  ]
};
