import {Component} from '@angular/core';
import {MatTab, MatTabGroup} from '@angular/material/tabs';
import {DeploymentsComponent} from './deployments/deployments.component';
import {SecretsComponent} from './secrets/secrets.component';

@Component({
  selector: 'app-root',
  imports: [MatTabGroup, MatTab, DeploymentsComponent, SecretsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'dirigent-frontend';
}
