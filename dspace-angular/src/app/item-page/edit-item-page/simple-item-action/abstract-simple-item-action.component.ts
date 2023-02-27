import { Component, OnInit, Predicate } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { ItemDataService } from '../../../core/data/item-data.service';
import { TranslateService } from '@ngx-translate/core';
import { Item } from '../../../core/shared/item.model';
import { RemoteData } from '../../../core/data/remote-data';
import { Observable } from 'rxjs';
import { getFirstSucceededRemoteData } from '../../../core/shared/operators';
import { first, map } from 'rxjs/operators';
import { findSuccessfulAccordingTo } from '../edit-item-operators';
import { getItemEditRoute, getItemPageRoute } from '../../item-page-routing-paths';

/**
 * Component to render and handle simple item edit actions such as withdrawal and reinstatement.
 * This component is not meant to be used itself but to be extended.
 */
@Component({
  selector: 'ds-simple-action',
  templateUrl: './abstract-simple-item-action.component.html'
})
export class AbstractSimpleItemActionComponent implements OnInit {

  itemRD$: Observable<RemoteData<Item>>;
  item: Item;

  protected messageKey: string;
  confirmMessage: string;
  cancelMessage: string;
  headerMessage: string;
  descriptionMessage: string;

  /**
   * Route to the item's page
   */
  itemPageRoute: string;

  protected predicate: Predicate<RemoteData<Item>>;

  constructor(protected route: ActivatedRoute,
              protected router: Router,
              protected notificationsService: NotificationsService,
              protected itemDataService: ItemDataService,
              protected translateService: TranslateService) {
  }

  ngOnInit(): void {
    this.itemRD$ = this.route.data.pipe(
      map((data) => data.dso),
      getFirstSucceededRemoteData()
    )as Observable<RemoteData<Item>>;

    this.itemRD$.pipe(first()).subscribe((rd) => {
        this.item = rd.payload;
        this.itemPageRoute = getItemPageRoute(this.item);
      }
    );

    this.confirmMessage = 'item.edit.' + this.messageKey + '.confirm';
    this.cancelMessage = 'item.edit.' + this.messageKey + '.cancel';
    this.headerMessage = 'item.edit.' + this.messageKey + '.header';
    this.descriptionMessage = 'item.edit.' + this.messageKey + '.description';
  }
  /**
   * Perform the operation linked to this action
   */
  performAction() {
    // Overwrite in subclasses
  }

  /**
   * Process the response obtained during the performAction method and navigate back to the edit page
   * @param response from the action in the performAction method
   */
  processRestResponse(response: RemoteData<any>) {
    if (response.hasSucceeded) {
      this.itemDataService.findById(this.item.id).pipe(
        findSuccessfulAccordingTo(this.predicate)).subscribe(() => {
        this.notificationsService.success(this.translateService.get('item.edit.' + this.messageKey + '.success'));
        this.router.navigate([getItemEditRoute(this.item)]);
      });
    } else {
      this.notificationsService.error(this.translateService.get('item.edit.' + this.messageKey + '.error'));
      this.router.navigate([getItemEditRoute(this.item)]);
    }
  }

}
