import {Injectable, signal} from '@angular/core';

@Injectable({providedIn: 'root'})
export class FederationTarget {
  readonly selectedId = signal('local');

  select(id: string): void {
    this.selectedId.set(id || 'local');
  }

  apiPath(resourcePath: string): string {
    const resource = resourcePath.replace(/^\/+/, '');
    const instanceId = this.selectedId();
    return instanceId === 'local'
      ? `api/v1/${resource}`
      : `api/v1/federation/instances/${encodeURIComponent(instanceId)}/${resource}`;
  }
}