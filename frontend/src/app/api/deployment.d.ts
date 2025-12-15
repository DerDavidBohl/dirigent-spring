export interface Deployment {
  name: string;
  state: string;
  message: string;
  source: string;
  imageUpdates: Array<ImageUpdate>
}

export interface ImageUpdate {
  service: string;
  image: string;
}
