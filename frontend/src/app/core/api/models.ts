export interface DeploymentState {
  name: string;
  state: string;
  message: string | null;
  source: string;
}

export interface DeploymentUpdate {
  deploymentName: string;
  service: string;
  image: string;
  isRunning: boolean;
}

export interface Secret {
  key: string | null;
  value: string | null;
  environmentVariable: string;
  deployments: string[];
}

export interface SystemInformation {
  instanceName: string;
  gitUrl: string;
}

export interface FederatedDeployment {
  name: string;
  state: string;
  message: string;
}

export interface FederatedInstance {
  id: string;
  instanceName: string;
  available: boolean;
  deployments: FederatedDeployment[];
  error: string;
}

export interface AuthenticatedUser {
  username: string;
  apiToken: boolean;
}

export interface AuthOptions {
  oidcEnabled: boolean;
  oidcLoginPath: string | null;
}

export interface MachineToken {
  id: number;
  name: string;
  permissions: string[];
  createdBy: string;
  createdAt: string;
  expiresAt: string | null;
  revokedAt: string | null;
  lastUsedAt?: string | null;
}

export interface CreatedMachineToken {
  id: number;
  name: string;
  permissions: string[];
  createdAt: string;
  expiresAt: string | null;
  token: string;
}
