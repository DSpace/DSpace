import { Operation } from 'fast-json-patch';

import { TypedObject } from '../cache/typed-object.model';

/**
 * An interface to determine what differs between two
 * NormalizedObjects
 */
export interface ChangeAnalyzer<T extends TypedObject> {

  /**
   * Compare two objects and return their differences as a
   * JsonPatch Operation Array
   *
   * @param {CacheableObject} object1
   *    The first object to compare
   * @param {CacheableObject} object2
   *    The second object to compare
   */
  diff(object1: T, object2: T): Operation[];
}
