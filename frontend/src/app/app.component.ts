import {Component} from '@angular/core';
import {MatTab, MatTabGroup} from '@angular/material/tabs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {DeploymentsComponent} from './deployments/deployments.component';
import {SecretsComponent} from './secrets/secrets.component';
import {ThemeService} from './theme.service';
import { UpdatesComponent } from "./updates/updates.component";

@Component({
  selector: 'app-root',
  imports: [
    MatTabGroup,
    MatTab,
    MatButtonModule,
    MatIconModule,
    DeploymentsComponent,
    SecretsComponent,
    UpdatesComponent
],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {

  constructor(public themeService: ThemeService) {}

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}

