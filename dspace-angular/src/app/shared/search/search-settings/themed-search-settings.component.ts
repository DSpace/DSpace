import { Component, Input } from '@angular/core';
import { ThemedComponent } from '../../theme-support/themed.component';
import { SearchSettingsComponent } from './search-settings.component';
import { SortOptions } from '../../../core/cache/models/sort-options.model';

/**
 * Themed wrapper for SearchSettingsComponent
 */
@Component({
  selector: 'ds-themed-search-settings',
  styleUrls: [],
  templateUrl: '../../theme-support/themed.component.html',
})
export class ThemedSearchSettingsComponent extends ThemedComponent<SearchSettingsComponent> {
  @Input() currentSortOption: SortOptions;
  @Input() sortOptionsList: SortOptions[];


  protected inAndOutputNames: (keyof SearchSettingsComponent & keyof this)[] = [
    'currentSortOption', 'sortOptionsList'];

  protected getComponentName(): string {
    return 'SearchSettingsComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../themes/${themeName}/app/shared/search/search-settings/search-settings.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./search-settings.component');
  }
}
