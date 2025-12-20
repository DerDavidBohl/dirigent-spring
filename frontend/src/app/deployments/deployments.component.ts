import { AsyncPipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatBadge } from '@angular/material/badge';
import { MatIconButton, MatButton, MatAnchor } from '@angular/material/button';
import { MatChip, MatChipListbox, MatChipListboxChange, MatChipOption } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
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
import { combineLatest, interval, Observable, ReplaySubject } from 'rxjs';
import { distinctUntilChanged, map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { ApiService } from '../api/api.service';
import { DeploymentState } from '../api/deployment-state';
import { DeploymentUpdate } from '../api/deployment-update';
import { SystemInformation } from '../api/system-information';
import { StartDialogComponent } from './start-dialog/start-dialog.component';

@Component({
  selector: 'app-deployments',
  imports: [
    MatBadge,
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
    MatIconButton,
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
    MatAnchor
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
  deploymentUpdates$: Observable<Array<DeploymentUpdate>>;
  statesAndUpdates$: Observable<Array<DeploymentState &  {updates: Array<DeploymentUpdate>}>>;

  displayedColumns = ['actions', 'name', 'state', 'message'];
  isCheckingForUpdates = false;

  readonly dialog = inject(MatDialog);

  constructor(private apiService: ApiService) {

    this.deploymentUpdates$ = this.apiService.deploymentUpdates$;

    this.deploymentStates$ = this.apiService.deploymentStates$;    

    this.systemInformation$ = apiService.getSystemInformation();

    this.systemInformation$.subscribe(i => document.title = `Dirigent@${i.instanceName}`)

    this.statesAndUpdates$ = combineLatest([this.deploymentStates$, this.deploymentUpdates$])
      .pipe(
        distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
        tap(a => console.log('triggered', a)),
        map(([states, updates]) => 
          states.map(s => ({...s, updates: updates.filter(u => u.deploymentName === s.name)})
      )));

    this.filterValues$ = this.statesAndUpdates$.pipe(map(ds => [...new Set(ds.map(ds => ds.state))]));

    this.tableDataSource$ = this.selectedFilterValues$.pipe(
      switchMap(selectedFilterValues => this.statesAndUpdates$.pipe(
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
      this.apiService.reloadDeployementUpdates();
    });

  }

  ngOnInit(): void {
    this.selectedFilterValues$.next(['RUNNING', 'STOPPED', 'FAILED', 'UPDATED', 'UNKNOWN', 'STARTING', 'STOPPING']);
    this.sort$.next({ active: 'name', direction: 'asc' });
    this.search$.next('');
  }

  checkForUpdates() {
    this.isCheckingForUpdates = true;
    this.apiService.checkForUpdates().subscribe(() => this.isCheckingForUpdates = false)
  }

  updateDeployment(deployment: DeploymentState) {
    this.apiService.updateDeployment(deployment)
      .subscribe(() => this.apiService.reloadDeploymentStates());
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
    console.log(event.target.value);
    // @ts-ignore
    this.search$.next(event.target.value);

  }

  countDeploymentsByState(state: string): Observable<number> {
    return this.deploymentStates$.pipe(
      map(deployments => deployments.filter(deployment => deployment.state === state).length)
    );
  }
}
