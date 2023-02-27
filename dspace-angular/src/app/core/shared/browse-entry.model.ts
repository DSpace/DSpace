import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { ListableObject } from '../../shared/object-collection/shared/listable-object.model';
import { typedObject } from '../cache/builders/build-decorators';
import { excludeFromEquals } from '../utilities/equals.decorators';
import { BROWSE_ENTRY } from './browse-entry.resource-type';
import { GenericConstructor } from './generic-constructor';
import { HALLink } from './hal-link.model';
import { ResourceType } from './resource-type';
import { TypedObject } from '../cache/typed-object.model';

/**
 * Class object representing a browse entry
 */
@typedObject
export class BrowseEntry extends ListableObject implements TypedObject {
  static type = BROWSE_ENTRY;

  /**
   * The object type
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The authority string of this browse entry
   */
  @autoserialize
  authority: string;

  /**
   * The value of this browse entry
   */
  @autoserialize
  value: string;

  /**
   * The language of the value of this browse entry
   */
  @autoserializeAs('valueLang')
  language: string;

  /**
   * Thumbnail link used when browsing items with showThumbs config enabled.
   */
  @autoserializeAs('thumbnail')
  thumbnail: string;

  /**
   * The count of this browse entry
   */
  @excludeFromEquals
  @autoserialize
  count: number;

  @deserialize
  _links: {
    self: HALLink;
    entries: HALLink;
    thumbnail: HALLink;
  };

  /**
   * Method that returns as which type of object this object should be rendered
   */
  getRenderTypes(): (string | GenericConstructor<ListableObject>)[] {
    return [this.constructor as GenericConstructor<ListableObject>];
  }
}
