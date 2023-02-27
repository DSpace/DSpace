import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { ListableObject } from '../../shared/object-collection/shared/listable-object.model';
import { typedObject } from '../cache/builders/build-decorators';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { EXTERNAL_SOURCE_ENTRY } from './external-source-entry.resource-type';
import { GenericConstructor } from './generic-constructor';
import { HALLink } from './hal-link.model';
import { MetadataMap, MetadataMapSerializer } from './metadata.models';
import { ResourceType } from './resource-type';

/**
 * Model class for a single entry from an external source
 */
@typedObject
export class ExternalSourceEntry extends ListableObject {
  static type = EXTERNAL_SOURCE_ENTRY;

  /**
   * Unique identifier
   */
  @autoserialize
  id: string;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The value to display
   */
  @autoserialize
  display: string;

  /**
   * The value to store the entry with
   */
  @autoserialize
  value: string;

  /**
   * The ID of the external source this entry originates from
   */
  @autoserialize
  externalSource: string;

  /**
   * Metadata of the entry
   */
  @autoserializeAs(MetadataMapSerializer)
  metadata: MetadataMap;

  /**
   * The {@link HALLink}s for this ExternalSourceEntry
   */
  @deserialize
  _links: {
    self: HALLink;
  };

  /**
   * Method that returns as which type of object this object should be rendered
   */
  getRenderTypes(): (string | GenericConstructor<ListableObject>)[] {
    return [this.constructor as GenericConstructor<ListableObject>];
  }
}
