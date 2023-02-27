/* eslint-disable max-classes-per-file */
import {
  AddDependentsObjectCacheAction,
  AddPatchObjectCacheAction,
  AddToObjectCacheAction,
  ApplyPatchObjectCacheAction,
  ObjectCacheAction,
  ObjectCacheActionTypes, RemoveDependentsObjectCacheAction,
  RemoveFromObjectCacheAction,
  ResetObjectCacheTimestampsAction,
} from './object-cache.actions';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { CacheEntry } from './cache-entry';
import { applyPatch, Operation } from 'fast-json-patch';
import { CacheableObject } from './cacheable-object.model';

/**
 * An interface to represent a JsonPatch
 */
export interface Patch {
  /**
   * The identifier for this Patch
   */
  uuid?: string;

  /**
   * the list of operations this Patch is composed of
   */
  operations: Operation[];
}

/**
 * Get the string value for an object that may be a string or a ResourceType
 *
 * @param type the object to get the type value for
 */
export const getResourceTypeValueFor = (type: any): string => {
  if (hasValue(type)) {
    if (typeof type === 'string') {
      return type;
    } else if (typeof type.value === 'string') {
      return type.value;
    }
  }
};

/**
 * An entry in the ObjectCache
 */
export class ObjectCacheEntry implements CacheEntry {
  /**
   * The object being cached
   */
  data: CacheableObject;

  /**
   * The timestamp for when this entry was set to completed
   */
  timeCompleted: number;

  /**
   * The number of milliseconds after the entry completes until it becomes stale
   */
  msToLive: number;

  /**
   * The UUIDs of the requests that caused this entry to be added
   * New UUIDs should be added to the front of the array
   * to make retrieving the latest UUID easier.
   */
  requestUUIDs: string[];

  /**
   * A list of UUIDs for the requests that depend on this object.
   * When this object is invalidated, these requests will be invalidated as well.
   */
  dependentRequestUUIDs: string[];

  /**
   * An array of patches that were made on the client side to this entry, but haven't been sent to the server yet
   */
  patches: Patch[] = [];

  /**
   * Whether this entry has changes that haven't been sent to the server yet
   */
  isDirty: boolean;

  /**
   * A list of links, apart from the self link, that also uniquely identify this object
   * e.g. https://rest.api/collections/12345/logo could be an alternative link for a
   * bitstream
   */
  alternativeLinks: string[];
}


/**
 * The ObjectCache State
 *
 * Consists of a map with self links as keys,
 * and ObjectCacheEntries as values
 */
export interface ObjectCacheState {
  [href: string]: ObjectCacheEntry;
}

// Object.create(null) ensures the object has no default js properties (e.g. `__proto__`)
const initialState: ObjectCacheState = Object.create(null);

/**
 * The ObjectCache Reducer
 *
 * @param state
 *    the current state
 * @param action
 *    the action to perform on the state
 * @return ObjectCacheState
 *    the new state
 */
export function objectCacheReducer(state = initialState, action: ObjectCacheAction): ObjectCacheState {
  switch (action.type) {

    case ObjectCacheActionTypes.ADD: {
      return addToObjectCache(state, action as AddToObjectCacheAction);
    }

    case ObjectCacheActionTypes.REMOVE: {
      return removeFromObjectCache(state, action as RemoveFromObjectCacheAction);
    }

    case ObjectCacheActionTypes.RESET_TIMESTAMPS: {
      return resetObjectCacheTimestamps(state, action as ResetObjectCacheTimestampsAction);
    }

    case ObjectCacheActionTypes.ADD_PATCH: {
      return addPatchObjectCache(state, action as AddPatchObjectCacheAction);
    }

    case ObjectCacheActionTypes.APPLY_PATCH: {
      return applyPatchObjectCache(state, action as ApplyPatchObjectCacheAction);
    }

    case ObjectCacheActionTypes.ADD_DEPENDENTS: {
      return addDependentsObjectCacheState(state, action as AddDependentsObjectCacheAction);
    }

    case ObjectCacheActionTypes.REMOVE_DEPENDENTS: {
      return removeDependentsObjectCacheState(state, action as RemoveDependentsObjectCacheAction);
    }

    default: {
      return state;
    }
  }
}

/**
 * Add an object to the cache
 *
 * @param state
 *    the current state
 * @param action
 *    an AddToObjectCacheAction
 * @return ObjectCacheState
 *    the new state, with the object added, or overwritten.
 */
function addToObjectCache(state: ObjectCacheState, action: AddToObjectCacheAction): ObjectCacheState {
  const existing = state[action.payload.objectToCache._links.self.href] || {} as any;
  const newAltLinks = hasValue(action.payload.alternativeLink) ? [action.payload.alternativeLink] : [];
  return Object.assign({}, state, {
    [action.payload.objectToCache._links.self.href]: {
      data: action.payload.objectToCache,
      timeCompleted: action.payload.timeCompleted,
      msToLive: action.payload.msToLive,
      requestUUIDs: [action.payload.requestUUID, ...(existing.requestUUIDs || [])],
      dependentRequestUUIDs: existing.dependentRequestUUIDs || [],
      isDirty: isNotEmpty(existing.patches),
      patches: existing.patches || [],
      alternativeLinks: [...(existing.alternativeLinks || []), ...newAltLinks]
    } as ObjectCacheEntry
  });
}

/**
 * Remove an object from the cache
 *
 * @param state
 *    the current state
 * @param action
 *    an RemoveFromObjectCacheAction
 * @return ObjectCacheState
 *    the new state, with the object removed if it existed.
 */
function removeFromObjectCache(state: ObjectCacheState, action: RemoveFromObjectCacheAction): ObjectCacheState {
  if (hasValue(state[action.payload])) {
    const newObjectCache = Object.assign({}, state);
    delete newObjectCache[action.payload];

    return newObjectCache;
  } else {
    return state;
  }
}

/**
 * Set the timeCompleted timestamp of every cached object to the specified value
 *
 * @param state
 *    the current state
 * @param action
 *    a ResetObjectCacheTimestampsAction
 * @return ObjectCacheState
 *    the new state, with all timeCompleted timestamps set to the specified value
 */
function resetObjectCacheTimestamps(state: ObjectCacheState, action: ResetObjectCacheTimestampsAction): ObjectCacheState {
  const newState = Object.create(null);
  Object.keys(state).forEach((key) => {
    newState[key] = Object.assign({}, state[key], {
      timeCompleted: action.payload
    });
  });
  return newState;
}

/**
 * Add the list of patch operations to a cached object
 *
 * @param state
 *    the current state
 * @param action
 *    an AddPatchObjectCacheAction
 * @return ObjectCacheState
 *    the new state, with the new operations added to the state of the specified ObjectCacheEntry
 */
function addPatchObjectCache(state: ObjectCacheState, action: AddPatchObjectCacheAction): ObjectCacheState {
  const uuid = action.payload.href;
  const operations = action.payload.operations;
  const newState = Object.assign({}, state);
  if (hasValue(newState[uuid])) {
    const patches = newState[uuid].patches;
    newState[uuid] = Object.assign({}, newState[uuid], {
      patches: [...patches, { operations } as Patch],
      isDirty: true
    });
  }
  return newState;
}

/**
 * Apply the list of patch operations to a cached object
 *
 * @param state
 *    the current state
 * @param action
 *    an ApplyPatchObjectCacheAction
 * @return ObjectCacheState
 *    the new state, with the new operations applied to the state of the specified ObjectCacheEntry
 */
function applyPatchObjectCache(state: ObjectCacheState, action: ApplyPatchObjectCacheAction): ObjectCacheState {
  const uuid = action.payload;
  const newState = Object.assign({}, state);
  if (hasValue(newState[uuid])) {
    // flatten two dimensional array
    const flatPatch: Operation[] = [].concat(...newState[uuid].patches.map((patch) => patch.operations));
    const newData = applyPatch(newState[uuid].data, flatPatch, undefined, false);
    newState[uuid] = Object.assign({}, newState[uuid], { data: newData.newDocument, patches: [] });
  }
  return newState;
}

/**
 * Add a list of dependent request UUIDs to a cached object, used when defining new dependencies
 *
 * @param state   the current state
 * @param action  an AddDependentsObjectCacheAction
 * @return        the new state, with the dependent requests of the cached object updated
 */
function addDependentsObjectCacheState(state: ObjectCacheState, action: AddDependentsObjectCacheAction): ObjectCacheState {
  const href = action.payload.href;
  const newState = Object.assign({}, state);

  if (hasValue(newState[href])) {
    newState[href] = Object.assign({}, newState[href], {
      dependentRequestUUIDs: [
        ...new Set([
          ...newState[href]?.dependentRequestUUIDs || [],
          ...action.payload.dependentRequestUUIDs,
        ])
      ]
    });
  }

  return newState;
}


/**
 * Remove all dependent request UUIDs from a cached object, used to clear out-of-date depedencies
 *
 * @param state   the current state
 * @param action  an AddDependentsObjectCacheAction
 * @return        the new state, with the dependent requests of the cached object updated
 */
function removeDependentsObjectCacheState(state: ObjectCacheState, action: RemoveDependentsObjectCacheAction): ObjectCacheState {
  const href = action.payload;
  const newState = Object.assign({}, state);

  if (hasValue(newState[href])) {
    newState[href] = Object.assign({}, newState[href], {
      dependentRequestUUIDs: []
    });
  }

  return newState;
}
