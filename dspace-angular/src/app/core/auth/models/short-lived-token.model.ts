import { typedObject } from '../../cache/builders/build-decorators';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { ResourceType } from '../../shared/resource-type';
import { SHORT_LIVED_TOKEN } from './short-lived-token.resource-type';
import { HALLink } from '../../shared/hal-link.model';
import { CacheableObject } from '../../cache/cacheable-object.model';

/**
 * A short-lived token that can be used to authenticate a rest request
 */
@typedObject
export class ShortLivedToken implements CacheableObject {
  static type = SHORT_LIVED_TOKEN;
  /**
   * The type for this ShortLivedToken
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The value for this ShortLivedToken
   */
  @autoserializeAs('token')
  value: string;

  /**
   * The {@link HALLink}s for this ShortLivedToken
   */
  @deserialize
  _links: {
    self: HALLink;
  };
}
