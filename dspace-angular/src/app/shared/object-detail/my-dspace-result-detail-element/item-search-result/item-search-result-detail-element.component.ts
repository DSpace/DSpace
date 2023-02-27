import { Component } from '@angular/core';

import { ViewMode } from '../../../../core/shared/view-mode.model';
import { Item } from '../../../../core/shared/item.model';
import { SearchResultDetailElementComponent } from '../search-result-detail-element.component';
import {
  MyDspaceItemStatusType
} from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { listableObjectComponent } from '../../../object-collection/shared/listable-object/listable-object.decorator';
import { ItemSearchResult } from '../../../object-collection/shared/item-search-result.model';
import { Context } from '../../../../core/shared/context.model';

/**
 * This component renders item object for the search result in the detail view.
 */
@Component({
  selector: 'ds-item-search-result-detail-element',
  styleUrls: ['../search-result-detail-element.component.scss', './item-search-result-detail-element.component.scss'],
  templateUrl: './item-search-result-detail-element.component.html'
})

@listableObjectComponent(ItemSearchResult, ViewMode.DetailedListElement, Context.Workspace)
@listableObjectComponent(ItemSearchResult, ViewMode.DetailedListElement, Context.Workflow)
export class ItemSearchResultDetailElementComponent extends SearchResultDetailElementComponent<ItemSearchResult, Item> {

  /**
   * Represent item's status
   */
  public status = MyDspaceItemStatusType.ARCHIVED;

}
