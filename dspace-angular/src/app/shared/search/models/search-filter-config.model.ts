import { FilterType } from './filter-type.model';
import { autoserialize, autoserializeAs, deserialize } from 'cerialize';
import { HALLink } from '../../../core/shared/hal-link.model';
import { typedObject } from '../../../core/cache/builders/build-decorators';
import { excludeFromEquals } from '../../../core/utilities/equals.decorators';
import { SEARCH_FILTER_CONFIG } from './types/search-filter-config.resource-type';
import { CacheableObject } from '../../../core/cache/cacheable-object.model';

/**
 * The configuration for a search filter
 */
@typedObject
  export class SearchFilterConfig implements CacheableObject {
    static type = SEARCH_FILTER_CONFIG;

    /**
     * The object type,
     * hardcoded because rest doesn't set one.
     */
    @excludeFromEquals
    type = SEARCH_FILTER_CONFIG;

    /**
     * The name of this filter
     */
    @autoserialize
    name: string;

    /**
     * The FilterType of this filter
     */
    @autoserializeAs(String, 'facetType')
    filterType: FilterType;

    /**
     * True if the filter has facets
     */
    @autoserialize
    hasFacets: boolean;

    /**
     * @type {number} The page size used for this facet
     */
    @autoserializeAs(String, 'facetLimit')
    pageSize = 5;

    /**
     * Defines if the item facet is collapsed by default or not on the search page
     */
    @autoserialize
    isOpenByDefault: boolean;

    /**
     * Minimum value possible for this facet in the repository
     */
    @autoserialize
    maxValue: string;

    /**
     * Maximum value possible for this facet in the repository
     */
    @autoserialize
    minValue: string;

    /**
     * The {@link HALLink}s for this SearchFilterConfig
     */
    @deserialize
    _links: {
      self: HALLink;
    };

    /**
     * Name of this configuration that can be used in a url
     * @returns Parameter name
     */
    get paramName(): string {
      return 'f.' + this.name;
    }
  }
