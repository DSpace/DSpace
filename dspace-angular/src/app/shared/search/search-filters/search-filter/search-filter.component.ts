import { Component, Inject, Input, OnInit } from '@angular/core';

import { BehaviorSubject, Observable, of as observableOf } from 'rxjs';
import { filter, map, startWith, switchMap, take } from 'rxjs/operators';

import { SearchFilterConfig } from '../../models/search-filter-config.model';
import { SearchFilterService } from '../../../../core/shared/search/search-filter.service';
import { slide } from '../../../animations/slide';
import { isNotEmpty } from '../../../empty.util';
import { SearchService } from '../../../../core/shared/search/search.service';
import { SearchConfigurationService } from '../../../../core/shared/search/search-configuration.service';
import { SEARCH_CONFIG_SERVICE } from '../../../../my-dspace-page/my-dspace-page.component';
import { SequenceService } from '../../../../core/shared/sequence.service';

@Component({
  selector: 'ds-search-filter',
  styleUrls: ['./search-filter.component.scss'],
  templateUrl: './search-filter.component.html',
  animations: [slide],
})

/**
 * Represents a part of the filter section for a single type of filter
 */
export class SearchFilterComponent implements OnInit {
  /**
   * The filter config for this component
   */
  @Input() filter: SearchFilterConfig;

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;

  /**
   * Emits when the search filters values may be stale, and so they must be refreshed.
   */
  @Input() refreshFilters: BehaviorSubject<boolean>;

  /**
   * True when the filter is 100% collapsed in the UI
   */
  closed: boolean;

  /**
   * True when the filter controls should be hidden & removed from the tablist
   */
  notab: boolean;

  /**
   * True when the filter toggle button is focused
   */
  focusBox = false;

  /**
   * Emits true when the filter is currently collapsed in the store
   */
  collapsed$: Observable<boolean>;

  /**
   * Emits all currently selected values for this filter
   */
  selectedValues$: Observable<string[]>;

  /**
   * Emits true when the current filter is supposed to be shown
   */
  active$: Observable<boolean>;

  private readonly sequenceId: number;

  constructor(
    private filterService: SearchFilterService,
    private searchService: SearchService,
    @Inject(SEARCH_CONFIG_SERVICE) private searchConfigService: SearchConfigurationService,
    private sequenceService: SequenceService,
  ) {
    this.sequenceId = this.sequenceService.next();
  }

  /**
   * Requests the current set values for this filter
   * If the filter config is open by default OR the filter has at least one value, the filter should be initially expanded
   * Else, the filter should initially be collapsed
   */
  ngOnInit() {
    this.selectedValues$ = this.getSelectedValues();
    this.active$ = this.isActive();
    this.collapsed$ = this.isCollapsed();
    this.initializeFilter();
    this.selectedValues$.pipe(take(1)).subscribe((selectedValues) => {
      if (isNotEmpty(selectedValues)) {
        this.filterService.expand(this.filter.name);
      }
    });
  }

  /**
   *  Changes the state for this filter to collapsed when it's expanded and to expanded it when it's collapsed
   */
  toggle() {
    this.filterService.toggle(this.filter.name);
  }

  /**
   * Checks if the filter is currently collapsed
   * @returns {Observable<boolean>} Emits true when the current state of the filter is collapsed, false when it's expanded
   */
  private isCollapsed(): Observable<boolean> {
    return this.filterService.isCollapsed(this.filter.name);
  }

  /**
   *  Sets the initial state of the filter
   */
  initializeFilter() {
    this.filterService.initializeFilter(this.filter);
  }

  /**
   * @returns {Observable<string[]>} Emits a list of all values that are currently active for this filter
   */
  private getSelectedValues(): Observable<string[]> {
    return this.filterService.getSelectedValuesForFilter(this.filter);
  }

  /**
   * Method to change this.collapsed to false when the slide animation ends and is sliding open
   * @param event The animation event
   */
  finishSlide(event: any): void {
    if (event.fromState === 'collapsed') {
      this.closed = false;
    }
    if (event.toState === 'collapsed') {
      this.notab = true;
    }
  }

  /**
   * Method to change this.collapsed to true when the slide animation starts and is sliding closed
   * @param event The animation event
   */
  startSlide(event: any): void {
    if (event.toState === 'collapsed') {
      this.closed = true;
    }
    if (event.fromState === 'collapsed') {
      this.notab = false;
    }
  }

  get regionId(): string {
    return `search-filter-region-${this.sequenceId}`;
  }

  get toggleId(): string {
    return `search-filter-toggle-${this.sequenceId}`;
  }

  /**
   * Check if a given filter is supposed to be shown or not
   * @returns {Observable<boolean>} Emits true whenever a given filter config should be shown
   */
  private isActive(): Observable<boolean> {
    return this.selectedValues$.pipe(
      switchMap((isActive) => {
        if (isNotEmpty(isActive)) {
          return observableOf(true);
        } else {
          return this.searchConfigService.searchOptions.pipe(
            switchMap((options) => {
                return this.searchService.getFacetValuesFor(this.filter, 1, options).pipe(
                  filter((RD) => !RD.isLoading),
                  map((valuesRD) => {
                    return valuesRD.payload?.totalElements > 0;
                  }),);
              }
            ));
        }
      }),
      startWith(true));
  }
}
