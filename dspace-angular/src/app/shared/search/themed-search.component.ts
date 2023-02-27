import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ThemedComponent } from '../theme-support/themed.component';
import { SearchComponent } from './search.component';
import { SearchConfigurationOption } from './search-switch-configuration/search-configuration-option.model';
import { Context } from '../../core/shared/context.model';
import { CollectionElementLinkType } from '../object-collection/collection-element-link.type';
import { SelectionConfig } from './search-results/search-results.component';
import { ViewMode } from '../../core/shared/view-mode.model';
import { SearchObjects } from './models/search-objects.model';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { ListableObject } from '../object-collection/shared/listable-object.model';

/**
 * Themed wrapper for SearchComponent
 */
@Component({
  selector: 'ds-themed-search',
  styleUrls: [],
  templateUrl: '../theme-support/themed.component.html',
})
export class ThemedSearchComponent extends ThemedComponent<SearchComponent> {
  protected inAndOutputNames: (keyof SearchComponent & keyof this)[] = ['configurationList', 'context', 'configuration', 'fixedFilterQuery', 'useCachedVersionIfAvailable', 'inPlaceSearch', 'linkType', 'paginationId', 'searchEnabled', 'sideBarWidth', 'searchFormPlaceholder', 'selectable', 'selectionConfig', 'showCsvExport', 'showSidebar', 'showViewModes', 'useUniquePageId', 'viewModeList', 'showScopeSelector', 'resultFound', 'deselectObject', 'selectObject', 'trackStatistics'];

  @Input() configurationList: SearchConfigurationOption[] = [];

  @Input() context: Context = Context.Search;

  @Input() configuration = 'default';

  @Input() fixedFilterQuery: string;

  @Input() useCachedVersionIfAvailable = true;

  @Input() inPlaceSearch = true;

  @Input() linkType: CollectionElementLinkType;

  @Input() paginationId = 'spc';

  @Input() searchEnabled = true;

  @Input() sideBarWidth = 3;

  @Input() searchFormPlaceholder = 'search.search-form.placeholder';

  @Input() selectable = false;

  @Input() selectionConfig: SelectionConfig;

  @Input() showCsvExport = false;

  @Input() showSidebar = true;

  @Input() showViewModes = true;

  @Input() useUniquePageId: false;

  @Input() viewModeList: ViewMode[];

  @Input() showScopeSelector = true;

  @Input() trackStatistics = false;

  @Output() resultFound: EventEmitter<SearchObjects<DSpaceObject>> = new EventEmitter<SearchObjects<DSpaceObject>>();

  @Output() deselectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  @Output() selectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  protected getComponentName(): string {
    return 'SearchComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../themes/${themeName}/app/shared/search/search.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./search.component');
  }
}
