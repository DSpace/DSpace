import { Component, Inject, Input } from '@angular/core';
import { SEARCH_CONFIG_SERVICE } from '../../../my-dspace-page/my-dspace-page.component';
import { Observable } from 'rxjs';
import { Params, Router } from '@angular/router';
import { SearchConfigurationService } from '../../../core/shared/search/search-configuration.service';
import { map } from 'rxjs/operators';
import { stripOperatorFromFilterValue } from '../search.utils';

@Component({
  selector: 'ds-search-labels',
  styleUrls: ['./search-labels.component.scss'],
  templateUrl: './search-labels.component.html',
})

/**
 * Component that represents the labels containing the currently active filters
 */
export class SearchLabelsComponent {
  /**
   * Emits the currently active filters
   */
  appliedFilters: Observable<Params>;

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;

  /**
   * Initialize the instance variable
   */
  constructor(
    protected router: Router,
    @Inject(SEARCH_CONFIG_SERVICE) public searchConfigService: SearchConfigurationService) {
    this.appliedFilters = this.searchConfigService.getCurrentFrontendFilters().pipe(
      map((params) => {
        const labels = {};
        Object.keys(params)
          .forEach((key) => {
            labels[key] = [...params[key].map((value) => stripOperatorFromFilterValue(value))];
          });
        return labels;
      })
    );
  }
}
