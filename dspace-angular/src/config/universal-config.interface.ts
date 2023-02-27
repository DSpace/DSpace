import { Config } from './config.interface';

export interface UniversalConfig extends Config {
  preboot: boolean;
  async: boolean;
  time: boolean;
}
