import { RestRequestMethod } from '../app/core/data/rest-request-method';

/**
 * The number of seconds between automatic syncs to the
 * server for requests using a certain HTTP Method
 */
type TimePerMethod = {
  [method in RestRequestMethod]: number;
};

/**
 * The config that determines how the automatic syncing
 * of changed data to the server works
 */
export interface AutoSyncConfig {
  /**
   * The number of seconds between automatic syncs to the server
   */
  defaultTime: number;

  /**
   * HTTP Method specific overrides of defaultTime
   */
  timePerMethod: TimePerMethod;

  /**
   * The max number of requests in the buffer before a sync to the server
   */
  maxBufferSize: number;
}
