import {PaginationComponentOptions} from './pagination-component-options.model';
import {SortOptions} from '../../core/cache/models/sort-options.model';


/**
 * The pagination event that contains updated pagination properties
 * for a given view
 */
export interface PaginationChangeEvent {
  /**
   * The pagination component object that contains id, current page, max size, page size options, and page size
   */
  pagination: PaginationComponentOptions;

  /**
   * The sort options object that contains which field to sort by and the sorting direction
   */
  sort: SortOptions;
}
