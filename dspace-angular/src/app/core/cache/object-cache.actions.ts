/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../shared/ngrx/type';
import { Operation } from 'fast-json-patch';
import { CacheableObject } from './cacheable-object.model';

/**
 * The list of ObjectCacheAction type definitions
 */
export const ObjectCacheActionTypes = {
  ADD: type('dspace/core/cache/object/ADD'),
  REMOVE: type('dspace/core/cache/object/REMOVE'),
  RESET_TIMESTAMPS: type('dspace/core/cache/object/RESET_TIMESTAMPS'),
  ADD_PATCH: type('dspace/core/cache/object/ADD_PATCH'),
  APPLY_PATCH: type('dspace/core/cache/object/APPLY_PATCH'),
  ADD_DEPENDENTS: type('dspace/core/cache/object/ADD_DEPENDENTS'),
  REMOVE_DEPENDENTS: type('dspace/core/cache/object/REMOVE_DEPENDENTS')
};

/**
 * An ngrx action to add an object to the cache
 */
export class AddToObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.ADD;
  payload: {
    objectToCache: CacheableObject;
    timeCompleted: number;
    msToLive: number;
    requestUUID: string;
    alternativeLink?: string;
  };

  /**
   * Create a new AddToObjectCacheAction
   *
   * @param objectToCache
   *    the object to add
   * @param timeCompleted
   *    the time it was added
   * @param msToLive
   *    the amount of milliseconds before it should expire
   * @param requestUUID
   *    The uuid of the request that resulted in this object
   *    This isn't necessarily the same as the object's self
   *    link, it could have been part of a list for example
   *  @param alternativeLink An optional alternative link to this object
   */
  constructor(objectToCache: CacheableObject, timeCompleted: number, msToLive: number, requestUUID: string, alternativeLink?: string) {
    this.payload = { objectToCache, timeCompleted, msToLive, requestUUID, alternativeLink };
  }
}

/**
 * An ngrx action to remove an object from the cache
 */
export class RemoveFromObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.REMOVE;
  payload: string;

  /**
   * Create a new RemoveFromObjectCacheAction
   *
   * @param href
   *    the unique href of the object to remove
   */
  constructor(href: string) {
    this.payload = href;
  }
}

/**
 * An ngrx action to reset the timeCompleted property of all cached objects
 */
export class ResetObjectCacheTimestampsAction implements Action {
  type = ObjectCacheActionTypes.RESET_TIMESTAMPS;
  payload: number;

  /**
   * Create a new ResetObjectCacheTimestampsAction
   *
   * @param newTimestamp
   *    the new timeCompleted all objects should get
   */
  constructor(newTimestamp: number) {
    this.payload = newTimestamp;
  }
}

/**
 * An ngrx action to add new operations to a specified cached object
 */
export class AddPatchObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.ADD_PATCH;
  payload: {
    href: string,
    operations: Operation[]
  };

  /**
   * Create a new AddPatchObjectCacheAction
   *
   * @param href
   *    the unique href of the object that should be updated
   * @param operations
   *    the list of operations to add
   */
  constructor(href: string, operations: Operation[]) {
    this.payload = { href, operations };
  }
}

/**
 * An ngrx action to apply all existing operations to a specified cached object
 */
export class ApplyPatchObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.APPLY_PATCH;
  payload: string;

  /**
   * Create a new ApplyPatchObjectCacheAction
   *
   * @param href
   *    the unique href of the object that should be updated
   */
  constructor(href: string) {
    this.payload = href;
  }
}

/**
 * An NgRx action to add dependent request UUIDs to a cached object
 */
export class AddDependentsObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.ADD_DEPENDENTS;
  payload: {
    href: string;
    dependentRequestUUIDs: string[];
  };

  /**
   * Create a new AddDependentsObjectCacheAction
   *
   * @param href                  the self link of a cached object
   * @param dependentRequestUUIDs the UUID of the request that depends on this object
   */
  constructor(href: string, dependentRequestUUIDs: string[]) {
    this.payload = {
      href,
      dependentRequestUUIDs,
    };
  }
}

/**
 * An NgRx action to remove all dependent request UUIDs from a cached object
 */
export class RemoveDependentsObjectCacheAction implements Action {
  type = ObjectCacheActionTypes.REMOVE_DEPENDENTS;
  payload: string;

  /**
   * Create a new RemoveDependentsObjectCacheAction
   *
   * @param href  the self link of a cached object for which to remove all dependent request UUIDs
   */
  constructor(href: string) {
    this.payload = href;
  }
}

/**
 * A type to encompass all ObjectCacheActions
 */
export type ObjectCacheAction
  = AddToObjectCacheAction
    | RemoveFromObjectCacheAction
    | ResetObjectCacheTimestampsAction
    | AddPatchObjectCacheAction
    | ApplyPatchObjectCacheAction
    | AddDependentsObjectCacheAction
    | RemoveDependentsObjectCacheAction;
