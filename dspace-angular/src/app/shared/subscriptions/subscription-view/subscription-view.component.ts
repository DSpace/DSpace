import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Subscription } from '../models/subscription.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';

import { take } from 'rxjs/operators';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

import { hasValue } from '../../empty.util';
import { ConfirmationModalComponent } from '../../confirmation-modal/confirmation-modal.component';
import { SubscriptionsDataService } from '../subscriptions-data.service';
import { getCommunityModuleRoute } from '../../../community-page/community-page-routing-paths';
import { getCollectionModuleRoute } from '../../../collection-page/collection-page-routing-paths';
import { getItemModuleRoute } from '../../../item-page/item-page-routing-paths';
import { SubscriptionModalComponent } from '../subscription-modal/subscription-modal.component';

@Component({
  // eslint-disable-next-line @angular-eslint/component-selector
  selector: '[ds-subscription-view]',
  templateUrl: './subscription-view.component.html',
  styleUrls: ['./subscription-view.component.scss']
})
/**
 * Table row representing a subscription that displays all information and action buttons to manage it
 */
export class SubscriptionViewComponent {

  /**
   * Subscription to be rendered
   */
  @Input() subscription: Subscription;

  /**
   * DSpaceObject of the subscription
   */
  @Input() dso: DSpaceObject;

  /**
   * EPerson of the subscription
   */
  @Input() eperson: string;

  /**
   * When an action is made emit a reload event
   */
  @Output() reload = new EventEmitter();

  /**
   * Reference to NgbModal
   */
  public modalRef: NgbModalRef;

  constructor(
    private modalService: NgbModal,
    private subscriptionService: SubscriptionsDataService,
  ) { }

  /**
   * Return the prefix of the route to the dso object page ( e.g. "items")
   */
  getPageRoutePrefix(): string {
    let routePrefix;
    switch (this.dso.type.toString()) {
      case 'community':
        routePrefix = getCommunityModuleRoute();
        break;
      case 'collection':
        routePrefix = getCollectionModuleRoute();
        break;
      case 'item':
        routePrefix = getItemModuleRoute();
        break;
    }
    return routePrefix;
  }

  /**
   * Deletes Subscription, show notification on success/failure & updates list
   * @param subscription Subscription to be deleted
   */
  deleteSubscriptionPopup(subscription: Subscription) {
    if (hasValue(subscription.id)) {
      const modalRef = this.modalService.open(ConfirmationModalComponent);
      modalRef.componentInstance.dso = this.dso;
      modalRef.componentInstance.headerLabel = 'confirmation-modal.delete-subscription.header';
      modalRef.componentInstance.infoLabel = 'confirmation-modal.delete-subscription.info';
      modalRef.componentInstance.cancelLabel = 'confirmation-modal.delete-subscription.cancel';
      modalRef.componentInstance.confirmLabel = 'confirmation-modal.delete-subscription.confirm';
      modalRef.componentInstance.brandColor = 'danger';
      modalRef.componentInstance.confirmIcon = 'fas fa-trash';
      modalRef.componentInstance.response.pipe(take(1)).subscribe((confirm: boolean) => {
        if (confirm) {
          this.subscriptionService.deleteSubscription(subscription.id).subscribe( (res) => {
            this.reload.emit();
          });
        }
      });
    }
  }

  public openSubscriptionModal() {
    this.modalRef = this.modalService.open(SubscriptionModalComponent);
    this.modalRef.componentInstance.dso = this.dso;
    this.modalRef.componentInstance.subscription = this.subscription;
    this.modalRef.componentInstance.updateSubscription.pipe(take(1)).subscribe((subscription: Subscription) => {
      this.subscription = subscription;
    });

  }
}
