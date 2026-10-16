import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {AuthService} from '../../core/auth/auth.service';

// UX-006: local or OIDC sign-in, no registration; the form stays usable on narrow viewports.
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {class: 'flex min-h-screen items-center justify-center p-4'},
  template: `
    <main class="w-full max-w-sm rounded-xl bg-white p-6 shadow-lg ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700">
      <div class="flex items-center gap-3">
        <img src="icon-no-background.svg" alt="" class="size-10">
        <h1 class="text-2xl font-semibold">Dirigent</h1>
      </div>

      <form class="mt-6 flex flex-col gap-4" (ngSubmit)="submit()">
        <label class="flex flex-col gap-1 text-sm font-medium">
          Username
          <input name="username" autocomplete="username" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
                 [ngModel]="username()" (ngModelChange)="username.set($event)" required>
        </label>
        <label class="flex flex-col gap-1 text-sm font-medium">
          Password
          <input name="password" type="password" autocomplete="current-password" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
                 [ngModel]="password()" (ngModelChange)="password.set($event)" required>
        </label>
        @if (error()) {
          <p role="alert" class="text-sm text-alert-700 dark:text-alert-300">{{ error() }}</p>
        }
        <button type="submit" class="rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60" [disabled]="pending()">
          {{ pending() ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>

      @if (auth.options().oidcEnabled && auth.options().oidcLoginPath) {
        <a [href]="auth.options().oidcLoginPath" data-testid="oidc"
           class="mt-4 block rounded-md px-3 py-2 text-center text-sm font-medium ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800">
          Sign in with OIDC
        </a>
      }
    </main>
  `
})
export class Login {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly username = signal('');
  protected readonly password = signal('');
  protected readonly pending = signal(false);
  protected readonly error = signal('');

  constructor() {
    if (!this.auth.initialized()) {
      this.auth.initialize().subscribe();
    }
  }

  protected submit(): void {
    this.pending.set(true);
    this.error.set('');
    this.auth.login(this.username(), this.password()).subscribe({
      next: () => {
        this.password.set('');
        this.auth.initialize().subscribe(user => {
          this.pending.set(false);
          if (user) {
            void this.router.navigate(['/']);
          } else {
            this.error.set('Invalid username or password.');
          }
        });
      },
      error: () => {
        this.pending.set(false);
        this.error.set('Invalid username or password.');
      }
    });
  }
}
