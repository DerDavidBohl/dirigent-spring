import {Component, inject, OnInit, ViewChild} from '@angular/core';
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
  MatTable, MatTableDataSource
} from '@angular/material/table';
import {DeploymentState} from './deploymentState';
import {ApiService} from './api.service';
import {MatMenu, MatMenuItem, MatMenuTrigger} from '@angular/material/menu';
import {MatIcon} from '@angular/material/icon';
import {MatChip, MatChipListbox, MatChipListboxChange, MatChipOption} from '@angular/material/chips';
import {MatDialog} from '@angular/material/dialog';
import {StartDialogComponent} from './start-dialog/start-dialog.component';
import {distinctUntilChanged, map, switchMap, tap} from 'rxjs/operators';
import {MatIconButton} from '@angular/material/button';
import {interval, Observable, ReplaySubject} from 'rxjs';
import {AsyncPipe} from '@angular/common';
import {FormsModule, ValueChangeEvent} from '@angular/forms';
import {MatSort, MatSortHeader, Sort} from '@angular/material/sort';

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
    MatIconButton,
    MatChipListbox,
    MatChipOption,
    AsyncPipe,
    FormsModule,
    MatSortHeader,
    MatSort
  ],
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.css',
})
export class OverviewComponent implements OnInit {

  selectedFilterValues$ = new ReplaySubject<Array<string>>(1);
  sort$ = new ReplaySubject<Sort>(1);

  @ViewChild(MatSort) sort!: MatSort;

  dataSource$: Observable<Array<DeploymentState>>;
  tableDataSource$: Observable<Array<DeploymentState>>;
  filterValues$: Observable<string[]>;

  displayedColumns = ['actions', 'name', 'state', 'message'];

  readonly dialog = inject(MatDialog);

  constructor(private apiService: ApiService) {
    this.dataSource$ = this.apiService.deploymentStates$.pipe(
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr))
    );
    this.filterValues$ = this.dataSource$.pipe(map(ds => [...new Set(ds.map(ds => ds.state))]));

    this.tableDataSource$ = this.selectedFilterValues$.pipe(
      switchMap(selectedFilterValues => this.dataSource$.pipe(
        map(ds => ds.filter(ds => selectedFilterValues.includes(ds.state))),
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
    this.apiService.updateDeploymentStates();

    interval(2000).subscribe(() => this.apiService.updateDeploymentStates());

  }

  ngOnInit(): void {
        this.selectedFilterValues$.next(['RUNNING', 'FAILED', 'STOPPED']);
        this.sort$.next({active: 'name', direction: 'asc'});
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
}
