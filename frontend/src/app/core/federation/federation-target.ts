import {Injectable, signal} from '@angular/core';
import {Subject} from 'rxjs';

@Injectable({providedIn: 'root'})
export class FederationTarget {
  readonly selectedId = signal('local');
  private readonly selectionChanged = new Subject<string>();
  readonly selectionChanges = this.selectionChanged.asObservable();

  select(id: string): void {
    const nextId = id || 'local';
    if (nextId === this.selectedId()) {
      return;
    }
    this.selectedId.set(nextId);
    this.selectionChanged.next(nextId);
  }

  apiPath(resourcePath: string): string {
    const resource = resourcePath.replace(/^\/+/, '');
    const instanceId = this.selectedId();
    return instanceId === 'local'
      ? `api/v1/${resource}`
      : `api/v1/federation/instances/${encodeURIComponent(instanceId)}/${resource}`;
  }
}