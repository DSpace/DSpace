import { Component, Inject } from '@angular/core';

import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { LinkService } from '../../../../core/cache/builders/link.service';
import { Item } from '../../../../core/shared/item.model';

import { ViewMode } from '../../../../core/shared/view-mode.model';
import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';
import { listableObjectComponent } from '../../../object-collection/shared/listable-object/listable-object.decorator';
import { MyDspaceItemStatusType } from '../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { WorkflowItemSearchResult } from '../../../object-collection/shared/workflow-item-search-result.model';
import { TruncatableService } from '../../../truncatable/truncatable.service';
import { followLink } from '../../../utils/follow-link-config.model';
import { SearchResultListElementComponent } from '../../search-result-list-element/search-result-list-element.component';
import { DSONameService } from '../../../../core/breadcrumbs/dso-name.service';
import { APP_CONFIG, AppConfig } from '../../../../../config/app-config.interface';
import { getFirstSucceededRemoteDataPayload } from '../../../../core/shared/operators';
import { ItemSearchResult } from '../../../object-collection/shared/item-search-result.model';
import { CollectionElementLinkType } from '../../../object-collection/collection-element-link.type';

/**
 * This component renders workflowitem object for the search result in the list view.
 */
@Component({
  selector: 'ds-workflow-item-my-dspace-result-list-element',
  styleUrls: ['../../search-result-list-element/search-result-list-element.component.scss'],
  templateUrl: './workflow-item-search-result-list-element.component.html',
})

@listableObjectComponent(WorkflowItemSearchResult, ViewMode.ListElement)
export class WorkflowItemSearchResultListElementComponent extends SearchResultListElementComponent<WorkflowItemSearchResult, WorkflowItem> {
  LinkTypes = CollectionElementLinkType;

  ViewModes = ViewMode;

  /**
   * The item search result derived from the WorkspaceItemSearchResult
   */
  derivedSearchResult$: Observable<ItemSearchResult>;

  /**
   * Represent item's status
   */
  public status = MyDspaceItemStatusType.WORKFLOW;

  /**
   * Display thumbnails if required by configuration
   */
  showThumbnails: boolean;

  constructor(
    protected truncatableService: TruncatableService,
    protected linkService: LinkService,
    protected dsoNameService: DSONameService,
    @Inject(APP_CONFIG) protected appConfig: AppConfig
  ) {
    super(truncatableService, dsoNameService, appConfig);
  }

  /**
   * Initialize all instance variables
   */
  ngOnInit() {
    super.ngOnInit();
    this.deriveSearchResult();
    this.showThumbnails = this.appConfig.browseBy.showThumbnails;
  }

  private deriveSearchResult() {
    this.linkService.resolveLink(this.object.indexableObject, followLink('item'));
    this.derivedSearchResult$ = this.object.indexableObject.item.pipe(
      getFirstSucceededRemoteDataPayload(),
      map((item: Item) => {
        const result = new ItemSearchResult();
        result.indexableObject = item;
        result.hitHighlights = this.object.hitHighlights;
        return result;
      }));
  }
}
