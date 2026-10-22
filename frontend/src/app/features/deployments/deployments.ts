import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {NgTemplateOutlet} from '@angular/common';
import {ActivatedRoute, Router} from '@angular/router';
import {filter, map, takeUntil} from 'rxjs';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {DeploymentState} from '../../core/api/models';
import {polledResource} from '../../core/polled-resource';
import {DEPLOYMENT_STATES} from '../../core/state-tone';
import {ConfirmService} from '../../ui/confirm.service';
import {StateBadge} from '../../ui/state-badge';
import {StatusLine} from '../../ui/status-line';
import {FederationTarget} from '../../core/federation/federation-target';

type SortKey = 'name' | 'state' | 'source';

const SORT_KEYS: readonly SortKey[] = ['name', 'state', 'source'];

// UX-001, UX-002: search, filter and sort live in the URL; Start and Stop both require a dialog.
@Component({
  selector: 'app-deployments',
  imports: [NgTemplateOutlet, StateBadge, StatusLine],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Deployments</h1>

    <div class="mt-4 flex flex-wrap items-end gap-3">
      <label class="flex flex-col gap-1 text-sm font-medium">
        Search by name
        <input type="search" class="w-64 max-w-full rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-900"
               [value]="query()" (input)="update({q: $any($event.target).value})">
      </label>
      <label class="flex flex-col gap-1 text-sm font-medium">
        State
        <select class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-900"
                [value]="stateFilter()" (change)="update({state: $any($event.target).value})">
          <option value="">All states</option>
          @for (state of states; track state) {
            <option [value]="state" [selected]="state === stateFilter()">{{ state }}</option>
          }
        </select>
      </label>
    </div>
    <app-status-line class="mt-2 block" [loading]="resource.loading()" [error]="resource.error()" [lastUpdated]="resource.lastUpdated()" />

    @if (actionError()) {
      <p role="alert" class="mt-3 rounded-md bg-alert-50 px-3 py-2 text-sm text-alert-700 dark:bg-alert-900 dark:text-alert-300">{{ actionError() }}</p>
    }

    <ng-template #actions let-item>
      <div class="flex gap-2">
        <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700" (click)="start(item)">
          Start<span class="sr-only"> {{ item.name }}</span>
        </button>
        <button type="button" class="rounded-md px-3 py-1.5 text-sm font-medium text-alert-700 ring-1 ring-alert-500 hover:bg-alert-50 dark:text-alert-300 dark:hover:bg-alert-900" (click)="stop(item)">
          Stop<span class="sr-only"> {{ item.name }}</span>
        </button>
      </div>
    </ng-template>

    <ng-template #source let-item>
      @if (isLink(item.source)) {
        <a [href]="item.source" target="_blank" rel="noopener noreferrer" class="break-all text-brand-700 underline dark:text-accent-300">{{ item.source }}</a>
      } @else {
        <span class="break-all">{{ item.source }}</span>
      }
    </ng-template>

    @if (visible().length === 0 && !resource.loading()) {
      <p class="mt-6 text-sm text-slate-600 dark:text-slate-300" data-testid="empty">No deployments match.</p>
    }

    <table class="mt-4 hidden w-full text-left text-sm md:table">
      <thead>
        <tr class="border-b border-slate-200 dark:border-slate-700">
          @for (column of columns; track column.key) {
            <th scope="col" class="py-2 pr-4" [attr.aria-sort]="ariaSort(column.key)">
              <button type="button" class="font-semibold" [attr.data-testid]="'sort-' + column.key" (click)="sortBy(column.key)">
                {{ column.label }}{{ sortKey() === column.key ? (descending() ? ' ▼' : ' ▲') : '' }}
              </button>
            </th>
          }
          <th scope="col" class="py-2 pr-4 font-semibold">Message</th>
          <th scope="col" class="py-2 font-semibold"><span class="sr-only">Actions</span></th>
        </tr>
      </thead>
      <tbody>
        @for (item of visible(); track item.name) {
          <tr class="border-b border-slate-100 align-top dark:border-slate-800" data-testid="row">
            <td class="py-2 pr-4 font-medium">{{ item.name }}</td>
            <td class="py-2 pr-4"><app-state-badge [state]="item.state" /></td>
            <td class="py-2 pr-4"><ng-container *ngTemplateOutlet="source; context: {$implicit: item}" /></td>
            <td class="max-w-md whitespace-pre-wrap break-words py-2 pr-4 text-slate-600 dark:text-slate-300">{{ item.message }}</td>
            <td class="py-2"><ng-container *ngTemplateOutlet="actions; context: {$implicit: item}" /></td>
          </tr>
        }
      </tbody>
    </table>

    <ul class="mt-4 flex flex-col gap-3 md:hidden">
      @for (item of visible(); track item.name) {
        <li class="rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700" data-testid="card">
          <p class="flex items-center justify-between gap-2 font-medium">{{ item.name }} <app-state-badge [state]="item.state" /></p>
          <p class="mt-1 text-sm"><ng-container *ngTemplateOutlet="source; context: {$implicit: item}" /></p>
          <p class="mt-1 whitespace-pre-wrap break-words text-sm text-slate-600 dark:text-slate-300">{{ item.message }}</p>
          <div class="mt-3"><ng-container *ngTemplateOutlet="actions; context: {$implicit: item}" /></div>
        </li>
      }
    </ul>
  `
})
export class Deployments {
  private readonly api = inject(DeploymentsApi);
  private readonly confirmDialog = inject(ConfirmService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly target = inject(FederationTarget);

  protected readonly resource = polledResource(() => this.api.list(), 2000, this.target.selectionChanges);
  protected readonly states = DEPLOYMENT_STATES;
  protected readonly columns: {key: SortKey; label: string}[] = [
    {key: 'name', label: 'Name'},
    {key: 'state', label: 'State'},
    {key: 'source', label: 'Source'}
  ];

  private readonly params = toSignal(this.route.queryParamMap, {requireSync: true});
  protected readonly query = computed(() => this.params().get('q') ?? '');
  protected readonly stateFilter = computed(() => this.params().get('state') ?? '');
  protected readonly sortKey = computed<SortKey>(() => {
    const key = this.params().get('sort') as SortKey | null;
    return key && SORT_KEYS.includes(key) ? key : 'name';
  });
  protected readonly descending = computed(() => this.params().get('dir') === 'desc');

  protected readonly actionError = signal('');

  protected readonly visible = computed(() => {
    const query = this.query().trim().toLowerCase();
    const state = this.stateFilter();
    const key = this.sortKey();
    const direction = this.descending() ? -1 : 1;
    return (this.resource.data() ?? [])
      .filter(d => (!query || d.name.toLowerCase().includes(query)) && (!state || d.state === state))
      .sort((a, b) => direction * a[key].localeCompare(b[key]));
  });

  protected isLink(source: string): boolean {
    return /^https?:\/\//i.test(source);
  }

  protected ariaSort(key: SortKey): 'ascending' | 'descending' | 'none' {
    if (this.sortKey() !== key) {
      return 'none';
    }
    return this.descending() ? 'descending' : 'ascending';
  }

  protected update(params: Record<string, string | null>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: Object.fromEntries(Object.entries(params).map(([k, v]) => [k, v || null])),
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  protected sortBy(key: SortKey): void {
    const flip = this.sortKey() === key && !this.descending();
    this.update({sort: key, dir: flip ? 'desc' : null});
  }

  protected start(item: DeploymentState): void {
    this.confirmDialog.confirm({
      title: `Start ${item.name}`,
      message: 'Start this deployment now?',
      confirmLabel: 'Start',
      optionLabel: 'Force recreate containers'
    }).pipe(takeUntil(this.target.selectionChanges), filter(result => !!result), map(result => result!.option)).subscribe(force =>
      this.run(this.api.start(item.name, force), `Starting ${item.name} failed.`));
  }

  protected stop(item: DeploymentState): void {
    this.confirmDialog.confirm({
      title: `Stop ${item.name}`,
      message: 'Stop this deployment and its containers?',
      confirmLabel: 'Stop',
      danger: true
    }).pipe(takeUntil(this.target.selectionChanges), filter(result => !!result)).subscribe(() =>
      this.run(this.api.stop(item.name), `Stopping ${item.name} failed.`));
  }

  private run(request: ReturnType<DeploymentsApi['stop']>, failure: string): void {
    this.actionError.set('');
    request.subscribe({
      next: () => this.resource.refresh(),
      error: () => this.actionError.set(failure)
    });
  }
}
