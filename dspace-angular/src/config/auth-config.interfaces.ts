import { Config } from './config.interface';

export interface AuthTarget {
  host: string;
  page: string;
}

export interface AuthConfig extends Config {
  target?: AuthTarget;

  ui: {
    // The amount of time before the idle warning is shown
    timeUntilIdle: number;
    // The amount of time the user has to react after the idle warning is shown before they are logged out.
    idleGracePeriod: number;
  };

  rest: {
    // If the rest token expires in less than this amount of time, it will be refreshed automatically.
    // This is independent from the idle warning.
    timeLeftBeforeTokenRefresh: number;
  };
}
