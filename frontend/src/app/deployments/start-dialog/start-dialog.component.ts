import {Component, inject} from '@angular/core';
import {
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogRef,
  MatDialogTitle
} from '@angular/material/dialog';
import {MatButton} from '@angular/material/button';
import {MatCheckbox} from '@angular/material/checkbox';
import {FormsModule} from '@angular/forms';

@Component({
  selector: 'app-start-dialog',
  imports: [
    MatDialogContent,
    MatDialogActions,
    MatDialogTitle,
    MatButton,
    MatCheckbox,
    FormsModule,
    MatDialogClose
  ],
  templateUrl: './start-dialog.component.html',
  styleUrl: './start-dialog.component.css'
})
export class StartDialogComponent {
  readonly dialogRef = inject(MatDialogRef<StartDialogComponent>);
  force: boolean = false;


}
