import { Component, Inject, OnInit } from '@angular/core';

import { BehaviorSubject, Observable } from 'rxjs';
import { map, mergeMap, take, tap } from 'rxjs/operators';

import { ViewMode } from '../../../../../core/shared/view-mode.model';
import {
  listableObjectComponent
} from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../../core/shared/context.model';
import { WorkspaceItem } from '../../../../../core/submission/models/workspaceitem.model';
import { LinkService } from '../../../../../core/cache/builders/link.service';
import { followLink } from '../../../../../shared/utils/follow-link-config.model';
import { RemoteData } from '../../../../../core/data/remote-data';
import {
  getAllSucceededRemoteData,
  getFirstCompletedRemoteData,
  getRemoteDataPayload
} from '../../../../../core/shared/operators';
import { Item } from '../../../../../core/shared/item.model';
import {
  SearchResultListElementComponent
} from '../../../../../shared/object-list/search-result-list-element/search-result-list-element.component';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { APP_CONFIG, AppConfig } from '../../../../../../config/app-config.interface';
import {
  WorkspaceItemSearchResult
} from '../../../../../shared/object-collection/shared/workspace-item-search-result.model';
import { SupervisionOrder } from '../../../../../core/supervision-order/models/supervision-order.model';
import { SupervisionOrderDataService } from '../../../../../core/supervision-order/supervision-order-data.service';
import { PaginatedList } from '../../../../../core/data/paginated-list.model';
import { DSpaceObject } from '../../../../../core/shared/dspace-object.model';

@listableObjectComponent(WorkspaceItemSearchResult, ViewMode.ListElement, Context.AdminWorkflowSearch)
@Component({
  selector: 'ds-workflow-item-search-result-admin-workflow-list-element',
  styleUrls: ['./workspace-item-search-result-admin-workflow-list-element.component.scss'],
  templateUrl: './workspace-item-search-result-admin-workflow-list-element.component.html'
})
/**
 * The component for displaying a list element for a workflow item on the admin workflow search page
 */
export class WorkspaceItemSearchResultAdminWorkflowListElementComponent extends SearchResultListElementComponent<WorkspaceItemSearchResult, WorkspaceItem> implements OnInit {

  /**
   * The item linked to the workflow item
   */
  public item$: Observable<Item>;

  /**
   * The id of the item linked to the workflow item
   */
  public itemId: string;

  /**
   * The supervision orders linked to the workflow item
   */
  public supervisionOrder$: BehaviorSubject<SupervisionOrder[]> = new BehaviorSubject<SupervisionOrder[]>([]);

  constructor(private linkService: LinkService,
              protected dsoNameService: DSONameService,
              protected supervisionOrderDataService: SupervisionOrderDataService,
              protected truncatableService: TruncatableService,
              @Inject(APP_CONFIG) protected appConfig: AppConfig
  ) {
    super(truncatableService, dsoNameService, appConfig);
  }

  /**
   * Initialize the item object from the workflow item
   */
  ngOnInit(): void {
    super.ngOnInit();
    this.dso = this.linkService.resolveLink(this.dso, followLink('item'));
    this.item$ = (this.dso.item as Observable<RemoteData<Item>>).pipe(getAllSucceededRemoteData(), getRemoteDataPayload());

    this.item$.pipe(
      take(1),
      tap((item: Item) => this.itemId = item.id),
      mergeMap((item: Item) => this.retrieveSupervisorOrders(item.id))
    ).subscribe((supervisionOrderList: SupervisionOrder[]) => {
      this.supervisionOrder$.next(supervisionOrderList);
    });
  }

  /**
   * Retrieve the list of SupervisionOrder object related to the given item
   *
   * @param itemId
   * @private
   */
  private retrieveSupervisorOrders(itemId): Observable<SupervisionOrder[]> {
    return this.supervisionOrderDataService.searchByItem(
      itemId, false, true, followLink('group')
    ).pipe(
      getFirstCompletedRemoteData(),
      map((soRD: RemoteData<PaginatedList<SupervisionOrder>>) => soRD.hasSucceeded && !soRD.hasNoContent ? soRD.payload.page : [])
    );
  }

  /**
   * Reload list element after supervision order change.
   */
  reloadObject(dso: DSpaceObject) {
    this.reloadedObject.emit(dso);
  }

}
