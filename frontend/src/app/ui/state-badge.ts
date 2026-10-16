import {ChangeDetectionStrategy, Component, computed, input} from '@angular/core';
import {StateTone, stateTone} from '../core/state-tone';

const TONES: Record<StateTone, {icon: string; classes: string}> = {
  running: {icon: '●', classes: 'bg-emerald-50 text-emerald-800 ring-emerald-600/30 dark:bg-emerald-950 dark:text-emerald-300 dark:ring-emerald-400/30'},
  transitional: {icon: '◐', classes: 'bg-brand-50 text-brand-800 ring-brand-500/30 dark:bg-brand-950 dark:text-brand-300 dark:ring-brand-400/30'},
  stopped: {icon: '■', classes: 'bg-slate-100 text-slate-700 ring-slate-500/30 dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-400/30'},
  failed: {icon: '▲', classes: 'bg-alert-50 text-alert-700 ring-alert-600/30 dark:bg-alert-900 dark:text-alert-300 dark:ring-alert-300/30'}
};

// UX-008: colour, icon, and text together so state is never colour-only.
@Component({
  selector: 'app-state-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset" [class]="tone().classes" data-testid="state-badge">
      <span aria-hidden="true">{{ tone().icon }}</span>{{ state() }}
    </span>
  `
})
export class StateBadge {
  readonly state = input.required<string>();
  protected readonly tone = computed(() => TONES[stateTone(this.state())]);
}
