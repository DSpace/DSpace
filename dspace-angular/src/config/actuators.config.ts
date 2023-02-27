import { Config } from './config.interface';

/**
 * Config that determines the spring Actuators options
 */
export class ActuatorsConfig implements Config {
  /**
   * The endpoint path
   */
  public endpointPath: string;
}
