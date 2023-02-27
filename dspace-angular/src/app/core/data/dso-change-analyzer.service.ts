import { compare, Operation } from 'fast-json-patch';
import { ChangeAnalyzer } from './change-analyzer';
import { Injectable } from '@angular/core';
import { DSpaceObject } from '../shared/dspace-object.model';
import { MetadataMap } from '../shared/metadata.models';
import cloneDeep from 'lodash/cloneDeep';

/**
 * A class to determine what differs between two
 * DSpaceObjects
 */
@Injectable()
export class DSOChangeAnalyzer<T extends DSpaceObject> implements ChangeAnalyzer<T> {

  /**
   * Compare the metadata of two DSpaceObjects and return the differences as
   * a JsonPatch Operation Array
   *
   * @param {DSpaceObject} object1
   *    The first object to compare
   * @param {DSpaceObject} object2
   *    The second object to compare
   */
  diff(object1: DSpaceObject, object2: DSpaceObject): Operation[] {
    return compare(this.filterUUIDsFromMetadata(object1.metadata), this.filterUUIDsFromMetadata(object2.metadata))
      .map((operation: Operation) => Object.assign({}, operation, { path: '/metadata' + operation.path }));
  }

  /**
   * Filter the UUIDs out of a MetadataMap
   * @param metadata
   */
  filterUUIDsFromMetadata(metadata: MetadataMap): MetadataMap {
    const result = cloneDeep(metadata);
    for (const key of Object.keys(result)) {
      for (const metadataValue of result[key]) {
        metadataValue.uuid = undefined;
      }
    }
    return result;
  }
}
