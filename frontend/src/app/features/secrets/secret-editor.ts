import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {FormsModule} from '@angular/forms';
import {Secret} from '../../core/api/models';

export interface SecretEditorData {
  secret: Secret | null;
  deployments: string[];
}

export type SecretEditorResult =
  | {action: 'save'; secret: Secret; restart: boolean}
  | {action: 'delete'; secret: Secret; restart: boolean};

// UX-003: stored values are never shown; a blank value keeps the stored one.
@Component({
  selector: 'app-secret-editor',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="w-[min(32rem,calc(100vw-2rem))] rounded-xl bg-white p-6 shadow-xl ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700" (ngSubmit)="save()">
      <h2 id="secret-title" class="text-lg font-semibold">{{ existing ? 'Edit secret' : 'New secret' }}</h2>

      <label class="mt-4 flex flex-col gap-1 text-sm font-medium">
        Key
        <input name="key" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal disabled:opacity-60 dark:border-slate-600 dark:bg-slate-950"
               [disabled]="existing" [ngModel]="key()" (ngModelChange)="key.set($event)" required>
      </label>

      <label class="mt-4 flex flex-col gap-1 text-sm font-medium">
        Environment variable
        <input name="env" class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
               [ngModel]="env()" (ngModelChange)="env.set($event)" required>
      </label>

      <label class="mt-4 flex flex-col gap-1 text-sm font-medium">
        Value
        <input name="value" type="password" autocomplete="new-password" aria-describedby="value-hint"
               class="rounded-md border border-slate-300 bg-white px-3 py-2 font-normal dark:border-slate-600 dark:bg-slate-950"
               [ngModel]="value()" (ngModelChange)="value.set($event)">
        <span id="value-hint" class="text-xs font-normal text-slate-500 dark:text-slate-400" data-testid="value-hint">
          {{ existing ? 'Leave blank to keep the stored value.' : 'Required for a new secret.' }}
        </span>
      </label>

      <fieldset class="mt-4">
        <legend class="text-sm font-medium">Deployments</legend>
        <div class="mt-1 flex flex-col gap-1">
          @for (name of data.deployments; track name) {
            <label class="flex items-center gap-2 text-sm">
              <input type="checkbox" class="size-4 accent-brand-600" [name]="'dep-' + name" [checked]="selected().has(name)" (change)="toggle(name)">
              {{ name }}
            </label>
          }
        </div>
      </fieldset>

      <label class="mt-4 flex items-center gap-2 text-sm">
        <input type="checkbox" class="size-4 accent-brand-600" name="restart" [ngModel]="restart()" (ngModelChange)="restart.set($event)">
        Restart associated deployments
      </label>

      <div class="mt-6 flex flex-wrap items-center justify-between gap-2">
        <div>
          @if (existing) {
            <button type="button" class="rounded-md px-3 py-2 text-sm font-medium text-alert-700 ring-1 ring-alert-500 hover:bg-alert-50 dark:text-alert-300 dark:hover:bg-alert-900" data-testid="delete" (click)="remove()">
              {{ confirmingDelete() ? 'Click again to delete' : 'Delete' }}
            </button>
          }
        </div>
        <div class="flex gap-2">
          <button type="button" class="rounded-md px-3 py-2 text-sm font-medium ring-1 ring-slate-300 hover:bg-slate-100 dark:ring-slate-600 dark:hover:bg-slate-800" (click)="ref.close()">Cancel</button>
          <button type="submit" class="rounded-md bg-brand-600 px-3 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60" [disabled]="!valid()">Save</button>
        </div>
      </div>
    </form>
  `
})
export class SecretEditor {
  protected readonly data = inject<SecretEditorData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<SecretEditorResult>>(DialogRef);
  protected readonly existing = this.data.secret?.key != null;

  protected readonly key = signal(this.data.secret?.key ?? '');
  protected readonly env = signal(this.data.secret?.environmentVariable ?? '');
  protected readonly value = signal('');
  protected readonly selected = signal(new Set(this.data.secret?.deployments ?? []));
  protected readonly restart = signal(false);
  protected readonly confirmingDelete = signal(false);

  protected readonly valid = computed(() =>
    this.key().trim() !== '' && this.env().trim() !== '' && (this.existing || this.value() !== ''));

  protected toggle(name: string): void {
    const next = new Set(this.selected());
    if (!next.delete(name)) {
      next.add(name);
    }
    this.selected.set(next);
  }

  protected save(): void {
    if (!this.valid()) {
      return;
    }
    this.ref.close({
      action: 'save',
      restart: this.restart(),
      secret: {
        key: this.key().trim(),
        environmentVariable: this.env().trim(),
        deployments: [...this.selected()],
        value: this.value() === '' ? null : this.value()
      }
    });
  }

  protected remove(): void {
    if (!this.confirmingDelete()) {
      this.confirmingDelete.set(true);
      return;
    }
    this.ref.close({action: 'delete', restart: this.restart(), secret: this.data.secret!});
  }
}
