import { Component } from '@angular/core';
import { ApiService } from '../api/api.service';
import { Observable } from 'rxjs';
import { DeploymentUpdate } from '../api/deployment-update';
import { MatTable, MatColumnDef, MatHeaderRowDef, MatHeaderCellDef, MatCellDef, MatRowDef, MatTableModule } from "@angular/material/table";
import { MatAnchor } from "@angular/material/button";
import { DeploymentUpdateServiceImage } from '../api/deployment-update-service-image';
import { DeploymentState } from '../api/deployment-state';

@Component({
  selector: 'app-updates',
  imports: [MatTableModule, MatTable, MatColumnDef, MatHeaderRowDef, MatHeaderCellDef, MatCellDef, MatAnchor, MatRowDef],
  templateUrl: './updates.component.html'
})
export class UpdatesComponent {
  updates$: Observable<Array<DeploymentUpdate>>;
  isCheckingForUpdates = false;
  currentlyUpdatingDeploymentNames: Array<string> = [];

  constructor(private apiService: ApiService) {
    this.updates$ = apiService.deploymentUpdates$;
    apiService.reloadDeployementUpdates();
  }

  createServiceUpdateString(serviceUpdates: Array<DeploymentUpdateServiceImage>): string {
    return serviceUpdates.map(su => `${su.image}: ${su.service}`).join(',');
  }

  checkForUpdates() {
    this.isCheckingForUpdates = true;
    this.apiService.checkForUpdates().subscribe(() => this.isCheckingForUpdates = false)
  }

  updateDeployment(deploymentName: string) {
    this.currentlyUpdatingDeploymentNames.push(deploymentName);
    this.apiService.updateDeployment(deploymentName)
      .subscribe(() => {
        this.currentlyUpdatingDeploymentNames = this.currentlyUpdatingDeploymentNames.filter(s => s !== deploymentName)
        this.apiService.reloadDeployementUpdates();
      });
  }

  isCurrentlyUpdating(deploymentName: string): boolean {
    return this.currentlyUpdatingDeploymentNames.includes(deploymentName);
  }

}

