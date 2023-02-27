import { autoserialize, deserialize } from 'cerialize';
import { HALLink } from '../../shared/hal-link.model';
import { ResourceType } from '../../shared/resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { CacheableObject } from '../../cache/cacheable-object.model';

export abstract class ConfigObject implements CacheableObject {

  /**
   * The name for this configuration
   */
  @autoserialize
  public id: string;

  /**
   * The name for this configuration
   */
  @autoserialize
  public name: string;

  /**
   * The type of this ConfigObject
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The links to all related resources returned by the rest api.
   */
  @deserialize
  _links: {
    self: HALLink,
    [name: string]: HALLink
  };
}
