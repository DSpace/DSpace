import { autoserializeAs, inheritSerialization } from 'cerialize';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { SearchResult } from './search-result.model';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { typedObject } from '../../../core/cache/builders/build-decorators';
import { SEARCH_OBJECTS } from './types/search-objects.resource-type';
import { excludeFromEquals } from '../../../core/utilities/equals.decorators';
import { SearchQueryResponse } from './search-query-response.model';

/**
 * Class representing the response returned by the server when performing a search request
 */
@typedObject
@inheritSerialization(PaginatedList)
@inheritSerialization(SearchQueryResponse)
export class SearchObjects<T extends DSpaceObject> extends SearchQueryResponse<SearchResult<T>> {
  static type = SEARCH_OBJECTS;

  /**
   * The sort parameters used in the search request
   * Hardcoded because rest doesn't provide a unique type
   */
  @excludeFromEquals
  public type = SEARCH_OBJECTS;

  /**
   * The results for this query
   */
  @autoserializeAs(SearchResult, 'objects')
  page: SearchResult<T>[];
}
