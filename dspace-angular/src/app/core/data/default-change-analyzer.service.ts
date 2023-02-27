import { Injectable } from '@angular/core';
import { compare } from 'fast-json-patch';
import { Operation } from 'fast-json-patch';
import { getClassForType } from '../cache/builders/build-decorators';
import { DSpaceNotNullSerializer } from '../dspace-rest/dspace-not-null.serializer';
import { ChangeAnalyzer } from './change-analyzer';
import { TypedObject } from '../cache/typed-object.model';

/**
 * A class to determine what differs between two
 * CacheableObjects
 */
@Injectable()
export class DefaultChangeAnalyzer<T extends TypedObject> implements ChangeAnalyzer<T> {
  /**
   * Compare the metadata of two CacheableObject and return the differences as
   * a JsonPatch Operation Array
   *
   * @param {CacheableObject} object1
   *    The first object to compare
   * @param {CacheableObject} object2
   *    The second object to compare
   */
  diff(object1: T, object2: T): Operation[] {
    const serializer1 = new DSpaceNotNullSerializer(getClassForType(object1.type));
    const serializer2 = new DSpaceNotNullSerializer(getClassForType(object2.type));
    return compare(serializer1.serialize(object1), serializer2.serialize(object2));
  }
}
