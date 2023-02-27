import { typedObject } from '../cache/builders/build-decorators';
import { autoserialize, deserialize, inheritSerialization } from 'cerialize';
import { FEATURE } from './feature.resource-type';
import { DSpaceObject } from './dspace-object.model';
import { HALLink } from './hal-link.model';

/**
 * Class representing a DSpace Feature
 */
@typedObject
@inheritSerialization(DSpaceObject)
export class Feature extends DSpaceObject {
  static type = FEATURE;

  /**
   * Unique identifier for this feature
   */
  @autoserialize
  id: string;

  /**
   * A human readable description of the feature's purpose
   */
  @autoserialize
  description: string;

  /**
   * A list of resource types this feature applies to
   */
  @autoserialize
  resourcetypes: string[];

  @deserialize
  _links: {
    self: HALLink;
  };
}
