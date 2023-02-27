/**
 * Interface for Health Status
 */
export enum HealthStatus {
  UP = 'UP',
  UP_WITH_ISSUES = 'UP_WITH_ISSUES',
  DOWN = 'DOWN'
}

/**
 * Interface describing the Health endpoint response
 */
export interface HealthResponse {
  status: HealthStatus;
  components: {
    [name: string]: HealthComponent;
  };
}

/**
 * Interface describing a single component retrieved from the Health endpoint response
 */
export interface HealthComponent {
  status: HealthStatus;
  details?: {
    [name: string]: number|string;
  };
  components?: {
    [name: string]: HealthComponent;
  };
}

/**
 * Interface describing the Health info endpoint response
 */
export interface HealthInfoResponse {
  [name: string]: HealthInfoComponent|string;
}

/**
 * Interface describing a single component retrieved from the Health info endpoint response
 */
export interface HealthInfoComponent {
  [property: string]: HealthInfoComponent|string;
}



