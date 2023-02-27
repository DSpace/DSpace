import { ChangeDetectorRef, Component, NgZone, OnDestroy } from '@angular/core';
import { AbstractItemUpdateComponent } from '../abstract-item-update/abstract-item-update.component';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { Observable, of as observableOf, Subscription, zip as observableZip } from 'rxjs';
import { ItemDataService } from '../../../core/data/item-data.service';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { BitstreamDataService } from '../../../core/data/bitstream-data.service';
import { hasValue, isNotEmpty } from '../../../shared/empty.util';
import { ObjectCacheService } from '../../../core/cache/object-cache.service';
import { RequestService } from '../../../core/data/request.service';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../../../core/shared/operators';
import { RemoteData } from '../../../core/data/remote-data';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { Bundle } from '../../../core/shared/bundle.model';
import { Bitstream } from '../../../core/shared/bitstream.model';
import { BundleDataService } from '../../../core/data/bundle-data.service';
import { PaginatedSearchOptions } from '../../../shared/search/models/paginated-search-options.model';
import { ResponsiveColumnSizes } from '../../../shared/responsive-table-sizes/responsive-column-sizes';
import { ResponsiveTableSizes } from '../../../shared/responsive-table-sizes/responsive-table-sizes';
import { NoContent } from '../../../core/shared/NoContent.model';
import { Operation } from 'fast-json-patch';
import { FieldUpdate } from '../../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../../core/data/object-updates/field-updates.model';
import { FieldChangeType } from '../../../core/data/object-updates/field-change-type.model';

@Component({
  selector: 'ds-item-bitstreams',
  styleUrls: ['./item-bitstreams.component.scss'],
  templateUrl: './item-bitstreams.component.html',
})
/**
 * Component for displaying an item's bitstreams edit page
 */
export class ItemBitstreamsComponent extends AbstractItemUpdateComponent implements OnDestroy {

  /**
   * The currently listed bundles
   */
  bundles$: Observable<Bundle[]>;

  /**
   * The page options to use for fetching the bundles
   */
  bundlesOptions = {
    id: 'bundles-pagination-options',
    currentPage: 1,
    pageSize: 9999
  } as any;

  /**
   * The bootstrap sizes used for the columns within this table
   */
  columnSizes = new ResponsiveTableSizes([
    // Name column
    new ResponsiveColumnSizes(2, 2, 3, 4, 4),
    // Description column
    new ResponsiveColumnSizes(2, 3, 3, 3, 3),
    // Format column
    new ResponsiveColumnSizes(2, 2, 2, 2, 2),
    // Actions column
    new ResponsiveColumnSizes(6, 5, 4, 3, 3)
  ]);

  /**
   * Are we currently submitting the changes?
   * Used to disable any action buttons until the submit finishes
   */
  submitting = false;

  /**
   * A subscription that checks when the item is deleted in cache and reloads the item by sending a new request
   * This is used to update the item in cache after bitstreams are deleted
   */
  itemUpdateSubscription: Subscription;

  constructor(
    public itemService: ItemDataService,
    public objectUpdatesService: ObjectUpdatesService,
    public router: Router,
    public notificationsService: NotificationsService,
    public translateService: TranslateService,
    public route: ActivatedRoute,
    public bitstreamService: BitstreamDataService,
    public objectCache: ObjectCacheService,
    public requestService: RequestService,
    public cdRef: ChangeDetectorRef,
    public bundleService: BundleDataService,
    public zone: NgZone
  ) {
    super(itemService, objectUpdatesService, router, notificationsService, translateService, route);
  }

  /**
   * Actions to perform after the item has been initialized
   */
  postItemInit(): void {
    this.bundles$ = this.itemService.getBundles(this.item.id, new PaginatedSearchOptions({pagination: this.bundlesOptions})).pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      map((bundlePage: PaginatedList<Bundle>) => bundlePage.page)
    );
  }

  /**
   * Initialize the notification messages prefix
   */
  initializeNotificationsPrefix(): void {
    this.notificationsPrefix = 'item.edit.bitstreams.notifications.';
  }


  /**
   * Submit the current changes
   * Bitstreams marked as deleted send out a delete request to the rest API
   * Display notifications and reset the current item/updates
   */
  submit() {
    this.submitting = true;
    const bundlesOnce$ = this.bundles$.pipe(take(1));

    // Fetch all removed bitstreams from the object update service
    const removedBitstreams$ = bundlesOnce$.pipe(
      switchMap((bundles: Bundle[]) => observableZip(
        ...bundles.map((bundle: Bundle) => this.objectUpdatesService.getFieldUpdates(bundle.self, [], true))
      )),
      map((fieldUpdates: FieldUpdates[]) => ([] as FieldUpdate[]).concat(
        ...fieldUpdates.map((updates: FieldUpdates) => Object.values(updates).filter((fieldUpdate: FieldUpdate) => fieldUpdate.changeType === FieldChangeType.REMOVE))
      )),
      map((fieldUpdates: FieldUpdate[]) => fieldUpdates.map((fieldUpdate: FieldUpdate) => fieldUpdate.field))
    );

    // Send out delete requests for all deleted bitstreams
    const removedResponses$ = removedBitstreams$.pipe(
      take(1),
      switchMap((removedBistreams: Bitstream[]) => {
        if (isNotEmpty(removedBistreams)) {
          return observableZip(...removedBistreams.map((bitstream: Bitstream) => this.bitstreamService.delete(bitstream.id)));
        } else {
          return observableOf(undefined);
        }
      })
    );

    // Perform the setup actions from above in order and display notifications
    removedResponses$.pipe(take(1)).subscribe((responses: RemoteData<NoContent>[]) => {
      this.displayNotifications('item.edit.bitstreams.notifications.remove', responses);
      this.submitting = false;
    });
  }

  /**
   * A bitstream was dropped in a new location. Send out a Move Patch request to the REST API, display notifications,
   * refresh the bundle's cache (so the lists can properly reload) and call the event's callback function (which will
   * navigate the user to the correct page)
   * @param bundle  The bundle to send patch requests to
   * @param event   The event containing the index the bitstream came from and was dropped to
   */
  dropBitstream(bundle: Bundle, event: any) {
    this.zone.runOutsideAngular(() => {
      if (hasValue(event) && hasValue(event.fromIndex) && hasValue(event.toIndex) && hasValue(event.finish)) {
        const moveOperation = {
          op: 'move',
          from: `/_links/bitstreams/${event.fromIndex}/href`,
          path: `/_links/bitstreams/${event.toIndex}/href`
        } as Operation;
        this.bundleService.patch(bundle, [moveOperation]).pipe(take(1)).subscribe((response: RemoteData<Bundle>) => {
          this.zone.run(() => {
            this.displayNotifications('item.edit.bitstreams.notifications.move', [response]);
            // Remove all cached requests from this bundle and call the event's callback when the requests are cleared
            this.requestService.removeByHrefSubstring(bundle.self).pipe(
              filter((isCached) => isCached),
              take(1)
            ).subscribe(() => event.finish());
          });
        });
      }
    });
  }

  /**
   * Display notifications
   * - Error notification for each failed response with their message
   * - Success notification in case there's at least one successful response
   * @param key       The i18n key for the notification messages
   * @param responses The returned responses to display notifications for
   */
  displayNotifications(key: string, responses: RemoteData<any>[]) {
    if (isNotEmpty(responses)) {
      const failedResponses = responses.filter((response: RemoteData<Bundle>) => hasValue(response) && response.hasFailed);
      const successfulResponses = responses.filter((response: RemoteData<Bundle>) => hasValue(response) && response.hasSucceeded);

      failedResponses.forEach((response: RemoteData<Bundle>) => {
        this.notificationsService.error(this.translateService.instant(`${key}.failed.title`), response.errorMessage);
      });
      if (successfulResponses.length > 0) {
        this.notificationsService.success(this.translateService.instant(`${key}.saved.title`), this.translateService.instant(`${key}.saved.content`));
      }
    }
  }

  /**
   * Request the object updates service to discard all current changes to this item
   * Shows a notification to remind the user that they can undo this
   */
  discard() {
    const undoNotification = this.notificationsService.info(this.getNotificationTitle('discarded'), this.getNotificationContent('discarded'), {timeOut: this.discardTimeOut});
    this.objectUpdatesService.discardAllFieldUpdates(this.url, undoNotification);
  }

  /**
   * Request the object updates service to undo discarding all changes to this item
   */
  reinstate() {
    this.bundles$.pipe(take(1)).subscribe((bundles: Bundle[]) => {
      bundles.forEach((bundle: Bundle) => {
        this.objectUpdatesService.reinstateFieldUpdates(bundle.self);
      });
    });
  }

  /**
   * Checks whether or not the object is currently reinstatable
   */
  isReinstatable(): Observable<boolean> {
    return this.bundles$.pipe(
      switchMap((bundles: Bundle[]) => observableZip(...bundles.map((bundle: Bundle) => this.objectUpdatesService.isReinstatable(bundle.self)))),
      map((reinstatable: boolean[]) => reinstatable.includes(true))
    );
  }

  /**
   * Checks whether or not there are currently updates for this object
   */
  hasChanges(): Observable<boolean> {
    return this.bundles$.pipe(
      switchMap((bundles: Bundle[]) => observableZip(...bundles.map((bundle: Bundle) => this.objectUpdatesService.hasUpdates(bundle.self)))),
      map((hasChanges: boolean[]) => hasChanges.includes(true))
    );
  }

  /**
   * Unsubscribe from open subscriptions whenever the component gets destroyed
   */
  ngOnDestroy(): void {
    if (this.itemUpdateSubscription) {
      this.itemUpdateSubscription.unsubscribe();
    }
  }
}
