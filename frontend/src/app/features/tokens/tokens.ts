import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {CreatedMachineToken, MachineToken} from '../../core/api/models';
import {TokensApi} from '../../core/api/tokens-api';
import {FederationDirectory} from '../../core/federation/federation-directory';
import {FederationTarget} from '../../core/federation/federation-target';

const PERMISSIONS = [
  {value: 'deployments:read', label: 'Read deployments'},
  {value: 'deployments:write', label: 'Start and stop deployments'},
  {value: 'secrets:read', label: 'Read secret metadata'},
  {value: 'secrets:write', label: 'Manage secrets'},
  {value: 'updates:read', label: 'Read image updates'},
  {value: 'updates:write', label: 'Run image updates'},
  {value: 'system:read', label: 'Read system information'},
  {value: 'webhooks:trigger', label: 'Trigger repository webhooks'}
];

/** UX-006: a calendar date means 00:00 UTC of that day. */
export function expiryToIso(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day)).toISOString();
}

export function tokenStatus(token: MachineToken, now = Date.now()): string {
  if (token.revokedAt) {
    return 'Revoked';
  }
  if (!token.expiresAt) {
    return 'Inactive: no expiry';
  }
  return new Date(token.expiresAt).getTime() <= now ? 'Expired' : 'Active';
}

@Component({
  selector: 'app-tokens',
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1 class="text-2xl font-semibold">Machine tokens</h1>
    <p class="mt-1 text-sm text-slate-600 dark:text-slate-300">Machine tokens are managed only on the main instance, the one you are signed in to. They are never managed through federation.</p>
    @if (target.selectedId() !== 'local') {
      <p role="note" class="mt-3 rounded-md bg-brand-50 px-3 py-2 text-sm text-slate-900 ring-1 ring-accent-400 dark:bg-brand-950 dark:text-slate-100" data-testid="remote-notice">
        The selected instance “{{ selectedName() }}” does not apply here. The tokens below belong to the main instance. To manage tokens of “{{ selectedName() }}”, open its own dashboard or use the server-side CLI on that instance.
      </p>
    }

    @if (issued(); as token) {
      <section class="mt-4 rounded-lg border-2 border-brand-500 bg-brand-50 p-4 dark:border-accent-400 dark:bg-brand-950" aria-labelledby="issued-heading" data-testid="issued">
        <h2 id="issued-heading" class="font-semibold">Token “{{ token.name }}” created</h2>
        <p class="mt-1 text-sm">Copy it now. It is shown only once.</p>
        <code class="mt-2 block break-all rounded bg-white p-2 text-sm dark:bg-slate-900" data-testid="issued-token">{{ token.token }}</code>
        <div class="mt-3 flex gap-2">
          <button type="button" class="rounded-md bg-brand-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-brand-700" (click)="copy()">Copy</button>
          <button type="button" class="rounded-md px-3 py-1.5 text-sm ring-1 ring-slate-300 dark:ring-slate-600" (click)="issued.set(null)">Dismiss</button>
        </div>
      </section>
    }

    @if (error()) {
      <p role="alert" class="mt-3 rounded-md bg-alert-50 px-3 py-2 text-sm text-alert-700 dark:bg-alert-900 dark:text-alert-300">{{ error() }}</p>
    }

    <form class="mt-6 rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700" (ngSubmit)="create()">
      <h2 class="text-lg font-semibold">Create token</h2>
      <div class="mt-3 flex flex-wrap gap-4">
        <label class="flex flex-col gap-1 text-sm font-medium">
          Name
          <input name="name" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
                 [ngModel]="name()" (ngModelChange)="name.set($event)" required>
        </label>
        <label class="flex flex-col gap-1 text-sm font-medium">
          Expires on
          <input name="expiry" type="date" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
                 [ngModel]="expiry()" (ngModelChange)="expiry.set($event)" required aria-describedby="expiry-hint">
          <span id="expiry-hint" class="text-xs font-normal text-slate-500 dark:text-slate-400">Expires at 00:00 UTC</span>
        </label>
      </div>
      <fieldset class="mt-4">
        <legend class="text-sm font-medium">Permissions</legend>
        <div class="mt-1 grid gap-1 sm:grid-cols-2">
          @for (permission of permissions; track permission.value) {
            <label class="flex items-center gap-2 text-sm">
              <input type="checkbox" class="size-4 accent-brand-600" [name]="permission.value" [checked]="selected().has(permission.value)" (change)="toggle(permission.value)">
              {{ permission.label }}
            </label>
          }
        </div>
      </fieldset>
      <button type="submit" class="mt-4 rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60" [disabled]="!canCreate()">Create token</button>
    </form>

    <div class="mt-6 overflow-x-auto">
      <table class="w-full text-left text-sm">
        <caption class="sr-only">Machine tokens</caption>
        <thead>
          <tr class="border-b border-slate-200 dark:border-slate-700">
            <th scope="col" class="py-2 pr-4 font-semibold">Name</th>
            <th scope="col" class="py-2 pr-4 font-semibold">Permissions</th>
            <th scope="col" class="py-2 pr-4 font-semibold">Expires (UTC)</th>
            <th scope="col" class="py-2 pr-4 font-semibold">Status</th>
            <th scope="col" class="py-2"><span class="sr-only">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          @for (token of tokens(); track token.id) {
            <tr class="border-b border-slate-100 align-top dark:border-slate-800" data-testid="token">
              <td class="py-2 pr-4 font-medium">{{ token.name }}</td>
              <td class="py-2 pr-4">
                <ul class="flex flex-wrap gap-1">
                  @for (permission of token.permissions; track permission) {
                    <li class="rounded bg-slate-100 px-1.5 py-0.5 text-xs dark:bg-slate-800">{{ permission }}</li>
                  }
                </ul>
              </td>
              <td class="py-2 pr-4">{{ token.expiresAt ? (token.expiresAt | date: 'mediumDate' : 'UTC') : 'Never' }}</td>
              <td class="py-2 pr-4" data-testid="status">{{ status(token) }}</td>
              <td class="py-2">
                @if (!token.revokedAt) {
                  <button type="button" class="rounded-md px-3 py-1.5 text-sm text-alert-700 ring-1 ring-alert-500 hover:bg-alert-50 dark:text-alert-300 dark:hover:bg-alert-900" (click)="revoke(token)">
                    Revoke<span class="sr-only"> {{ token.name }}</span>
                  </button>
                }
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `
})
export class Tokens {
  private readonly api = inject(TokensApi);
  private readonly directory = inject(FederationDirectory);
  protected readonly target = inject(FederationTarget);
  protected readonly selectedName = computed(() => {
    const id = this.target.selectedId();
    return this.directory.instances().find(instance => instance.id === id)?.instanceName ?? id;
  });

  protected readonly permissions = PERMISSIONS;
  protected readonly tokens = signal<MachineToken[]>([]);
  protected readonly issued = signal<CreatedMachineToken | null>(null);
  protected readonly error = signal('');
  protected readonly busy = signal(false);

  protected readonly name = signal('');
  protected readonly expiry = signal('');
  protected readonly selected = signal(new Set<string>());

  protected readonly canCreate = computed(() =>
    !this.busy() && this.name().trim() !== '' && this.expiry() !== '' && this.selected().size > 0);

  constructor() {
    this.reload();
  }

  protected status(token: MachineToken): string {
    return tokenStatus(token);
  }

  protected toggle(permission: string): void {
    const next = new Set(this.selected());
    if (!next.delete(permission)) {
      next.add(permission);
    }
    this.selected.set(next);
  }

  protected create(): void {
    if (!this.canCreate()) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.api.create(this.name().trim(), [...this.selected()], expiryToIso(this.expiry())).subscribe({
      next: token => {
        this.issued.set(token);
        this.name.set('');
        this.expiry.set('');
        this.selected.set(new Set());
        this.busy.set(false);
        this.reload();
      },
      error: () => {
        this.busy.set(false);
        this.error.set('The token could not be created. Check its name, permissions, and expiry.');
      }
    });
  }

  protected revoke(token: MachineToken): void {
    this.error.set('');
    this.api.revoke(token.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('The token could not be revoked.')
    });
  }

  protected copy(): void {
    const token = this.issued();
    if (token) {
      void navigator.clipboard.writeText(token.token);
    }
  }

  private reload(): void {
    this.api.list().subscribe({
      next: tokens => this.tokens.set(tokens),
      error: () => this.error.set('Token information could not be loaded.')
    });
  }
}
