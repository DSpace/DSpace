import { Config } from './config.interface';

export interface MediaViewerConfig extends Config {
  image: boolean;
  video: boolean;
}
