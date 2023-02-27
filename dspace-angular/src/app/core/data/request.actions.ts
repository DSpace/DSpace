/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../../shared/ngrx/type';
import { HALLink } from '../shared/hal-link.model';
import { UnCacheableObject } from '../shared/uncacheable-object.model';
import { RestRequest } from './rest-request.model';

/**
 * The list of RequestAction type definitions
 */
export const RequestActionTypes = {
  CONFIGURE: type('dspace/core/data/request/CONFIGURE'),
  EXECUTE: type('dspace/core/data/request/EXECUTE'),
  SUCCESS: type('dspace/core/data/request/SUCCESS'),
  ERROR: type('dspace/core/data/request/ERROR'),
  STALE: type('dspace/core/data/request/STALE'),
  RESET_TIMESTAMPS: type('dspace/core/data/request/RESET_TIMESTAMPS'),
  REMOVE: type('dspace/core/data/request/REMOVE')
};

export abstract class RequestUpdateAction implements Action {
  abstract type: string;
  lastUpdated: number;

  constructor() {
    this.lastUpdated = new Date().getTime();
  }
}

export class RequestConfigureAction extends RequestUpdateAction {
  type = RequestActionTypes.CONFIGURE;
  payload: RestRequest;

  constructor(
    request: RestRequest
  ) {
    super();
    this.payload = request;
  }
}

export class RequestExecuteAction extends RequestUpdateAction {
  type = RequestActionTypes.EXECUTE;
  payload: string;

  /**
   * Create a new RequestExecuteAction
   *
   * @param uuid
   *    the request's uuid
   */
  constructor(uuid: string) {
    super();
    this.payload = uuid;
  }
}

/**
 * An ngrx action to indicate a successful response was returned
 */
export class RequestSuccessAction extends RequestUpdateAction {
  type = RequestActionTypes.SUCCESS;
  payload: {
    uuid: string,
    timeCompleted: number,
    statusCode: number,
    link?: HALLink,
    unCacheableObject?: UnCacheableObject
  };

  /**
   * Create a new RequestSuccessAction
   *
   * @param uuid
   *    the request's uuid
   * @param statusCode
   *    the statusCode of the response
   * @param link
   *    the HALlink to the object that was returned, and has been cached
   * @param unCacheableObject
   *    in case the REST API returns an object that can't be cached, because it doesn't have a self
   *    link, provide it here
   */
  constructor(uuid: string, statusCode: number, link?: HALLink, unCacheableObject?: UnCacheableObject) {
    super();
    this.payload = {
      uuid,
      timeCompleted: new Date().getTime(),
      statusCode,
      link,
      unCacheableObject
    };
  }
}

/**
 * An ngrx action to indicate an error response was returned
 */
export class RequestErrorAction extends RequestUpdateAction {
  type = RequestActionTypes.ERROR;
  payload: {
    uuid: string,
    timeCompleted: number,
    statusCode: number,
    errorMessage: string
  };

  /**
   * Create a new RequestErrorAction
   *
   * @param uuid
   *    the request's uuid
   * @param statusCode
   *    the statusCode of the response
   * @param errorMessage
   *    the error message in the response
   */
  constructor(uuid: string, statusCode: number, errorMessage: string) {
    super();
    this.payload = {
      uuid,
      timeCompleted: new Date().getTime(),
      statusCode,
      errorMessage
    };
  }
}

/**
 * An ngrx action to indicate the response to this request is stale
 */
export class RequestStaleAction extends RequestUpdateAction {
  type = RequestActionTypes.STALE;
  payload: {
    uuid: string,
  };

  /**
   * Create a new RequestStaleAction
   *
   * @param uuid
   *    the request's uuid
   */
  constructor(uuid: string) {
    super();
    this.payload = {
      uuid,
    };
  }
}

/**
 * An ngrx action to reset the timeCompleted property of all responses in the cached objects
 */
export class ResetResponseTimestampsAction implements Action {
  type = RequestActionTypes.RESET_TIMESTAMPS;
  payload: number;

  /**
   * Create a new ResetResponseTimestampsAction
   *
   * @param newTimestamp
   *    the new timeCompleted all objects should get
   */
  constructor(newTimestamp: number) {
    this.payload = newTimestamp;
  }
}

/**
 * An ngrx action to remove a cached request
 */
export class RequestRemoveAction implements Action {
  type = RequestActionTypes.REMOVE;
  uuid: string;

  /**
   * Create a new RequestRemoveAction
   *
   * @param uuid
   *    the request's uuid
   */
  constructor(uuid: string) {
    this.uuid = uuid;
  }
}


/**
 * A type to encompass all RequestActions
 */
export type RequestAction
  = RequestConfigureAction
  | RequestExecuteAction
  | RequestSuccessAction
  | RequestErrorAction
  | RequestStaleAction
  | ResetResponseTimestampsAction
  | RequestRemoveAction;
