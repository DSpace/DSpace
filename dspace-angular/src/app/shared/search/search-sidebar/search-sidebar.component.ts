import { Component, EventEmitter, Input, Output } from '@angular/core';

import { SearchConfigurationOption } from '../search-switch-configuration/search-configuration-option.model';
import { BehaviorSubject, Observable } from 'rxjs';
import { PaginatedSearchOptions } from '../models/paginated-search-options.model';
import { SortOptions } from '../../../core/cache/models/sort-options.model';
import { ViewMode } from '../../../core/shared/view-mode.model';
import { RemoteData } from '../../../core/data/remote-data';
import { SearchFilterConfig } from '../models/search-filter-config.model';

/**
 * This component renders a simple item page.
 * The route parameter 'id' is used to request the item it represents.
 * All fields of the item that should be displayed, are defined in its template.
 */

@Component({
  selector: 'ds-search-sidebar',
  styleUrls: ['./search-sidebar.component.scss'],
  templateUrl: './search-sidebar.component.html',
})

/**
 * Component representing the sidebar on the search page
 */
export class SearchSidebarComponent {

  /**
   * The configuration to use for the search options
   */
  @Input() configuration;

  /**
   * The list of available configuration options
   */
  @Input() configurationList: SearchConfigurationOption[];

  /**
   * The current search scope
   */
  @Input() currentScope: string;

  /**
   * The current sort option used
   */
  @Input() currentSortOption: SortOptions;

  /**
   * An observable containing configuration about which filters are shown and how they are shown
   */
  @Input() filters: Observable<RemoteData<SearchFilterConfig[]>>;

  /**
   * The total amount of results
   */
  @Input() resultCount;

  /**
   * The list of available view mode options
   */
  @Input() viewModeList: ViewMode[];

  /**
   * Whether to show the view mode switch
   */
  @Input() showViewModes = true;

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;

  /**
   * The configuration for the current paginated search results
   */
  @Input() searchOptions: PaginatedSearchOptions;

  /**
   * All sort options that are shown in the settings
   */
  @Input() sortOptionsList: SortOptions[];

  /**
   * Emits when the search filters values may be stale, and so they must be refreshed.
   */
  @Input() refreshFilters: BehaviorSubject<boolean>;

  /**
   * Emits event when the user clicks a button to open or close the sidebar
   */
  @Output() toggleSidebar = new EventEmitter<boolean>();

  /**
   * Emits event when the user select a new configuration
   */
  @Output() changeConfiguration: EventEmitter<SearchConfigurationOption> = new EventEmitter<SearchConfigurationOption>();

  /**
   * Emits event when the user select a new view mode
   */
  @Output() changeViewMode: EventEmitter<ViewMode> = new EventEmitter<ViewMode>();

}
