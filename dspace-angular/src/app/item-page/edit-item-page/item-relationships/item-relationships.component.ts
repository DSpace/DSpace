import { ChangeDetectorRef, Component } from '@angular/core';
import { Item } from '../../../core/shared/item.model';
import {
  DeleteRelationship,
  RelationshipIdentifiable,
} from '../../../core/data/object-updates/object-updates.reducer';
import { map, startWith, switchMap, take } from 'rxjs/operators';
import { combineLatest as observableCombineLatest, of as observableOf, zip as observableZip, Observable } from 'rxjs';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { AbstractItemUpdateComponent } from '../abstract-item-update/abstract-item-update.component';
import { ItemDataService } from '../../../core/data/item-data.service';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { ActivatedRoute, Router } from '@angular/router';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { RelationshipDataService } from '../../../core/data/relationship-data.service';
import { RemoteData } from '../../../core/data/remote-data';
import { ObjectCacheService } from '../../../core/cache/object-cache.service';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../../../core/shared/operators';
import { RequestService } from '../../../core/data/request.service';
import { RelationshipType } from '../../../core/shared/item-relationships/relationship-type.model';
import { ItemType } from '../../../core/shared/item-relationships/item-type.model';
import { EntityTypeDataService } from '../../../core/data/entity-type-data.service';
import { Relationship } from '../../../core/shared/item-relationships/relationship.model';
import { NoContent } from '../../../core/shared/NoContent.model';
import { hasValue } from '../../../shared/empty.util';
import { FieldUpdate } from '../../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../../core/data/object-updates/field-updates.model';
import { FieldChangeType } from '../../../core/data/object-updates/field-change-type.model';
import { RelationshipTypeDataService } from '../../../core/data/relationship-type-data.service';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'ds-item-relationships',
  styleUrls: ['./item-relationships.component.scss'],
  templateUrl: './item-relationships.component.html',
})
/**
 * Component for displaying an item's relationships edit page
 */
export class ItemRelationshipsComponent extends AbstractItemUpdateComponent {


  /**
   * The allowed relationship types for this type of item as an observable list
   */
  relationshipTypes$: Observable<RelationshipType[]>;

  /**
   * The item's entity type as an observable
   */
  entityType$: Observable<ItemType>;

  constructor(
    public itemService: ItemDataService,
    public objectUpdatesService: ObjectUpdatesService,
    public router: Router,
    public notificationsService: NotificationsService,
    public translateService: TranslateService,
    public route: ActivatedRoute,
    public relationshipService: RelationshipDataService,
    public objectCache: ObjectCacheService,
    public requestService: RequestService,
    public entityTypeService: EntityTypeDataService,
    protected relationshipTypeService: RelationshipTypeDataService,
    public cdr: ChangeDetectorRef,
    protected modalService: NgbModal,
  ) {
    super(itemService, objectUpdatesService, router, notificationsService, translateService, route);
  }

  /**
   * Initialize the values and updates of the current item's relationship fields
   */
  public initializeUpdates(): void {

    const label = this.item.firstMetadataValue('dspace.entity.type');
    if (label !== undefined) {
      this.relationshipTypes$ = this.relationshipTypeService.searchByEntityType(label, true, true, ...this.getRelationshipTypeFollowLinks())
      .pipe(
        map((relationshipTypes: PaginatedList<RelationshipType>) => relationshipTypes.page)
      );

      this.entityType$ = this.entityTypeService.getEntityTypeByLabel(label).pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload(),
      );

    } else {
      this.entityType$ = observableOf(undefined);
    }
  }

  /**
   * Initialize the prefix for notification messages
   */
  public initializeNotificationsPrefix(): void {
    this.notificationsPrefix = 'item.edit.relationships.notifications.';
  }

  /**
   * Resolve the currently selected related items back to relationships and send a delete request for each of the relationships found
   * Make sure the lists are refreshed afterwards and notifications are sent for success and errors
   */
  public submit(): void {

    // Get all the relationships that should be removed
    const removedRelationshipIDs$: Observable<DeleteRelationship[]> = this.relationshipService.getItemRelationshipsArray(this.item).pipe(
      startWith([]),
      map((relationships: Relationship[]) => relationships.map((relationship) =>
        Object.assign(new Relationship(), relationship, { uuid: relationship.id })
      )),
      switchMap((relationships: Relationship[]) => {
        return this.objectUpdatesService.getFieldUpdatesExclusive(this.url, relationships) as Observable<FieldUpdates>;
      }),
      map((fieldUpdates: FieldUpdates) =>
        Object.values(fieldUpdates)
          .filter((fieldUpdate: FieldUpdate) => fieldUpdate.changeType === FieldChangeType.REMOVE)
          .map((fieldUpdate: FieldUpdate) => fieldUpdate.field as DeleteRelationship)
      ),
    );

    const addRelatedItems$: Observable<RelationshipIdentifiable[]> = this.objectUpdatesService.getFieldUpdates(this.url, []).pipe(
      map((fieldUpdates: FieldUpdates) =>
        Object.values(fieldUpdates)
          .filter((fieldUpdate: FieldUpdate) => hasValue(fieldUpdate))
          .filter((fieldUpdate: FieldUpdate) => fieldUpdate.changeType === FieldChangeType.ADD)
          .map((fieldUpdate: FieldUpdate) => fieldUpdate.field as RelationshipIdentifiable)
      ),
    );

    observableCombineLatest(
      removedRelationshipIDs$,
      addRelatedItems$,
    ).pipe(
      take(1),
    ).subscribe(([removeRelationshipIDs, addRelatedItems]) => {
      const actions = [
        this.deleteRelationships(removeRelationshipIDs),
        this.addRelationships(addRelatedItems),
      ];
      actions.forEach((action) =>
        action.subscribe((response) => {
          if (response.length > 0) {
            this.initializeOriginalFields();
            this.cdr.detectChanges();
            this.displayNotifications(response);
            this.modalService.dismissAll();
          }
        })
      );
    });
  }

  deleteRelationships(deleteRelationshipIDs: DeleteRelationship[]): Observable<RemoteData<NoContent>[]> {
    return observableZip(...deleteRelationshipIDs.map((deleteRelationship) => {
        let copyVirtualMetadata: string;
        if (deleteRelationship.keepLeftVirtualMetadata && deleteRelationship.keepRightVirtualMetadata) {
          copyVirtualMetadata = 'all';
        } else if (deleteRelationship.keepLeftVirtualMetadata) {
          copyVirtualMetadata = 'left';
        } else if (deleteRelationship.keepRightVirtualMetadata) {
          copyVirtualMetadata = 'right';
        } else {
          copyVirtualMetadata = 'none';
        }
        return this.relationshipService.deleteRelationship(deleteRelationship.uuid, copyVirtualMetadata);
      }
    ));
  }

  addRelationships(addRelatedItems: RelationshipIdentifiable[]): Observable<RemoteData<Relationship>[]> {
    return observableZip(...addRelatedItems.map((addRelationship) =>
      this.entityType$.pipe(
        switchMap((entityType) => this.entityTypeService.isLeftType(addRelationship.type, entityType)),
        switchMap((isLeftType) => {
          let leftItem: Item;
          let rightItem: Item;
          let leftwardValue: string;
          let rightwardValue: string;
          if (isLeftType) {
            leftItem = this.item;
            rightItem = addRelationship.relatedItem;
            leftwardValue = null;
            rightwardValue = addRelationship.nameVariant;
          } else {
            leftItem = addRelationship.relatedItem;
            rightItem = this.item;
            leftwardValue = addRelationship.nameVariant;
            rightwardValue = null;
          }
          return this.relationshipService.addRelationship(addRelationship.type.id, leftItem, rightItem, leftwardValue, rightwardValue);
        }),
      )
    ));
  }

  /**
   * Display notifications
   * - Error notification for each failed response with their message
   * - Success notification in case there's at least one successful response
   * @param responses
   */
  displayNotifications(responses: RemoteData<NoContent>[]) {
    const failedResponses = responses.filter((response: RemoteData<NoContent>) => response.hasFailed);
    const successfulResponses = responses.filter((response: RemoteData<NoContent>) => response.hasSucceeded);

    failedResponses.forEach((response: RemoteData<NoContent>) => {
      this.notificationsService.error(this.getNotificationTitle('failed'), response.errorMessage);
    });
    if (successfulResponses.length > 0) {
      this.notificationsService.success(this.getNotificationTitle('saved'), this.getNotificationContent('saved'));
    }
  }
  /**
   * Sends all initial values of this item to the object updates service
   */
  public initializeOriginalFields() {
    return this.relationshipService.getRelatedItems(this.item).pipe(
      take(1),
    ).subscribe((items: Item[]) => {
      this.objectUpdatesService.initialize(this.url, items, this.item.lastModified);
    });
  }


  getRelationshipTypeFollowLinks() {
    return [
      followLink('leftType'),
      followLink('rightType')
    ];
  }

}
