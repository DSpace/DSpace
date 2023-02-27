/* tslint:disable:max-classes-per-file */
import { HALResource } from '../shared/hal-resource.model';
import { HALLink } from '../shared/hal-link.model';
import { TypedObject } from './typed-object.model';

/**
 * An interface to represent objects that can be cached
 *
 * A cacheable object should have a self link
 */
export class CacheableObject extends TypedObject implements HALResource {
  uuid?: string;
  handle?: string;
  _links: {
    self: HALLink;
  };
  // isNew: boolean;
  // dirtyType: DirtyType;
  // hasDirtyAttributes: boolean;
  // changedAttributes: AttributeDiffh;
  // save(): void;
}
