import { DeploymentUpdateServiceImage } from "./deployment-update-service-image";

export interface DeploymentUpdate {
    deploymentName: string;
    service: string;
    image: string;
    isRunning: boolean;
}