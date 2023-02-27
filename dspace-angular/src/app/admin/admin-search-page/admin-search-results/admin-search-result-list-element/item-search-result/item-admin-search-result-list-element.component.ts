import { Component } from '@angular/core';
import { Item } from '../../../../../core/shared/item.model';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { SearchResultListElementComponent } from '../../../../../shared/object-list/search-result-list-element/search-result-list-element.component';

@listableObjectComponent(ItemSearchResult, ViewMode.ListElement, Context.AdminSearch)
@Component({
  selector: 'ds-item-admin-search-result-list-element',
  styleUrls: ['./item-admin-search-result-list-element.component.scss'],
  templateUrl: './item-admin-search-result-list-element.component.html'
})
/**
 * The component for displaying a list element for an item search result on the admin search page
 */
export class ItemAdminSearchResultListElementComponent extends SearchResultListElementComponent<ItemSearchResult, Item> {

}
