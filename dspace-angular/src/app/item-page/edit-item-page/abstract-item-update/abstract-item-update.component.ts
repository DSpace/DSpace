import { Component, OnInit, OnDestroy, Input } from '@angular/core';
import { combineLatest as observableCombineLatest, Observable, Subscription } from 'rxjs';
import { Item } from '../../../core/shared/item.model';
import { ItemDataService } from '../../../core/data/item-data.service';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { ActivatedRoute, Router, Data } from '@angular/router';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { first, map, switchMap, tap } from 'rxjs/operators';
import { RemoteData } from '../../../core/data/remote-data';
import { AbstractTrackableComponent } from '../../../shared/trackable/abstract-trackable.component';
import { environment } from '../../../../environments/environment';
import { getItemPageRoute } from '../../item-page-routing-paths';
import { getAllSucceededRemoteData } from '../../../core/shared/operators';
import { hasValue } from '../../../shared/empty.util';
import { ITEM_PAGE_LINKS_TO_FOLLOW } from '../../item.resolver';
import { FieldUpdate } from '../../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../../core/data/object-updates/field-updates.model';

@Component({
  selector: 'ds-abstract-item-update',
  template: ''
})
/**
 * Abstract component for managing object updates of an item
 */
export class AbstractItemUpdateComponent extends AbstractTrackableComponent implements OnInit, OnDestroy {
  /**
   * The item to display the edit page for
   */
  @Input() item: Item;
  /**
   * The current values and updates for all this item's fields
   * Should be initialized in the initializeUpdates method of the child component
   */
  updates$: Observable<FieldUpdates>;

  /**
   * Route to the item's page
   */
  itemPageRoute: string;

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
    public route: ActivatedRoute
  ) {
    super(objectUpdatesService, notificationsService, translateService);
  }

  /**
   * Initialize common properties between item-update components
   */
  ngOnInit(): void {
    if (hasValue(this.item)) {
      this.setItem(this.item);
    } else {
      // The item wasn't provided through an input, retrieve it from the route instead.
      this.itemUpdateSubscription = observableCombineLatest([this.route.data, this.route.parent.data]).pipe(
        map(([data, parentData]: [Data, Data]) => Object.assign({}, data, parentData)),
        map((data: any) => data.dso),
        tap((rd: RemoteData<Item>) => {
          this.item = rd.payload;
        }),
        switchMap((rd: RemoteData<Item>) => {
          return this.itemService.findByHref(rd.payload._links.self.href, true, true, ...ITEM_PAGE_LINKS_TO_FOLLOW);
        }),
        getAllSucceededRemoteData()
      ).subscribe((rd: RemoteData<Item>) => {
        this.setItem(rd.payload);
      });
    }

    this.discardTimeOut = environment.item.edit.undoTimeout;
    this.url = this.router.url;
    if (this.url.indexOf('?') > 0) {
      this.url = this.url.substr(0, this.url.indexOf('?'));
    }
    this.hasChanges().pipe(first()).subscribe((hasChanges) => {
      if (!hasChanges) {
        this.initializeOriginalFields();
      } else {
        this.checkLastModified();
      }
    });

    this.initializeNotificationsPrefix();
    this.initializeUpdates();
  }

  setItem(item: Item) {
    this.item = item;
    this.itemPageRoute = getItemPageRoute(this.item);
    this.postItemInit();
    this.initializeUpdates();
  }

  ngOnDestroy() {
    if (hasValue(this.itemUpdateSubscription)) {
      this.itemUpdateSubscription.unsubscribe();
    }
  }

  /**
   * Actions to perform after the item has been initialized
   * Abstract method: Should be overwritten in the sub class
   */
  postItemInit(): void {
    // Overwrite in subclasses
  }

  /**
   * Initialize the values and updates of the current item's fields
   * Abstract method: Should be overwritten in the sub class
   */
  initializeUpdates(): void {
    // Overwrite in subclasses
  }

  /**
   * Initialize the prefix for notification messages
   * Abstract method: Should be overwritten in the sub class
   */
  initializeNotificationsPrefix(): void {
    // Overwrite in subclasses
  }

  /**
   * Sends all initial values of this item to the object updates service
   * Abstract method: Should be overwritten in the sub class
   */
  initializeOriginalFields(): void {
    // Overwrite in subclasses
  }

  /**
   * Submit the current changes
   * Abstract method: Should be overwritten in the sub class
   */
  submit(): void {
    // Overwrite in subclasses
  }

  /**
   * Prevent unnecessary rerendering so fields don't lose focus
   */
  trackUpdate(index, update: FieldUpdate) {
    return update && update.field ? update.field.uuid : undefined;
  }

  /**
   * Check if the current page is entirely valid
   */
  public isValid() {
    return this.objectUpdatesService.isValidPage(this.url);
  }

  /**
   * Checks if the current item is still in sync with the version in the store
   * If it's not, a notification is shown and the changes are removed
   */
  private checkLastModified() {
    const currentVersion = this.item.lastModified;
    this.objectUpdatesService.getLastModified(this.url).pipe(first()).subscribe(
      (updateVersion: Date) => {
        if (updateVersion.getDate() !== currentVersion.getDate()) {
          this.notificationsService.warning(this.getNotificationTitle('outdated'), this.getNotificationContent('outdated'));
          this.initializeOriginalFields();
        }
      }
    );
  }
}
