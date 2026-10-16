import {Injectable, inject} from '@angular/core';
import {Dialog} from '@angular/cdk/dialog';
import {Observable} from 'rxjs';
import {ConfirmData, ConfirmDialog, ConfirmResult} from './confirm-dialog';

@Injectable({providedIn: 'root'})
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  /** Emits the result once the operator confirms; completes without emitting on cancel or Escape. */
  confirm(data: ConfirmData): Observable<ConfirmResult | undefined> {
    return this.dialog.open<ConfirmResult>(ConfirmDialog, {
      data,
      ariaLabelledBy: 'confirm-title',
      autoFocus: 'first-tabbable'
    }).closed;
  }
}
