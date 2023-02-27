import { Component, EventEmitter, Inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';

import { Subscription } from 'rxjs';

import { hasValue } from '../../empty.util';
import { SEARCH_CONFIG_SERVICE } from '../../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationService } from '../../../core/shared/search/search-configuration.service';
import { MyDSpaceConfigurationValueType } from '../../../my-dspace-page/my-dspace-configuration-value-type';
import { SearchConfigurationOption } from './search-configuration-option.model';
import { SearchService } from '../../../core/shared/search/search.service';
import { currentPath } from '../../utils/route.utils';
import findIndex from 'lodash/findIndex';

@Component({
  selector: 'ds-search-switch-configuration',
  styleUrls: ['./search-switch-configuration.component.scss'],
  templateUrl: './search-switch-configuration.component.html',
})
/**
 * Represents a select that allow to switch over available search configurations
 */
export class SearchSwitchConfigurationComponent implements OnDestroy, OnInit {

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;
  /**
   * The list of available configuration options
   */
  @Input() configurationList: SearchConfigurationOption[] = [];
  /**
   * The default configuration to use if no defined
   */
  @Input() defaultConfiguration: string;
  /**
   * The selected option
   */
  public selectedOption: SearchConfigurationOption;

  /**
   * Subscription to unsubscribe from
   */
  private sub: Subscription;

  /**
   * Emits event when the user select a new configuration
   */
  @Output() changeConfiguration: EventEmitter<SearchConfigurationOption> = new EventEmitter<SearchConfigurationOption>();

  constructor(private router: Router,
              private searchService: SearchService,
              @Inject(SEARCH_CONFIG_SERVICE) private searchConfigService: SearchConfigurationService) {
  }

  /**
   * Init current configuration
   */
  ngOnInit() {
    this.searchConfigService.getCurrentConfiguration(this.defaultConfiguration)
      .subscribe((currentConfiguration) => {
        const index = findIndex(this.configurationList, {value: currentConfiguration });
        this.selectedOption = this.configurationList[index];
      });
  }

  /**
   * Init current configuration
   */
  onSelect() {
    const navigationExtras: NavigationExtras = {
      queryParams: {configuration: this.selectedOption.value},
    };

    this.changeConfiguration.emit(this.selectedOption);
    this.router.navigate(this.getSearchLinkParts(), navigationExtras);
  }

  /**
   * Define the select 'compareWith' method to tell Angular how to compare the values
   *
   * @param item1
   * @param item2
   */
  compare(item1: MyDSpaceConfigurationValueType, item2: MyDSpaceConfigurationValueType) {
    return item1 === item2;
  }

  /**
   * Make sure the subscription is unsubscribed from when this component is destroyed
   */
  ngOnDestroy() {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }

  /**
   * @returns {string} The base path to the search page, or the current page when inPlaceSearch is true
   */
  public getSearchLink(): string {
    if (this.inPlaceSearch) {
      return currentPath(this.router);
    }
    return this.searchService.getSearchLink();
  }

  /**
   * @returns {string[]} The base path to the search page, or the current page when inPlaceSearch is true, split in separate pieces
   */
  public getSearchLinkParts(): string[] {
    if (this.searchService) {
      return [];
    }
    return this.getSearchLink().split('/');
  }
}
