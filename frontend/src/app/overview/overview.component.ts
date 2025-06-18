import {Component, inject} from '@angular/core';
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatRow,
  MatRowDef,
  MatTable
} from '@angular/material/table';
import {DeploymentState} from './deploymentState';
import {ApiService} from './api.service';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatIcon} from '@angular/material/icon';
import {MatChip} from '@angular/material/chips';
import {MatDialog} from '@angular/material/dialog';
import {StartDialogComponent} from './start-dialog/start-dialog.component';
import {interval} from 'rxjs';

@Component({
  selector: 'app-overview',
  imports: [
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatCell,
    MatHeaderCellDef,
    MatCellDef,
    MatHeaderRow,
    MatRow,
    MatRowDef,
    MatHeaderRowDef,
    MatMenuTrigger,
    MatIcon,
    MatMenu,
    MatMenuItem,
    MatChip,

  ],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css',
})
export class OverviewComponent {
  dataSource: Array<DeploymentState> = [];
  displayedColumns = ['actions', 'name', 'state', 'message'];

  readonly dialog = inject(MatDialog);

  constructor(private apiService: ApiService) {
    this.apiService.deploymentStates$.subscribe(states => {
      if (JSON.stringify(states) !== JSON.stringify(this.dataSource)) {
        this.dataSource = states;
      }
    });

    this.apiService.updateDeploymentStates();

    interval(2000).subscribe(() => this.apiService.updateDeploymentStates());

  }

  startDeployment(deploymentState: DeploymentState) {
    const dialogRef = this.dialog.open(StartDialogComponent);

    dialogRef.afterClosed().subscribe((result) => {
      if (result.result) {
        this.apiService.startDeployment(deploymentState, result.force)
          .subscribe(() => this.apiService.updateDeploymentStates())
      }
    });
  }

  stopDeployment(element: DeploymentState) {

    this.apiService.stopDeployment(element).subscribe(() => this.apiService.updateDeploymentStates());

  }
}
