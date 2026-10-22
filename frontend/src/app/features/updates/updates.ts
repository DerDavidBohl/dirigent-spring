import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {UpdatesApi} from '../../core/api/updates-api';
import {DeploymentUpdate} from '../../core/api/models';
import {polledResource} from '../../core/polled-resource';
import {StatusLine} from '../../ui/status-line';
import {FederationTarget} from '../../core/federation/federation-target';

// UX-005: the check request only acknowledges asynchronous work; rows expose isRunning.
@Component({
  selector: 'app-updates',
  imports: [StatusLine],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-semibold">Updates</h1>
      <button type="button" class="rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
              [disabled]="checking()" (click)="check()">
        {{ checking() ? 'Checking…' : 'Check for updates' }}
      </button>
    </div>
    <app-status-line class="mt-2 block" [loading]="resource.loading()" [error]="resource.error()" [lastUpdated]="resource.lastUpdated()" />

    @if (actionError()) {
      <p role="alert" class="mt-3 rounded-md bg-alert-50 px-3 py-2 text-sm text-alert-700 dark:bg-alert-900 dark:text-alert-300">{{ actionError() }}</p>
    }

    @if ((resource.data() ?? []).length === 0 && !resource.loading()) {
      <p class="mt-6 text-sm text-slate-600 dark:text-slate-300" data-testid="empty">No image updates available.</p>
    } @else {
      <ul class="mt-4 flex flex-col gap-3">
        @for (update of resource.data(); track update.deploymentName + '/' + update.service) {
          <li class="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700" data-testid="update">
            <div class="min-w-0">
              <p class="font-medium">{{ update.deploymentName }} <span class="font-normal text-slate-500">/ {{ update.service }}</span></p>
              <p class="break-all text-sm text-slate-600 dark:text-slate-300">{{ update.image }}</p>
            </div>
            <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60"
                    [disabled]="update.isRunning" (click)="run(update)">
              {{ update.isRunning ? 'Updating…' : 'Update' }}<span class="sr-only"> {{ update.deploymentName }} {{ update.service }}</span>
            </button>
          </li>
        }
      </ul>
    }
  `
})
export class Updates {
  private readonly api = inject(UpdatesApi);
  private readonly target = inject(FederationTarget);

  protected readonly resource = polledResource(() => this.api.list(), 2000, this.target.selectionChanges);
  protected readonly checking = signal(false);
  protected readonly actionError = signal('');

  protected check(): void {
    this.checking.set(true);
    this.actionError.set('');
    this.api.check().subscribe({
      next: () => {
        this.checking.set(false);
        this.resource.refresh();
      },
      error: () => {
        this.checking.set(false);
        this.actionError.set('The update check could not be started.');
      }
    });
  }

  protected run(update: DeploymentUpdate): void {
    this.actionError.set('');
    this.api.run(update).subscribe({
      next: () => this.resource.refresh(),
      error: () => this.actionError.set(`Updating ${update.deploymentName} failed.`)
    });
  }
}
