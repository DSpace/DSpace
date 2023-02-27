import { ThemedComponent } from '../../theme-support/themed.component';
import { SearchResultsComponent, SelectionConfig } from './search-results.component';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CollectionElementLinkType } from '../../object-collection/collection-element-link.type';
import { RemoteData } from '../../../core/data/remote-data';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { SearchResult } from '../models/search-result.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { PaginatedSearchOptions } from '../models/paginated-search-options.model';
import { SortOptions } from '../../../core/cache/models/sort-options.model';
import { ViewMode } from '../../../core/shared/view-mode.model';
import { Context } from '../../../core/shared/context.model';
import { ListableObject } from '../../object-collection/shared/listable-object.model';

/**
 * Themed wrapper for SearchResultsComponent
 */
@Component({
  selector: 'ds-themed-search-results',
  styleUrls: [],
  templateUrl: '../../theme-support/themed.component.html',
})
export class ThemedSearchResultsComponent extends ThemedComponent<SearchResultsComponent> {
  protected inAndOutputNames: (keyof SearchResultsComponent & keyof this)[] = ['linkType', 'searchResults', 'searchConfig', 'showCsvExport', 'sortConfig', 'viewMode', 'configuration', 'disableHeader', 'selectable', 'context', 'hidePaginationDetail', 'selectionConfig', 'contentChange', 'deselectObject', 'selectObject'];

  @Input() linkType: CollectionElementLinkType;

  @Input() searchResults: RemoteData<PaginatedList<SearchResult<DSpaceObject>>>;

  @Input() searchConfig: PaginatedSearchOptions;

  @Input() showCsvExport = false;

  @Input() sortConfig: SortOptions;

  @Input() viewMode: ViewMode;

  @Input() configuration: string;

  @Input() disableHeader = false;

  @Input() selectable = false;

  @Input() context: Context;

  @Input() hidePaginationDetail = false;

  @Input() selectionConfig: SelectionConfig = null;

  @Output() contentChange: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  @Output() deselectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  @Output() selectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  protected getComponentName(): string {
    return 'SearchResultsComponent';
  }

  protected importThemedComponent(themeName: string): Promise<any> {
    return import(`../../../../themes/${themeName}/app/shared/search/search-results/search-results.component`);
  }

  protected importUnthemedComponent(): Promise<any> {
    return import('./search-results.component');
  }

}
