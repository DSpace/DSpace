import { SortOptions } from '../../../core/cache/models/sort-options.model';
import { PaginationComponentOptions } from '../../pagination/pagination-component-options.model';
import { isNotEmpty } from '../../empty.util';
import { SearchOptions } from './search-options.model';
import { SearchFilter } from './search-filter.model';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { ViewMode } from '../../../core/shared/view-mode.model';

/**
 * This model class represents all parameters needed to request information about a certain page of a search request, in a certain order
 */
export class PaginatedSearchOptions extends SearchOptions {
  pagination?: PaginationComponentOptions;
  sort?: SortOptions;

  constructor(options: {configuration?: string, scope?: string, query?: string, dsoTypes?: DSpaceObjectType[], filters?: SearchFilter[], fixedFilter?: any, pagination?: PaginationComponentOptions, sort?: SortOptions, view?: ViewMode}) {
    super(options);
    this.pagination = options.pagination;
    this.sort = options.sort;
    this.view = options.view;
  }

  /**
   * Method to generate the URL that can be used to request a certain page with specific sort options
   * @param {string} url The URL to the REST endpoint
   * @param {string[]} args A list of query arguments that should be included in the URL
   * @returns {string} URL with all paginated search options and passed arguments as query parameters
   */
  toRestUrl(url: string, args: string[] = []): string {
    if (isNotEmpty(this.sort)) {
      args.push(`sort=${this.sort.field},${this.sort.direction}`);
    }
    if (isNotEmpty(this.pagination)) {
      args.push(`page=${this.pagination.currentPage - 1}`);
      args.push(`size=${this.pagination.pageSize}`);
    }
    return super.toRestUrl(url, args);
  }
}
