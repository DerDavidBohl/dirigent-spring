import {inject} from '@angular/core';
import {CanActivateFn, Router} from '@angular/router';
import {map} from 'rxjs';
import {AuthService} from './auth.service';

function sessionGuard(requireSession: boolean): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const decide = (user: unknown) => {
      if (requireSession) {
        return user ? true : router.createUrlTree(['/login']);
      }
      return user ? router.createUrlTree(['/']) : true;
    };
    return auth.initialized() ? decide(auth.user()) : auth.initialize().pipe(map(decide));
  };
}

export const authGuard = sessionGuard(true);
export const guestGuard = sessionGuard(false);
