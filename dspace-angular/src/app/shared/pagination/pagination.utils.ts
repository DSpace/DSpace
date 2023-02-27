import { PaginationComponentOptions } from './pagination-component-options.model';
import { FindListOptions } from '../../core/data/find-list-options.model';

/**
 * Transform a PaginationComponentOptions object into a FindListOptions object
 * @param pagination  The PaginationComponentOptions to transform
 * @param original    An original FindListOptions object to start from
 */
export function toFindListOptions(pagination: PaginationComponentOptions, original?: FindListOptions): FindListOptions {
  return Object.assign(new FindListOptions(), original, {
    currentPage: pagination.currentPage,
    elementsPerPage: pagination.pageSize
  });
}
