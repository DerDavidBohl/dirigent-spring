import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {AuthService} from '../core/auth/auth.service';
import {ThemeService} from '../core/theme/theme.service';
import {FederationDirectory} from '../core/federation/federation-directory';
import {FederationTarget} from '../core/federation/federation-target';

interface NavItem {
  label: string;
  path: string;
  exact?: boolean;
}

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {class: 'flex min-h-screen flex-col md:flex-row'},
  template: `
    <a href="#main" class="sr-only z-50 rounded bg-brand-600 px-3 py-2 text-white focus:not-sr-only focus:absolute focus:left-2 focus:top-2">Skip to content</a>

    <aside class="border-b border-slate-200 bg-white md:w-60 md:shrink-0 md:border-b-0 md:border-r dark:border-slate-800 dark:bg-slate-900">
      <div class="flex items-center justify-between px-4 py-3 md:py-5">
        <a routerLink="/" queryParamsHandling="merge" class="flex items-center gap-2 text-lg font-semibold">
          <img src="icon-no-background.svg" alt="" class="size-8">
          Dirigent
        </a>
        <button type="button" class="rounded-md px-3 py-1.5 text-sm ring-1 ring-slate-300 md:hidden dark:ring-slate-600"
                aria-controls="primary-nav" [attr.aria-expanded]="menuOpen()" (click)="menuOpen.set(!menuOpen())">
          Menu
        </button>
      </div>
      <nav id="primary-nav" aria-label="Primary" class="px-2 pb-3 md:block md:pb-0" [class.hidden]="!menuOpen()">
        <ul class="flex flex-col gap-1">
          @for (item of items; track item.path) {
            <li>
              <a [routerLink]="item.path" queryParamsHandling="merge" routerLinkActive="bg-brand-50 text-brand-800 dark:bg-brand-950 dark:text-brand-200"
                 [routerLinkActiveOptions]="{exact: item.exact ?? false}" ariaCurrentWhenActive="page"
                 class="block rounded-md px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800"
                 (click)="menuOpen.set(false)">
                {{ item.label }}
              </a>
            </li>
          }
        </ul>
      </nav>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <header class="flex flex-wrap items-center justify-end gap-3 border-b border-slate-200 bg-white px-4 py-3 dark:border-slate-800 dark:bg-slate-900">
        @if (logoutError()) {
          <p role="alert" class="mr-auto text-sm text-alert-700 dark:text-alert-300">{{ logoutError() }}</p>
        }
        <label class="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
          Instance
          <select aria-label="Selected instance" class="max-w-56 rounded-md border border-slate-300 bg-white px-2 py-1.5 text-slate-900 dark:border-slate-600 dark:bg-slate-950 dark:text-slate-100"
                  [value]="target.selectedId()" (change)="selectInstance($any($event.target).value)">
            @for (instance of directory.instances(); track instance.id) {
              <option [value]="instance.id" [disabled]="!instance.available">{{ instance.instanceName }}{{ instance.available ? '' : ' (offline)' }}</option>
            }
            @if (directory.instances().length === 0) {
              <option value="local">Local instance</option>
            }
          </select>
        </label>
        <button type="button" class="rounded-md px-2 py-1.5 text-sm ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800"
                [disabled]="directory.loading()" (click)="directory.refresh()">Refresh instances</button>
        <span class="text-sm text-slate-600 dark:text-slate-300" data-testid="username">{{ auth.user()?.username }}</span>
        <button type="button" class="rounded-md px-3 py-1.5 text-sm ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800"
                [attr.aria-pressed]="theme.theme() === 'dark'" (click)="theme.toggle()">
          {{ theme.theme() === 'dark' ? 'Dark theme' : 'Light theme' }}
        </button>
        <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
                [disabled]="signingOut()" (click)="signOut()">
          Sign out
        </button>
      </header>
      <main id="main" tabindex="-1" class="flex-1 p-4 outline-none md:p-6">
        <router-outlet />
      </main>
    </div>
  `
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly theme = inject(ThemeService);
  protected readonly directory = inject(FederationDirectory);
  protected readonly target = inject(FederationTarget);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => this.target.select(params.get('instance') ?? 'local'));
  }

  protected readonly items: NavItem[] = [
    {label: 'Dashboard', path: '/', exact: true},
    {label: 'Instances', path: '/instances'},
    {label: 'Deployments', path: '/deployments'},
    {label: 'Updates', path: '/updates'},
    {label: 'Secrets', path: '/secrets'},
    {label: 'Machine tokens', path: '/settings/tokens'}
  ];

  protected readonly menuOpen = signal(false);
  protected readonly signingOut = signal(false);
  protected readonly logoutError = signal('');

  protected selectInstance(id: string): void {
    void this.router.navigate([], {relativeTo: this.route, queryParams: {instance: id}, queryParamsHandling: 'merge'});
  }

  protected signOut(): void {
    this.signingOut.set(true);
    this.logoutError.set('');
    this.auth.logout().subscribe({
      next: () => {
        this.signingOut.set(false);
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.signingOut.set(false);
        this.logoutError.set('Sign out failed. Please try again.');
      }
    });
  }
}
