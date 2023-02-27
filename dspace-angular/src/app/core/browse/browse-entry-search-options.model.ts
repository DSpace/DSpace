import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { SortOptions } from '../cache/models/sort-options.model';

/**
 * A class that defines the search options to be used for fetching browse entries or items
 * - metadataDefinition:  The metadata definition to fetch entries or items for
 * - pagination:          Optional pagination options to use
 * - sort:                Optional sorting options to use
 * - startsWith           An optional value to use to filter the browse results
 * - scope:               An optional scope to limit the results within a specific collection or community
 * - fetchThumbnail       An optional boolean to request thumbnail for items
 */
export class BrowseEntrySearchOptions {
  constructor(public metadataDefinition: string,
              public pagination?: PaginationComponentOptions,
              public sort?: SortOptions,
              public startsWith?: string,
              public scope?: string,
              public fetchThumbnail?: boolean) {
  }
}
