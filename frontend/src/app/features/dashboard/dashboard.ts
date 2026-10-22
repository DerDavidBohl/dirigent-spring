import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {RouterLink} from '@angular/router';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {UpdatesApi} from '../../core/api/updates-api';
import {SystemApi} from '../../core/api/system-api';
import {polledResource} from '../../core/polled-resource';
import {DEPLOYMENT_STATES} from '../../core/state-tone';
import {StateBadge} from '../../ui/state-badge';
import {StatusLine} from '../../ui/status-line';
import {FederationTarget} from '../../core/federation/federation-target';

// UX-007: read-only overview; lifecycle actions live on the deployments page.
@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, StateBadge, StatusLine],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Dashboard</h1>

    <section aria-labelledby="states-heading" class="mt-6">
      <h2 id="states-heading" class="text-lg font-semibold">Deployments by state</h2>
      <app-status-line [loading]="deployments.loading()" [error]="deployments.error()" [lastUpdated]="deployments.lastUpdated()" />
      <ul class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-4">
        @for (tile of tiles(); track tile.state) {
          <li>
            <a routerLink="/deployments" [queryParams]="{state: tile.state}" queryParamsHandling="merge" data-testid="state-tile"
               class="block rounded-lg bg-white p-4 ring-1 ring-slate-200 hover:ring-brand-400 dark:bg-slate-900 dark:ring-slate-700 dark:hover:ring-accent-400">
              <span class="block text-3xl font-semibold" data-testid="tile-count">{{ tile.count }}</span>
              <app-state-badge [state]="tile.state" />
            </a>
          </li>
        }
      </ul>
    </section>

    <div class="mt-8 grid gap-6 lg:grid-cols-2">
      <section aria-labelledby="attention-heading" class="rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700">
        <h2 id="attention-heading" class="text-lg font-semibold">Needs attention</h2>
        @if (failed().length === 0) {
          <p class="mt-2 text-sm text-slate-600 dark:text-slate-300" data-testid="no-failures">No failed deployments.</p>
        } @else {
          <ul class="mt-2 divide-y divide-slate-200 dark:divide-slate-700">
            @for (item of failed(); track item.name) {
              <li class="py-2" data-testid="failed-item">
                <p class="flex items-center gap-2 font-medium">{{ item.name }} <app-state-badge [state]="item.state" /></p>
                <p class="mt-1 whitespace-pre-wrap break-words text-sm text-slate-600 dark:text-slate-300">{{ item.message }}</p>
              </li>
            }
          </ul>
        }
      </section>

      <section aria-labelledby="updates-heading" class="rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700">
        <div class="flex items-center justify-between gap-2">
          <h2 id="updates-heading" class="text-lg font-semibold">Pending updates</h2>
          <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
                  [disabled]="checking()" (click)="checkForUpdates()">
            {{ checking() ? 'Checking…' : 'Check for updates' }}
          </button>
        </div>
        <app-status-line [loading]="updates.loading()" [error]="updates.error()" [lastUpdated]="updates.lastUpdated()" />
        @if (checkError()) {
          <p role="alert" class="mt-2 text-sm text-alert-700 dark:text-alert-300">The update check could not be started.</p>
        }
        @if ((updates.data() ?? []).length === 0) {
          <p class="mt-2 text-sm text-slate-600 dark:text-slate-300" data-testid="no-updates">No pending updates.</p>
        } @else {
          <ul class="mt-2 divide-y divide-slate-200 dark:divide-slate-700">
            @for (update of updates.data(); track update.deploymentName + update.service) {
              <li class="py-2 text-sm" data-testid="update-item">
                <span class="font-medium">{{ update.deploymentName }}</span> / {{ update.service }}
                <span class="block break-all text-slate-600 dark:text-slate-300">{{ update.image }}</span>
              </li>
            }
          </ul>
        }
      </section>
    </div>

    <section aria-labelledby="system-heading" class="mt-6 rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700">
      <h2 id="system-heading" class="text-lg font-semibold">System</h2>
      @if (system.data(); as info) {
        <dl class="mt-2 grid gap-1 text-sm sm:grid-cols-[max-content_1fr] sm:gap-x-4">
          <dt class="font-medium">Instance</dt>
          <dd data-testid="instance-name">{{ info.instanceName }}</dd>
          <dt class="font-medium">Git source</dt>
          <dd class="break-all" data-testid="git-url"><a href="{{ info.gitUrl }}" target="_blank" rel="noopener noreferrer">{{ info.gitUrl }}</a></dd>
        </dl>
      }
      <app-status-line [loading]="system.loading()" [error]="system.error()" [lastUpdated]="system.lastUpdated()" />
    </section>
  `
})
export class Dashboard {
  private readonly updatesApi = inject(UpdatesApi);
  private readonly deploymentsApi = inject(DeploymentsApi);
  private readonly systemApi = inject(SystemApi);
  private readonly target = inject(FederationTarget);

  protected readonly deployments = polledResource(() => this.deploymentsApi.list(), 2000, this.target.selectionChanges);
  protected readonly updates = polledResource(() => this.updatesApi.list(), 2000, this.target.selectionChanges);
  protected readonly system = polledResource(() => this.systemApi.info(), 60_000, this.target.selectionChanges);

  protected readonly tiles = computed(() => {
    const list = this.deployments.data() ?? [];
    return DEPLOYMENT_STATES.map(state => ({state, count: list.filter(d => d.state === state).length})).filter(item => item.count > 0);
  });
  protected readonly failed = computed(() => (this.deployments.data() ?? []).filter(d => d.state === 'FAILED'));

  protected readonly checking = signal(false);
  protected readonly checkError = signal(false);

  protected checkForUpdates(): void {
    this.checking.set(true);
    this.checkError.set(false);
    this.updatesApi.check().subscribe({
      next: () => {
        this.checking.set(false);
        this.updates.refresh();
      },
      error: () => {
        this.checking.set(false);
        this.checkError.set(true);
      }
    });
  }
}
