import {ChangeDetectionStrategy, Component, inject, Inject} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogActions, MatDialogContent, MatDialogRef} from '@angular/material/dialog';
import {Secret} from '../../api/secret';
import {FormsModule} from '@angular/forms';
import {MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatButton} from '@angular/material/button';
import {MatChipGrid, MatChipInput, MatChipInputEvent, MatChipRemove, MatChipRow} from '@angular/material/chips';
import {ENTER, SPACE} from '@angular/cdk/keycodes';
import {MatIcon} from '@angular/material/icon';
import {ApiService} from '../../api/api.service';
import {Observable, ReplaySubject} from 'rxjs';
import {map, switchMap} from 'rxjs/operators';
import {
  MatAutocomplete,
  MatAutocompleteSelectedEvent,
  MatAutocompleteTrigger,
  MatOption
} from '@angular/material/autocomplete';
import {AsyncPipe} from '@angular/common';
import { MatCheckbox } from "@angular/material/checkbox";

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
    MatChipInput,
    MatChipRemove,
    MatAutocompleteTrigger,
    MatAutocomplete,
    AsyncPipe,
    MatOption,
    MatCheckbox
],
  templateUrl: './edit-secret-dialog.component.html',
  styleUrl: './edit-secret-dialog.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditSecretDialogComponent {

  readonly dialogRef = inject(MatDialogRef<EditSecretDialogComponent>);
  
  secret: Secret;
  originalSecret: Secret;
  sureDelete: boolean = false;
  restartDeployments: boolean = false;
  deploymentsInputValue: string = '';
  
  $deploymentNames: Observable<Array<string>>;
  $deploymentNamesFilter: ReplaySubject<string> = new ReplaySubject<string>(1);


  constructor(@Inject(MAT_DIALOG_DATA) public data: Secret, private apiService: ApiService) {
    this.secret = structuredClone(data);
    this.originalSecret = structuredClone(data);

    this.$deploymentNames = apiService.deploymentStates$.pipe(
      switchMap(ds => this.$deploymentNamesFilter.pipe(
        map(filter => [{name: filter}, ...ds.filter(ds => ds.name.toLowerCase().includes(filter.toLowerCase()))].filter(d => !!d.name))
      )),
      map(ds =>
        ds.filter(ds => !this.secret.deployments.includes(ds.name))
          .map(ds => ds.name)
          .sort((a, b) => a.localeCompare(b))
      )
    );

    this.$deploymentNamesFilter.next('');
  }

  changed(): boolean {
    return JSON.stringify(this.secret) !== JSON.stringify(this.originalSecret);
  }

  delete() {

    if (!this.sureDelete) {
      this.sureDelete = true;
      return;
    }
    this.apiService.deleteSecret(this.secret, this.restartDeployments).subscribe(() => this.dialogRef.close());
  }

  save() {
    this.apiService.putSecret(this.secret, this.restartDeployments).subscribe(() => this.dialogRef.close());
  }

  cancel() {
    this.dialogRef.close();
  }

  removeDeployment(deployment: string) {
    this.secret.deployments = this.secret.deployments.filter(d => d !== deployment);
  }

  addDeploymentFromInput($event: Event) {

    if(!this.deploymentsInputValue)
      return;

    this.deploymentsInputValue = '';
    this.addDeployment(this.deploymentsInputValue);
  }

  private addDeployment(value: string) {
    if (value.trim().length === 0) return;


    if (this.secret.deployments.includes(value)) return;

    this.secret.deployments.push(value);
    this.$deploymentNamesFilter.next('');
  }

  addDeploymentFromAutoComplete($event: MatAutocompleteSelectedEvent) {
    this.addDeployment($event.option.viewValue);
    $event.option.deselect();
  }

  filterDeployments($event: Event) {
    this.$deploymentNamesFilter.next(($event.target as HTMLInputElement).value);
  }
}
