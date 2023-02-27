import { Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FacetValue } from '../../../../models/facet-value.model';
import { SearchFilterConfig } from '../../../../models/search-filter-config.model';
import { SearchService } from '../../../../../../core/shared/search/search.service';
import { SearchFilterService } from '../../../../../../core/shared/search/search-filter.service';
import {
  RANGE_FILTER_MAX_SUFFIX,
  RANGE_FILTER_MIN_SUFFIX
} from '../../search-range-filter/search-range-filter.component';
import { SearchConfigurationService } from '../../../../../../core/shared/search/search-configuration.service';
import { hasValue } from '../../../../../empty.util';
import { currentPath } from '../../../../../utils/route.utils';
import { PaginationService } from '../../../../../../core/pagination/pagination.service';

const rangeDelimiter = '-';

@Component({
  selector: 'ds-search-facet-range-option',
  styleUrls: ['./search-facet-range-option.component.scss'],
  // templateUrl: './search-facet-range-option.component.html',
  templateUrl: './search-facet-range-option.component.html',
})

/**
 * Represents a single option in a range filter facet
 */
export class SearchFacetRangeOptionComponent implements OnInit, OnDestroy {
  /**
   * A single value for this component
   */
  @Input() filterValue: FacetValue;

  /**
   * The filter configuration for this facet option
   */
  @Input() filterConfig: SearchFilterConfig;

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;

  /**
   * Emits true when this option should be visible and false when it should be invisible
   */
  isVisible: Observable<boolean>;

  /**
   * UI parameters when this filter is changed
   */
  changeQueryParams;

  /**
   * Subscription to unsubscribe from on destroy
   */
  sub: Subscription;

  /**
   * Link to the search page
   */
  searchLink: string;

  constructor(protected searchService: SearchService,
              protected filterService: SearchFilterService,
              protected searchConfigService: SearchConfigurationService,
              protected router: Router,
              protected paginationService: PaginationService
  ) {
  }

  /**
   * Initializes all observable instance variables and starts listening to them
   */
  ngOnInit(): void {
    this.searchLink = this.getSearchLink();
    this.isVisible = this.isChecked().pipe(map((checked: boolean) => !checked));
    this.sub = this.searchConfigService.searchOptions.subscribe(() => {
      this.updateChangeParams();
    });
  }

  /**
   * Checks if a value for this filter is currently active
   */
  private isChecked(): Observable<boolean> {
    return this.filterService.isFilterActiveWithValue(this.filterConfig.paramName, this.filterValue.value);
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
   * Calculates the parameters that should change if a given values for this range filter would be changed
   */
  private updateChangeParams(): void {
    const parts = this.filterValue.value.split(rangeDelimiter);
    const min = parts.length > 1 ? parts[0].trim() : this.filterValue.value;
    const max = parts.length > 1 ? parts[1].trim() : this.filterValue.value;
    const page = this.paginationService.getPageParam(this.searchConfigService.paginationID);
    this.changeQueryParams = {
      [this.filterConfig.paramName + RANGE_FILTER_MIN_SUFFIX]: [min],
      [this.filterConfig.paramName + RANGE_FILTER_MAX_SUFFIX]: [max],
      [page]: 1
    };
  }

  /**
   * Make sure the subscription is unsubscribed from when this component is destroyed
   */
  ngOnDestroy(): void {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }
}
