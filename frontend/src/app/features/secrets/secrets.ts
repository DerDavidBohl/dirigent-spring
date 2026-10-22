import {DestroyRef, ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Dialog} from '@angular/cdk/dialog';
import {DeploymentsApi} from '../../core/api/deployments-api';
import {Secret} from '../../core/api/models';
import {SecretsApi} from '../../core/api/secrets-api';
import {FederationTarget} from '../../core/federation/federation-target';
import {SecretEditor, SecretEditorResult} from './secret-editor';

// UX-003: list responses never contain stored values.
@Component({
  selector: 'app-secrets',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-wrap items-center justify-between gap-3">
      <h1 class="text-2xl font-semibold">Secrets</h1>
      <button type="button" class="rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700" data-testid="new" (click)="edit(null)">New secret</button>
    </div>

    @if (error()) {
      <p role="alert" class="mt-3 rounded-md bg-alert-50 px-3 py-2 text-sm text-alert-700 dark:bg-alert-900 dark:text-alert-300">{{ error() }}</p>
    }

    @if (loading()) {
      <p role="status" class="mt-4 text-sm text-slate-500">Loading…</p>
    } @else if (secrets().length === 0) {
      <p class="mt-6 text-sm text-slate-600 dark:text-slate-300" data-testid="empty">No secrets defined.</p>
    } @else {
      <ul class="mt-4 flex flex-col gap-3">
        @for (secret of secrets(); track secret.key) {
          <li class="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-white p-4 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700" data-testid="secret">
            <div class="min-w-0">
              <p class="font-medium">{{ secret.key }}</p>
              <p class="text-sm text-slate-600 dark:text-slate-300">
                <code>{{ secret.environmentVariable }}</code>
                @if (secret.deployments.length) { in {{ secret.deployments.join(', ') }} } @else { (no deployments) }
              </p>
            </div>
            <button type="button" class="rounded-md px-3 py-1.5 text-sm font-medium ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800" (click)="edit(secret)">
              Edit<span class="sr-only"> {{ secret.key }}</span>
            </button>
          </li>
        }
      </ul>
    }
  `
})
export class Secrets {
  private readonly secretsApi = inject(SecretsApi);
  private readonly deploymentsApi = inject(DeploymentsApi);
  private readonly dialog = inject(Dialog);
  private readonly target = inject(FederationTarget);

  protected readonly secrets = signal<Secret[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal('');
  private loadGeneration = 0;

  constructor() {
    this.reload();
    this.target.selectionChanges.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => {
      this.loadGeneration++;
      this.secrets.set([]);
      this.loading.set(true);
      this.error.set('');
      this.reload();
    });
  }

  protected edit(secret: Secret | null): void {
    const selectedId = this.target.selectedId();
    this.deploymentsApi.list().subscribe({
      next: deployments => {
        if (selectedId === this.target.selectedId()) {
          this.openEditor(secret, deployments.map(d => d.name), selectedId);
        }
      },
      error: () => this.error.set('Deployments could not be loaded.')
    });
  }

  private openEditor(secret: Secret | null, deployments: string[], selectedId: string): void {
    this.dialog.open<SecretEditorResult | undefined>(SecretEditor, {
      data: {secret, deployments},
      ariaLabelledBy: 'secret-title'
    }).closed.subscribe(result => {
      if (!result || selectedId !== this.target.selectedId()) {
        return;
      }
      const request = result.action === 'save'
        ? this.secretsApi.put(result.secret, result.restart)
        : this.secretsApi.remove(result.secret, result.restart);
      this.error.set('');
      request.subscribe({
        next: () => this.reload(),
        error: () => this.error.set(result.action === 'save' ? 'The secret could not be saved.' : 'The secret could not be deleted.')
      });
    });
  }

  private reload(): void {
    const generation = this.loadGeneration;
    this.secretsApi.list().subscribe({
      next: secrets => {
        if (generation !== this.loadGeneration) {
          return;
        }
        this.secrets.set(secrets);
        this.loading.set(false);
      },
      error: () => {
        if (generation !== this.loadGeneration) {
          return;
        }
        this.loading.set(false);
        this.error.set('Secrets could not be loaded.');
      }
    });
  }
}
