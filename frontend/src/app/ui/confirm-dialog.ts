import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {DIALOG_DATA, DialogRef} from '@angular/cdk/dialog';
import {FormsModule} from '@angular/forms';

export interface ConfirmData {
  title: string;
  message: string;
  confirmLabel: string;
  danger?: boolean;
  optionLabel?: string;
}

export interface ConfirmResult {
  option: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="w-[min(28rem,calc(100vw-2rem))] rounded-xl bg-white p-6 shadow-xl ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-700">
      <h2 id="confirm-title" class="text-lg font-semibold">{{ data.title }}</h2>
      <p class="mt-2 text-sm text-slate-600 dark:text-slate-300">{{ data.message }}</p>
      @if (data.optionLabel) {
        <label class="mt-4 flex items-center gap-2 text-sm">
          <input type="checkbox" class="size-4 accent-brand-600" name="option" [ngModel]="option()" (ngModelChange)="option.set($event)">
          {{ data.optionLabel }}
        </label>
      }
      <div class="mt-6 flex justify-end gap-2">
        <button type="button" class="rounded-md px-3 py-2 text-sm font-medium text-slate-700 ring-1 ring-slate-300 hover:bg-slate-100 dark:text-slate-200 dark:ring-slate-600 dark:hover:bg-slate-800" (click)="ref.close()">Cancel</button>
        <button type="button" cdkFocusInitial class="rounded-md px-3 py-2 text-sm font-medium text-white"
                [class]="data.danger ? 'bg-alert-600 hover:bg-alert-700' : 'bg-brand-600 hover:bg-brand-700'"
                (click)="ref.close({option: option()})">{{ data.confirmLabel }}</button>
      </div>
    </div>
  `
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<ConfirmResult>>(DialogRef);
  protected readonly option = signal(false);
}
