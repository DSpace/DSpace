import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';

import { map, Observable } from 'rxjs';
import { switchMap, take, tap } from 'rxjs/operators';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';

import { Item } from '../../../../../core/shared/item.model';
import { getFirstSucceededRemoteDataPayload } from '../../../../../core/shared/operators';
import {
  SupervisionOrderGroupSelectorComponent
} from './supervision-order-group-selector/supervision-order-group-selector.component';
import {
  getWorkflowItemDeleteRoute
} from '../../../../../workflowitems-edit-page/workflowitems-edit-page-routing-paths';
import { ITEM_EDIT_AUTHORIZATIONS_PATH } from '../../../../../item-page/edit-item-page/edit-item-page.routing-paths';
import { WorkspaceItem } from '../../../../../core/submission/models/workspaceitem.model';
import { SupervisionOrder } from '../../../../../core/supervision-order/models/supervision-order.model';
import { SupervisionOrderListEntry } from './supervision-order-status/supervision-order-status.component';
import { ConfirmationModalComponent } from '../../../../../shared/confirmation-modal/confirmation-modal.component';
import { hasValue } from '../../../../../shared/empty.util';
import { NotificationsService } from '../../../../../shared/notifications/notifications.service';
import { SupervisionOrderDataService } from '../../../../../core/supervision-order/supervision-order-data.service';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { DSpaceObject } from '../../../../../core/shared/dspace-object.model';
import { getSearchResultFor } from '../../../../../shared/search/search-result-element-decorator';

@Component({
  selector: 'ds-workspace-item-admin-workflow-actions-element',
  styleUrls: ['./workspace-item-admin-workflow-actions.component.scss'],
  templateUrl: './workspace-item-admin-workflow-actions.component.html'
})
/**
 * The component for displaying the actions for a list element for a workspace-item on the admin workflow search page
 */
export class WorkspaceItemAdminWorkflowActionsComponent implements OnInit {

  /**
   * The workspace item to perform the actions on
   */
  @Input() public wsi: WorkspaceItem;

  /**
   * Whether to use small buttons or not
   */
  @Input() public small: boolean;

  /**
   * The list of supervision order object to show
   */
  @Input() supervisionOrderList: SupervisionOrder[] = [];

  /**
   * The item related to the workspace item
   */
  item: Item;

  /**
   * An array containing the route to the resource policies page
   */
  resourcePoliciesPageRoute: string[];

  /**
   * The i18n keys prefix
   * @private
   */
  private messagePrefix = 'workflow-item.search.result';

  /**
   * Event emitted when a new SupervisionOrder has been created
   */
  @Output() create: EventEmitter<DSpaceObject> = new EventEmitter<DSpaceObject>();

  /**
   * Event emitted when new SupervisionOrder has been deleted
   */
  @Output() delete: EventEmitter<DSpaceObject> = new EventEmitter<DSpaceObject>();

  /**
   * Event emitted when a new SupervisionOrder has been created
   */
  constructor(
    protected dsoNameService: DSONameService,
    protected modalService: NgbModal,
    protected notificationsService: NotificationsService,
    protected supervisionOrderDataService: SupervisionOrderDataService,
    protected translateService: TranslateService,
  ) {
  }

  ngOnInit(): void {
    const item$: Observable<Item> = this.wsi.item.pipe(
      getFirstSucceededRemoteDataPayload(),
    );

    item$.pipe(
      map((item: Item) => this.getPoliciesRoute(item))
    ).subscribe((route: string[]) => {
      this.resourcePoliciesPageRoute = route;
    });

    item$.subscribe((item: Item) => {
      this.item = item;
    });
  }

  /**
   * Returns the path to the delete page of this workflow item
   */
  getDeleteRoute(): string {
    return getWorkflowItemDeleteRoute(this.wsi.id);
  }

  /**
   * Returns the path to the administrative edit page policies tab
   */
  getPoliciesRoute(item: Item): string[] {
    return ['/items', item.uuid, 'edit', ITEM_EDIT_AUTHORIZATIONS_PATH];
  }

  /**
   * Deletes the Group from the Repository. The Group will be the only that this form is showing.
   * It'll either show a success or error message depending on whether delete was successful or not.
   */
  deleteSupervisionOrder(supervisionOrderEntry: SupervisionOrderListEntry) {
    const modalRef = this.modalService.open(ConfirmationModalComponent);
    modalRef.componentInstance.dso = supervisionOrderEntry.group;
    modalRef.componentInstance.headerLabel = this.messagePrefix + '.delete-supervision.modal.header';
    modalRef.componentInstance.infoLabel = this.messagePrefix + '.delete-supervision.modal.info';
    modalRef.componentInstance.cancelLabel = this.messagePrefix + '.delete-supervision.modal.cancel';
    modalRef.componentInstance.confirmLabel = this.messagePrefix + '.delete-supervision.modal.confirm';
    modalRef.componentInstance.brandColor = 'danger';
    modalRef.componentInstance.confirmIcon = 'fas fa-trash';
    modalRef.componentInstance.response.pipe(
      take(1),
      switchMap((confirm: boolean) => {
        if (confirm && hasValue(supervisionOrderEntry.supervisionOrder.id)) {
          return this.supervisionOrderDataService.delete(supervisionOrderEntry.supervisionOrder.id).pipe(
            take(1),
            tap((result: boolean) => {
              if (result) {
                this.notificationsService.success(
                  null,
                  this.translateService.get(
                    this.messagePrefix + '.notification.deleted.success',
                    { name: this.dsoNameService.getName(supervisionOrderEntry.group) }
                  )
                );
              } else {
                this.notificationsService.error(
                  null,
                  this.translateService.get(
                    this.messagePrefix + '.notification.deleted.failure',
                    { name: this.dsoNameService.getName(supervisionOrderEntry.group) }
                  )
                );
              }
            })
          );
        }
      })
    ).subscribe((result: boolean) => {
      if (result) {
        this.delete.emit(this.convertReloadedObject());
      }
    });
  }

  /**
   * Opens the Supervision Modal to create a supervision order
   */
  openSupervisionModal() {
    const supervisionModal: NgbModalRef = this.modalService.open(SupervisionOrderGroupSelectorComponent, {
      size: 'lg',
      backdrop: 'static'
    });
    supervisionModal.componentInstance.itemUUID = this.item.uuid;
    supervisionModal.componentInstance.create.subscribe(() => {
      this.create.emit(this.convertReloadedObject());
    });
  }

  /**
   * Convert the reloadedObject to the Type required by this dso.
   */
  private convertReloadedObject(): DSpaceObject {
    const constructor = getSearchResultFor((this.wsi as any).constructor);
    return Object.assign(new constructor(), this.wsi, {
      indexableObject: this.wsi
    });
  }
}
