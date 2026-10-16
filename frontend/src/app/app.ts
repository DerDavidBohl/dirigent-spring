import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ThemeService} from './core/theme/theme.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<router-outlet />'
})
export class App {
  // Instantiated eagerly so the stored theme is applied before the first view renders.
  protected readonly theme = inject(ThemeService);
}
