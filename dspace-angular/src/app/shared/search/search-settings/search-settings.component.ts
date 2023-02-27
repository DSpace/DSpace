import { Component, Inject, Input } from '@angular/core';
import { SearchService } from '../../../core/shared/search/search.service';
import { SortDirection, SortOptions } from '../../../core/cache/models/sort-options.model';
import { ActivatedRoute, Router } from '@angular/router';
import { SearchConfigurationService } from '../../../core/shared/search/search-configuration.service';
import { SEARCH_CONFIG_SERVICE } from '../../../my-dspace-page/my-dspace-page.component';
import { PaginationService } from '../../../core/pagination/pagination.service';

@Component({
  selector: 'ds-search-settings',
  styleUrls: ['./search-settings.component.scss'],
  templateUrl: './search-settings.component.html'
})

/**
 * This component represents the part of the search sidebar that contains the general search settings.
 */
export class SearchSettingsComponent {
  /**
   * The current sort option used
   */
  @Input() currentSortOption: SortOptions;

  /**
   * All sort options that are shown in the settings
   */
  @Input() sortOptionsList: SortOptions[];

  constructor(private service: SearchService,
              private route: ActivatedRoute,
              private router: Router,
              private paginationService: PaginationService,
              @Inject(SEARCH_CONFIG_SERVICE) public searchConfigurationService: SearchConfigurationService) {
  }

  /**
   * Method to change the current sort field and direction
   * @param {Event} event Change event containing the sort direction and sort field
   */
  reloadOrder(event: Event) {
    const values = (event.target as HTMLInputElement).value.split(',');
    this.paginationService.updateRoute(this.searchConfigurationService.paginationID, {
      sortField: values[0],
      sortDirection: values[1] as SortDirection,
      page: 1
    });
  }
}
