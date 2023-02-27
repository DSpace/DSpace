import { Component, Input } from '@angular/core';
import { ThemedComponent } from '../shared/theme-support/themed.component';
import { ConfigurationSearchPageComponent } from './configuration-search-page.component';
import { Observable } from 'rxjs';
import { Context } from '../core/shared/context.model';

/**
 * Themed wrapper for ConfigurationSearchPageComponent
 */
@Component({
  selector: 'ds-themed-configuration-search-page',
  styleUrls: [],
  templateUrl: '../shared/theme-support/themed.component.html',
})
export class ThemedConfigurationSearchPageComponent extends ThemedComponent<ConfigurationSearchPageComponent> {
  /**
   * The configuration to use for the search options
   * If empty, the configuration will be determined by the route parameter called 'configuration'
   */
  @Input() configuration: string;

  /**
   * The actual query for the fixed filter.
   * If empty, the query will be determined by the route parameter called 'filter'
   */
  @Input() fixedFilterQuery: string;

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch = true;

  /**
   * Whether or not the search bar should be visible
   */
  @Input()
  searchEnabled = true;

  /**
   * The width of the sidebar (bootstrap columns)
   */
  @Input()
  sideBarWidth = 3;

  /**
   * The currently applied configuration (determines title of search)
   */
  @Input()
  configuration$: Observable<string>;

  /**
   * The current context
   */
  @Input()
  context: Context;

  protected inAndOutputNames: (keyof ConfigurationSearchPageComponent & keyof this)[] =
    ['context', 'configuration', 'fixedFilterQuery', 'inPlaceSearch', 'searchEnabled', 'sideBarWidth'];

  protected getComponentName(): string {
    return 'ConfigurationSearchPageComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../themes/${themeName}/app/search-page/configuration-search-page.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import(`./configuration-search-page.component`);
  }

}
