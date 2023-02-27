/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../shared/ngrx/type';
import { RestRequestMethod } from '../data/rest-request-method';

/**
 * The list of ServerSyncBufferAction type definitions
 */
export const ServerSyncBufferActionTypes = {
  ADD: type('dspace/core/cache/syncbuffer/ADD'),
  COMMIT: type('dspace/core/cache/syncbuffer/COMMIT'),
  EMPTY: type('dspace/core/cache/syncbuffer/EMPTY'),
};


/**
 * An ngrx action to add a new cached object to the server sync buffer
 */
export class AddToSSBAction implements Action {
  type = ServerSyncBufferActionTypes.ADD;
  payload: {
    href: string,
    method: RestRequestMethod
  };

  /**
   * Create a new AddToSSBAction
   *
   * @param href
   *    the unique href of the cached object entry that should be updated
   */
  constructor(href: string, method: RestRequestMethod) {
    this.payload = { href, method: method };
  }
}

/**
 * An ngrx action to commit everything (for a certain method, when specified) in the ServerSyncBuffer to the server
 */
export class CommitSSBAction implements Action {
  type = ServerSyncBufferActionTypes.COMMIT;
  payload?: RestRequestMethod;

  /**
   * Create a new CommitSSBAction
   *
   * @param method
   *    an optional method for which the ServerSyncBuffer should send its entries to the server
   */
  constructor(method?: RestRequestMethod) {
    this.payload = method;
  }
}
/**
 * An ngrx action to remove everything (for a certain method, when specified) from the ServerSyncBuffer to the server
 */
export class EmptySSBAction implements Action {
  type = ServerSyncBufferActionTypes.EMPTY;
  payload?: RestRequestMethod;

  /**
   * Create a new EmptySSBAction
   *
   * @param method
   *    an optional method for which the ServerSyncBuffer should remove its entries
   *    if this parameter is omitted, the buffer will be emptied as a whole
   */
  constructor(method?: RestRequestMethod) {
    this.payload = method;
  }
}


/**
 * A type to encompass all ServerSyncBufferActions
 */
export type ServerSyncBufferAction
  = AddToSSBAction
  | CommitSSBAction
  | EmptySSBAction;
