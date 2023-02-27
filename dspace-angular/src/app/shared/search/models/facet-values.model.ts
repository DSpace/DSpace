import { typedObject } from '../../../core/cache/builders/build-decorators';
import { excludeFromEquals } from '../../../core/utilities/equals.decorators';
import { FACET_VALUES } from './types/facet-values.resource-type';
import { FacetValue } from './facet-value.model';
import { SearchQueryResponse } from './search-query-response.model';
import { autoserialize, autoserializeAs, inheritSerialization } from 'cerialize';
import { FilterType } from './filter-type.model';
import { PaginatedList } from '../../../core/data/paginated-list.model';

@typedObject
@inheritSerialization(PaginatedList)
@inheritSerialization(SearchQueryResponse)
export class FacetValues extends SearchQueryResponse<FacetValue> {
  static type = FACET_VALUES;

  /**
   * The sort parameters used in the search request
   * Hardcoded because rest doesn't provide a unique type
   */
  @excludeFromEquals
  public type = FACET_VALUES;

  /**
   * The name of the facet the values are for
   */
  @autoserialize
  name: string;

  /**
   * The type of facet the values are for
   */
  @autoserializeAs(String, 'facetType')
  filterType: FilterType;

  /**
   * The max number of returned facetValues
   */
  @autoserialize
  facetLimit: number;

  /**
   * The results for this query
   */
  @autoserializeAs(FacetValue, 'values')
  page: FacetValue[];
}
