import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {ApiService} from '../api/api.service';
import {Observable} from 'rxjs';
import {Secret} from '../api/secret';
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
import {MatIcon} from '@angular/material/icon';
import {MatChip, MatChipSet} from '@angular/material/chips';
import {MatButton, MatIconButton} from '@angular/material/button';
import {MatDialog} from '@angular/material/dialog';
import {EditSecretDialogComponent} from './edit-secret-dialog/edit-secret-dialog.component';

@Component({
  selector: 'app-secrets',
  imports: [
    MatCell,
    MatCellDef,
    MatColumnDef,
    MatHeaderCell,
    MatIcon,
    MatTable,
    MatHeaderCellDef,
    MatHeaderRowDef,
    MatRowDef,
    MatHeaderRow,
    MatRow,
    MatIconButton,
    MatChipSet,
    MatChip,
    MatButton,
  ],
  templateUrl: './secrets.component.html',
  styleUrl: './secrets.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SecretsComponent {

  secrets$: Observable<Array<Secret>>;
  displayedColumns = ['actions', 'key', 'environmentVariable', 'deployments'];

  readonly dialog = inject(MatDialog);

  constructor(private apiService: ApiService) {
    this.apiService.reloadSecrets();
    this.secrets$ = apiService.secrets$;
  }

  edit(element: Secret) {
    this.dialog.open(EditSecretDialogComponent, {
      data: element
    });
  }

  add() {
    this.dialog.open(EditSecretDialogComponent, {
      data: {
        key: null,
        value: null,
        environmentVariable: '',
        deployments: []
      } as Secret,
    });
  }
}
