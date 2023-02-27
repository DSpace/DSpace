import { Component, OnInit } from '@angular/core';

import { Item } from '../../../../core/shared/item.model';
import { MyDspaceItemStatusType } from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../object-collection/shared/item-search-result.model';
import { SearchResultListElementComponent } from '../../search-result-list-element/search-result-list-element.component';

/**
 * This component renders item object for the search result in the list view for submission.
 */
@Component({
  selector: 'ds-item-search-result-list-element-submission',
  styleUrls: ['../../search-result-list-element/search-result-list-element.component.scss', './item-search-result-list-element-submission.component.scss'],
  templateUrl: './item-search-result-list-element-submission.component.html'
})

@listableObjectComponent(ItemSearchResult, ViewMode.ListElement, Context.Workspace)
@listableObjectComponent(ItemSearchResult, ViewMode.ListElement, Context.Workflow)
export class ItemSearchResultListElementSubmissionComponent extends SearchResultListElementComponent<ItemSearchResult, Item> implements OnInit {
  /**
   * Represent item's status
   */
  public status = MyDspaceItemStatusType.ARCHIVED;

  /**
   * Display thumbnails if required by configuration
   */
  showThumbnails: boolean;

  ngOnInit() {
    super.ngOnInit();
    this.showThumbnails = this.appConfig.browseBy.showThumbnails;
  }
}
