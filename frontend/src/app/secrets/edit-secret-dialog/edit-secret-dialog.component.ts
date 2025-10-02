import {ChangeDetectionStrategy, Component, inject, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef} from '@angular/material/dialog';
import {Secret} from '../../api/secret';
import {FormsModule} from '@angular/forms';
import {MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatButton} from '@angular/material/button';
import {MatChipGrid, MatChipInput, MatChipInputEvent, MatChipRow} from '@angular/material/chips';
import {ENTER, SPACE} from '@angular/cdk/keycodes';
import {MatIcon} from '@angular/material/icon';
import {ApiService} from '../../api/api.service';

@Component({
  selector: 'app-edit-secret-dialog',
  imports: [
    FormsModule,
    MatInput,
    MatFormField,
    MatLabel,
    MatButton,
    MatDialogContent,
    MatDialogActions,
    MatChipGrid,
    MatChipRow,
    MatIcon,
    MatChipInput
  ],
  templateUrl: './edit-secret-dialog.component.html',
  styleUrl: './edit-secret-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditSecretDialogComponent {

  protected readonly ENTER = ENTER;
  protected readonly SPACE = SPACE;
  readonly dialogRef = inject(MatDialogRef<EditSecretDialogComponent>);
  secret: Secret;
  originalSecret: Secret;
  sureDelete: boolean = false;

  constructor(@Inject(MAT_DIALOG_DATA) public data: Secret, private apiService: ApiService) {
    this.secret = structuredClone(data);
    this.originalSecret = structuredClone(data);
  }

  changed(): boolean {
    return JSON.stringify(this.secret) !== JSON.stringify(this.originalSecret);
  }

  delete() {

    if (!this.sureDelete) {
      this.sureDelete = true;
      return;
    }
    this.apiService.deleteSecret(this.secret).subscribe(() => this.dialogRef.close());
  }

  save() {
    this.apiService.putSecret(this.secret).subscribe(() => this.dialogRef.close());
  }

  cancel() {
    this.dialogRef.close();
  }

  removeDeployment(deployment: string) {
    this.secret.deployments = this.secret.deployments.filter(d => d !== deployment);
  }

  addDeployment($event: MatChipInputEvent) {

    $event.chipInput.clear();

    if (this.secret.deployments.includes($event.value)) return;

    this.secret.deployments.push($event.value);
  }
}
