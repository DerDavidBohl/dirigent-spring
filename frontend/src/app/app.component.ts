import {Component, OnInit, signal} from '@angular/core';
import {MatTab, MatTabGroup} from '@angular/material/tabs';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {DeploymentsComponent} from './deployments/deployments.component';
import {SecretsComponent} from './secrets/secrets.component';
import {ThemeService} from './theme.service';
import { UpdatesComponent } from "./updates/updates.component";
import {FormsModule} from '@angular/forms';
import {AuthService} from './api/auth.service';
import {MachineTokensComponent} from './auth/machine-tokens.component';

@Component({
  selector: 'app-root',
  host: {class: 'block min-h-screen'},
  imports: [
    MatTabGroup,
    MatTab,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    DeploymentsComponent,
    SecretsComponent,
    UpdatesComponent,
    MachineTokensComponent
],
  templateUrl: './app.component.html',
})
export class AppComponent implements OnInit {
  checkingAuthentication = signal(true);
  loginPending = signal(false);
  username = '';
  password = '';
  loginError = signal('');

  constructor(public themeService: ThemeService, public authService: AuthService) {}

  ngOnInit(): void {
    this.refreshAuthentication();
  }

  login(): void {
    this.loginPending.set(true);
    this.loginError.set('');
    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.password = '';
        this.refreshAuthentication();
      },
      error: () => {
        this.loginError.set('Invalid username or password.');
        this.loginPending.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.refreshAuthentication(),
      error: () => this.refreshAuthentication()
    });
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  private refreshAuthentication(): void {
    this.checkingAuthentication.set(true);
    this.authService.initialize().subscribe({
      next: () => {
        this.checkingAuthentication.set(false);
        this.loginPending.set(false);
      },
      error: () => {
        this.checkingAuthentication.set(false);
        this.loginPending.set(false);
        this.authService.user.set(null);
      }
    });
  }
}

