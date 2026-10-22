import {Injectable, signal, inject} from '@angular/core';
import {FederationApi} from '../api/federation-api';
import {FederatedInstance} from '../api/models';

@Injectable({providedIn: 'root'})
export class FederationDirectory {
  private readonly api = inject(FederationApi);

  readonly instances = signal<FederatedInstance[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);

  constructor() {
    this.refresh();
  }

  refresh(): void {
    this.loading.set(true);
    this.error.set(false);
    this.api.list().subscribe({
      next: (instances: FederatedInstance[]) => {
        this.instances.set(instances);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }
}