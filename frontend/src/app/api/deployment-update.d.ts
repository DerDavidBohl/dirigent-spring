import { DeploymentUpdateServiceImage } from "./deployment-update-service-image";

export interface DeploymentUpdate {
    deploymentName: string;
    serviceUpdates: Array<DeploymentUpdateServiceImage>;
}