import { NgbPaginationConfig } from '@ng-bootstrap/ng-bootstrap';
import { Injectable } from '@angular/core';

@Injectable()
export class PaginationComponentOptions extends NgbPaginationConfig {
  /**
   * ID for the pagination instance. Only useful if you wish to
   * have more than once instance at a time in a given component.
   */
  id: string;

  /**
   * The active page.
   */
  currentPage = 1;

  /**
   * Maximum number of pages to display.
   */
  maxSize = 10;

  /**
   * A number array that represents options for a context pagination limit.
   */
  pageSizeOptions: number[] = [1, 5, 10, 20, 40, 60, 80, 100];

  /**
   * Number of items per page.
   */
  pageSize: number;

}
