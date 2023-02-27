import { Component, Input, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { Params, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { hasValue, isNotEmpty } from '../../../empty.util';
import { SearchService } from '../../../../core/shared/search/search.service';
import { currentPath } from '../../../utils/route.utils';
import { PaginationService } from '../../../../core/pagination/pagination.service';
import { SearchConfigurationService } from '../../../../core/shared/search/search-configuration.service';

@Component({
  selector: 'ds-search-label',
  templateUrl: './search-label.component.html',
})

/**
 * Component that represents the label containing the currently active filters
 */
export class SearchLabelComponent implements OnInit {
  @Input() key: string;
  @Input() value: string;
  @Input() inPlaceSearch: boolean;
  @Input() appliedFilters: Observable<Params>;
  searchLink: string;
  removeParameters: Observable<Params>;

  /**
   * The name of the filter without the f. prefix
   */
  filterName: string;

  /**
   * Initialize the instance variable
   */
  constructor(
    private searchService: SearchService,
    private paginationService: PaginationService,
    private searchConfigurationService: SearchConfigurationService,
    private router: Router) {
  }

  ngOnInit(): void {
    this.searchLink = this.getSearchLink();
    this.removeParameters = this.getRemoveParams();
    this.filterName = this.getFilterName();
  }

  /**
   * Calculates the parameters that should change if a given value for the given filter would be removed from the active filters
   * @returns {Observable<Params>} The changed filter parameters
   */
  getRemoveParams(): Observable<Params> {
    return this.appliedFilters.pipe(
      map((filters) => {
        const field: string = Object.keys(filters).find((f) => f === this.key);
        const newValues = hasValue(filters[field]) ? filters[field].filter((v) => v !== this.value) : null;
        const page = this.paginationService.getPageParam(this.searchConfigurationService.paginationID);
        return {
          [field]: isNotEmpty(newValues) ? newValues : null,
          [page]: 1
        };
      })
    );
  }

  /**
   * @returns {string} The base path to the search page, or the current page when inPlaceSearch is true
   */
  private getSearchLink(): string {
    if (this.inPlaceSearch) {
      return currentPath(this.router);
    }
    return this.searchService.getSearchLink();
  }

  /**
   * TODO to review after https://github.com/DSpace/dspace-angular/issues/368 is resolved
   * Strips authority operator from filter value
   * e.g. 'test ,authority' => 'test'
   *
   * @param value
   */
  normalizeFilterValue(value: string) {
    // const pattern = /,[^,]*$/g;
    const pattern = /,authority*$/g;
    return value.replace(pattern, '');
  }

  private getFilterName(): string {
    return this.key.startsWith('f.') ? this.key.substring(2) : this.key;
  }
}
