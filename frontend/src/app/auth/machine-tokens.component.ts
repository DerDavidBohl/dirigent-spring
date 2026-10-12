import {ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {CommonModule, DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatChipsModule} from '@angular/material/chips';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {provideNativeDateAdapter} from '@angular/material/core';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {MatTableModule} from '@angular/material/table';
import {AuthService, CreatedMachineToken, MachineToken} from '../api/auth.service';

@Component({
  selector: 'app-machine-tokens',
  imports: [CommonModule, FormsModule, MatButtonModule, MatCheckboxModule, MatChipsModule, MatDatepickerModule, MatIconModule, MatInputModule, MatTableModule, DatePipe],
  providers: [provideNativeDateAdapter()],
  templateUrl: './machine-tokens.component.html',
})
export class MachineTokensComponent implements OnInit {
  readonly availablePermissions = [
    {value: 'deployments:read', label: 'Read deployments'},
    {value: 'deployments:write', label: 'Start and stop deployments'},
    {value: 'secrets:read', label: 'Read secret metadata'},
    {value: 'secrets:write', label: 'Manage secrets'},
    {value: 'updates:read', label: 'Read image updates'},
    {value: 'updates:write', label: 'Run image updates'},
    {value: 'system:read', label: 'Read system information'},
    {value: 'webhooks:trigger', label: 'Trigger repository webhooks'}
  ];

  readonly displayedColumns = ['name', 'permissions', 'expiresAt', 'status', 'actions'];

  tokens: MachineToken[] = [];
  name = '';
  expiry: Date | null = null;
  selectedPermissions = new Set<string>();
  issuedToken: CreatedMachineToken | null = null;
  error = '';
  busy = false;

  constructor(private authService: AuthService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.reload();
  }

  hasPermission(permission: string): boolean {
    return this.selectedPermissions.has(permission);
  }

  tokenStatus(token: MachineToken): string {
    if (token.revokedAt) return 'Revoked';
    if (!token.expiresAt) return 'Inactive: no expiry';
    return new Date(token.expiresAt).getTime() <= Date.now() ? 'Expired' : 'Active';
  }

  setPermission(permission: string, checked: boolean): void {
    const updated = new Set(this.selectedPermissions);
    checked ? updated.add(permission) : updated.delete(permission);
    this.selectedPermissions = updated;
  }

  create(): void {
    this.busy = true;
    this.error = '';
    const expiresAt = this.expiry
      ? new Date(Date.UTC(this.expiry.getFullYear(), this.expiry.getMonth(), this.expiry.getDate())).toISOString()
      : null;
    this.authService.createToken(this.name.trim(), [...this.selectedPermissions], expiresAt).subscribe({
      next: token => {
        this.issuedToken = token;
        this.name = '';
        this.expiry = null;
        this.selectedPermissions = new Set<string>();
        this.busy = false;
        this.cdr.detectChanges();
        this.reload();
      },
      error: () => {
        this.error = 'The token could not be created. Check its name, permissions, and expiry.';
        this.busy = false;
        this.cdr.detectChanges();
      }
    });
  }

  revoke(token: MachineToken): void {
    this.authService.revokeToken(token.id).subscribe({
      next: () => this.reload(),
      error: () => this.error = 'The token could not be revoked.'
    });
  }

  copyIssuedToken(): void {
    if (this.issuedToken) {
      void navigator.clipboard.writeText(this.issuedToken.token);
    }
  }

  dismissIssuedToken(): void {
    this.issuedToken = null;
  }

  private reload(): void {
    this.authService.listTokens().subscribe({
      next: tokens => {
        this.tokens = tokens;
        this.cdr.markForCheck();
      },
      error: () => this.error = 'Token information could not be loaded.'
    });
  }
}