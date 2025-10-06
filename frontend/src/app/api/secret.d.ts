export interface Secret {
  key: string | null;
  value: string | null;
  environmentVariable: string;
  deployments: Array<string>;
}
