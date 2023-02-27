import { SortOptions } from '../cache/models/sort-options.model';
import { RequestParam } from '../cache/models/request-param.model';

/**
 * The options for a find list request
 */
export class FindListOptions {
    scopeID?: string;
    elementsPerPage?: number;
    currentPage?: number;
    sort?: SortOptions;
    searchParams?: RequestParam[];
    startsWith?: string;
    fetchThumbnail?: boolean;
}
