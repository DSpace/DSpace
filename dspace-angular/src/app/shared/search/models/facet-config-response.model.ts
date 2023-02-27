import { typedObject } from '../../../core/cache/builders/build-decorators';
import { FACET_CONFIG_RESPONSE } from './types/facet-config-response.resouce-type';
import { excludeFromEquals } from '../../../core/utilities/equals.decorators';
import { SearchFilterConfig } from './search-filter-config.model';
import { deserialize } from 'cerialize';
import { HALLink } from '../../../core/shared/hal-link.model';
import { CacheableObject } from '../../../core/cache/cacheable-object.model';

/**
 * The response from the discover/facets endpoint
 */
@typedObject
export class FacetConfigResponse implements CacheableObject {
  static type = FACET_CONFIG_RESPONSE;

  /**
   * The object type,
   * hardcoded because rest doesn't a unique one.
   */
  @excludeFromEquals
  type = FACET_CONFIG_RESPONSE;

  /**
   * the filters in this response
   */
  filters: SearchFilterConfig[];

  /**
   * The {@link HALLink}s for this SearchFilterConfig
   */
  @deserialize
  _links: {
    self: HALLink;
  };
}
