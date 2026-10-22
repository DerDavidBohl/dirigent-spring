import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {Router} from '@angular/router';
import {FederationDirectory} from '../../core/federation/federation-directory';

@Component({
  selector: 'app-instances',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-semibold">Instances</h1>
      <button type="button" class="rounded-md px-3 py-1.5 text-sm ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800"
              [disabled]="directory.loading()" (click)="directory.refresh()">{{ directory.loading() ? 'Refreshing…' : 'Refresh' }}</button>
    </div>
    @if (directory.error()) {
      <p role="alert" class="mt-4 text-sm text-alert-700 dark:text-alert-300">Instance information could not be loaded.</p>
    }
    <ul class="mt-5 divide-y divide-slate-200 dark:divide-slate-700">
      @for (instance of directory.instances(); track instance.id) {
        <li class="py-5">
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 class="text-lg font-semibold">{{ instance.instanceName }}</h2>
              <p class="text-sm" [class]="instance.available ? 'text-slate-600 dark:text-slate-300' : 'text-alert-700 dark:text-alert-300'">
                {{ instance.available ? 'Connected' : instance.error }}
              </p>
            </div>
            <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700"
                  [disabled]="!instance.available" (click)="useInstance(instance.id)">Manage this instance</button>
          </div>
        </li>
      } @empty {
        @if (!directory.loading() && !directory.error()) {
          <li class="py-4 text-sm text-slate-600 dark:text-slate-300">No instances are configured.</li>
        }
      }
    </ul>
  `
})
export class Instances {
  protected readonly directory = inject(FederationDirectory);
  private readonly router = inject(Router);

  protected useInstance(id: string): void {
    void this.router.navigate(['/'], {queryParams: {instance: id}});
  }
}