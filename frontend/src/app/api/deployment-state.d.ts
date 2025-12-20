import { ImageUpdate } from "./image-update";

export interface DeploymentState {
  name: string;
  state: string;
  message: string;
  source: string;
}

