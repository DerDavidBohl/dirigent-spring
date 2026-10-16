import {ChangeDetectionStrategy, Component, input} from '@angular/core';
import {DatePipe} from '@angular/common';

// UX-005: every polled view states whether it is loading, failed to refresh, or current.
@Component({
  selector: 'app-status-line',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="text-xs text-slate-500 dark:text-slate-400" role="status" aria-live="polite">
      @if (error()) {
        <span class="font-medium text-alert-700 dark:text-alert-300">Could not refresh; showing the last known data.</span>
      } @else if (loading()) {
        Loading…
      }
      @if (lastUpdated(); as updated) {
        Updated {{ updated | date: 'mediumTime' }}
      }
    </p>
  `
})
export class StatusLine {
  readonly loading = input(false);
  readonly error = input(false);
  readonly lastUpdated = input<Date | null>(null);
}
