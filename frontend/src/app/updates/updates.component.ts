import { Component } from '@angular/core';
import { ApiService } from '../api/api.service';
import { interval, Observable } from 'rxjs';
import { DeploymentUpdate } from '../api/deployment-update';
import { MatTable, MatColumnDef, MatHeaderRowDef, MatHeaderCellDef, MatCellDef, MatRowDef, MatTableModule } from "@angular/material/table";
import { MatAnchor } from "@angular/material/button";

@Component({
  selector: 'app-updates',
  imports: [MatTableModule, MatTable, MatColumnDef, MatHeaderRowDef, MatHeaderCellDef, MatCellDef, MatAnchor, MatRowDef],
  templateUrl: './updates.component.html'
})
export class UpdatesComponent {
  updates$: Observable<Array<DeploymentUpdate>>;
  isCheckingForUpdates = false;

  constructor(private apiService: ApiService) {
    this.updates$ = apiService.deploymentUpdates$;
    apiService.reloadDeployementUpdates();

    interval(2000).subscribe(() => {
      this.apiService.reloadDeployementUpdates();
    });
  }

  checkForUpdates() {
    this.isCheckingForUpdates = true;
    this.apiService.checkForUpdates().subscribe(() => this.isCheckingForUpdates = false)
  }

  updateDeployment(deployment: DeploymentUpdate) {

    deployment.isRunning = true;

    this.apiService.updateDeployment(deployment)
      .subscribe(() => {
        this.apiService.reloadDeployementUpdates();
      });
  }

}

