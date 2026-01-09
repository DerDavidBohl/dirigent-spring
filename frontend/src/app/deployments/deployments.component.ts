import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatAnchor, MatButton, MatIconButton } from '@angular/material/button';
import { MatChip, MatChipListbox, MatChipListboxChange, MatChipOption } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatInput, MatLabel } from '@angular/material/input';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { MatSort, MatSortHeader, Sort } from '@angular/material/sort';
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
import { interval, Observable, ReplaySubject } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ApiService } from '../api/api.service';
import { DeploymentState } from '../api/deployment-state';
import { SystemInformation } from '../api/system-information';
import { StartDialogComponent } from './start-dialog/start-dialog.component';
import { MatIcon } from "@angular/material/icon";

@Component({
  selector: 'app-deployments',
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
    MatMenu,
    MatMenuItem,
    MatChip,
    MatChipListbox,
    MatChipOption,
    AsyncPipe,
    FormsModule,
    MatSortHeader,
    MatSort,
    MatLabel,
    MatInput,
    MatFormField,
    MatButton,
    MatAnchor,
    MatMenuTrigger,
    MatIcon,
    MatIconButton
],
  templateUrl: './deployments.component.html',
  styleUrl: './deployments.component.css',
})
export class DeploymentsComponent implements OnInit {

  selectedFilterValues$ = new ReplaySubject<Array<string>>(1);
  sort$ = new ReplaySubject<Sort>(1);
  search$ = new ReplaySubject<string>(1);

  deploymentStates$: Observable<Array<DeploymentState>>;
  tableDataSource$: Observable<Array<DeploymentState>>;
  filterValues$: Observable<string[]>;
  systemInformation$: Observable<SystemInformation>;

  displayedColumns = ['actions', 'name', 'state', 'message'];

  readonly dialog = inject(MatDialog);

  constructor(private apiService: ApiService) {

    this.deploymentStates$ = this.apiService.deploymentStates$;    

    this.systemInformation$ = apiService.getSystemInformation();

    this.systemInformation$.subscribe(i => document.title = `Dirigent@${i.instanceName}`)

    this.filterValues$ = this.deploymentStates$.pipe(map(ds => [...new Set(ds.map(ds => ds.state))]));

    this.tableDataSource$ = this.selectedFilterValues$.pipe(
      switchMap(selectedFilterValues => this.deploymentStates$.pipe(
        map(ds => ds.filter(ds => selectedFilterValues.includes(ds.state))),
      )),
      switchMap(ds => this.search$.pipe(
        map(search => ds.filter(ds => ds.name.toLowerCase().includes(search.toLowerCase())))
      )),
      switchMap(ds => this.sort$.pipe(

        map(sort => {
          if (!sort.active) return ds;

          return [...ds].sort((a, b) => {
            // @ts-ignore
            const valueA = a[sort.active];
            // @ts-ignore
            const valueB = b[sort.active];

            if (valueA == null || valueB == null) return 0;

            const comparison = String(valueA).localeCompare(String(valueB));
            return sort.direction === 'desc' ? -comparison : comparison;
          });
        })


      ))
    );
    this.apiService.reloadDeploymentStates();

    interval(2000).subscribe(() => {
      this.apiService.reloadDeploymentStates();
    });

  }

  ngOnInit(): void {
    this.selectedFilterValues$.next(['RUNNING', 'STOPPED', 'FAILED', 'UPDATED', 'UNKNOWN', 'STARTING', 'STOPPING']);
    this.sort$.next({ active: 'name', direction: 'asc' });
    this.search$.next('');
  }

  startDeployment(deploymentState: DeploymentState) {
    const dialogRef = this.dialog.open(StartDialogComponent);

    dialogRef.afterClosed().subscribe((result) => {
      if (result.result) {
        this.apiService.startDeployment(deploymentState, result.force)
          .subscribe(() => this.apiService.reloadDeploymentStates())
      }
    });
  }

  stopDeployment(element: DeploymentState) {

    this.apiService.stopDeployment(element).subscribe(() => this.apiService.reloadDeploymentStates());

  }

  getBackgroudColorForState(state: string): string {

    if (state.toUpperCase() === 'RUNNING') return 'lightgreen';
    if (state.toUpperCase() === 'FAILED') return 'lightred';
    if (state.toUpperCase() === 'REMOVED') return 'lightgrey';
    if (state.toUpperCase() === 'STOPPED') return 'grey';

    return '';
  }

  updateFilter(event: MatChipListboxChange) {
    this.selectedFilterValues$.next(event.value);
  }

  announceSortChange($event: Sort) {
    this.sort$.next($event);
  }

  search(event: KeyboardEvent) {
    // @ts-ignore
    this.search$.next(event.target.value);

  }

  countDeploymentsByState(state: string): Observable<number> {
    return this.deploymentStates$.pipe(
      map(deployments => deployments.filter(deployment => deployment.state === state).length)
    );
  }
}
