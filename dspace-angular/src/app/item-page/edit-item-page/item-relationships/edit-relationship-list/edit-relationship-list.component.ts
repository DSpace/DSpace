import { Component, EventEmitter, Inject, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { LinkService } from '../../../../core/cache/builders/link.service';
import { ObjectUpdatesService } from '../../../../core/data/object-updates/object-updates.service';
import {
  BehaviorSubject,
  combineLatest as observableCombineLatest,
  from as observableFrom,
  Observable,
  Subscription
} from 'rxjs';
import {
  RelationshipIdentifiable
} from '../../../../core/data/object-updates/object-updates.reducer';
import { RelationshipDataService } from '../../../../core/data/relationship-data.service';
import { Item } from '../../../../core/shared/item.model';
import { defaultIfEmpty, map, mergeMap, startWith, switchMap, take, tap, toArray } from 'rxjs/operators';
import { hasNoValue, hasValue, hasValueOperator } from '../../../../shared/empty.util';
import { Relationship } from '../../../../core/shared/item-relationships/relationship.model';
import { RelationshipType } from '../../../../core/shared/item-relationships/relationship-type.model';
import {
  getAllSucceededRemoteData,
  getFirstSucceededRemoteData,
  getFirstSucceededRemoteDataPayload,
  getRemoteDataPayload,
} from '../../../../core/shared/operators';
import { ItemType } from '../../../../core/shared/item-relationships/item-type.model';
import { DsDynamicLookupRelationModalComponent } from '../../../../shared/form/builder/ds-dynamic-form-ui/relation-lookup-modal/dynamic-lookup-relation-modal.component';
import { RelationshipOptions } from '../../../../shared/form/builder/models/relationship-options.model';
import { SelectableListService } from '../../../../shared/object-list/selectable-list/selectable-list.service';
import { SearchResult } from '../../../../shared/search/models/search-result.model';
import { FollowLinkConfig } from '../../../../shared/utils/follow-link-config.model';
import { PaginatedList } from '../../../../core/data/paginated-list.model';
import { RemoteData } from '../../../../core/data/remote-data';
import { Collection } from '../../../../core/shared/collection.model';
import { PaginationComponentOptions } from '../../../../shared/pagination/pagination-component-options.model';
import { PaginationService } from '../../../../core/pagination/pagination.service';
import { RelationshipTypeDataService } from '../../../../core/data/relationship-type-data.service';
import { FieldUpdate } from '../../../../core/data/object-updates/field-update.model';
import { FieldUpdates } from '../../../../core/data/object-updates/field-updates.model';
import { FieldChangeType } from '../../../../core/data/object-updates/field-change-type.model';
import { APP_CONFIG, AppConfig } from '../../../../../config/app-config.interface';
import { itemLinksToFollow } from '../../../../shared/utils/relation-query.utils';

@Component({
  selector: 'ds-edit-relationship-list',
  styleUrls: ['./edit-relationship-list.component.scss'],
  templateUrl: './edit-relationship-list.component.html',
})
/**
 * A component creating a list of editable relationships of a certain type
 * The relationships are rendered as a list of related items
 */
export class EditRelationshipListComponent implements OnInit, OnDestroy {

  /**
   * The item to display related items for
   */
  @Input() item: Item;

  @Input() itemType: ItemType;

  /**
   * The URL to the current page
   * Used to fetch updates for the current item from the store
   */
  @Input() url: string;

  /**
   * The label of the relationship-type we're rendering a list for
   */
  @Input() relationshipType: RelationshipType;

  /**
   * If updated information has changed
   */
  @Input() hasChanges!: Observable<boolean>;

  /**
   * The event emmiter to submit the new information
   */
  @Output() submit: EventEmitter<any> = new EventEmitter();

  /**
   * Observable that emits the left and right item type of {@link relationshipType} simultaneously.
   */
  private relationshipLeftAndRightType$: Observable<[ItemType, ItemType]>;

  /**
   * Observable that emits true if {@link itemType} is on the left-hand side of {@link relationshipType},
   * false if it is on the right-hand side and undefined in the rare case that it is on neither side.
   */
  private currentItemIsLeftItem$: Observable<boolean>;

  private relatedEntityType$: Observable<ItemType>;

  /**
   * The list ID to save selected entities under
   */
  listId: string;

  /**
   * The FieldUpdates for the relationships in question
   */
  updates$: BehaviorSubject<FieldUpdates> = new BehaviorSubject(undefined);

  /**
   * The RemoteData for the relationships
   */
  relationshipsRd$: BehaviorSubject<RemoteData<PaginatedList<Relationship>>> = new BehaviorSubject(undefined);

  /**
   * Whether the current page is the last page
   */
  isLastPage$: BehaviorSubject<boolean> = new BehaviorSubject(true);

  /**
   * Whether we're loading
   */
  loading$: BehaviorSubject<boolean> = new BehaviorSubject(true);

  /**
   * The number of added fields that haven't been saved yet
   */
  nbAddedFields$: BehaviorSubject<number> = new BehaviorSubject(0);

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  /**
   * The pagination config
   */
  paginationConfig: PaginationComponentOptions;

  /**
   * A reference to the lookup window
   */
  modalRef: NgbModalRef;

  /**
   * Determines whether to ask for the embedded item thumbnail.
   */
  fetchThumbnail: boolean;

  constructor(
    protected objectUpdatesService: ObjectUpdatesService,
    protected linkService: LinkService,
    protected relationshipService: RelationshipDataService,
    protected relationshipTypeService: RelationshipTypeDataService,
    protected modalService: NgbModal,
    protected paginationService: PaginationService,
    protected selectableListService: SelectableListService,
    @Inject(APP_CONFIG) protected appConfig: AppConfig
  ) {
    this.fetchThumbnail = this.appConfig.browseBy.showThumbnails;
  }

  /**
   * Get the i18n message key for this relationship type
   */
  public getRelationshipMessageKey(): Observable<string> {

    return observableCombineLatest(
      this.getLabel(),
      this.relatedEntityType$,
    ).pipe(
      map(([label, relatedEntityType]) => {
        if (hasValue(label) && label.indexOf('is') > -1 && label.indexOf('Of') > -1) {
          const relationshipLabel = `${label.substring(2, label.indexOf('Of'))}`;
          if (relationshipLabel !== relatedEntityType.label) {
            return `relationships.is${relationshipLabel}Of.${relatedEntityType.label}`;
          } else {
            return `relationships.is${relationshipLabel}Of`;
          }
        } else {
          return label;
        }
      }),
    );
  }

  /**
   * Get the relevant label for this relationship type
   */
  private getLabel(): Observable<string> {
    return observableCombineLatest([
      this.relationshipType.leftType,
      this.relationshipType.rightType,
    ].map((itemTypeRD) => itemTypeRD.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    ))).pipe(
      map((itemTypes: ItemType[]) => [
        this.relationshipType.leftwardType,
        this.relationshipType.rightwardType,
      ][itemTypes.findIndex((itemType) => itemType.id === this.itemType.id)]),
    );
  }

  /**
   * Prevent unnecessary rerendering so fields don't lose focus
   */
  trackUpdate(index, update: FieldUpdate) {
    return update && update.field ? update.field.uuid : undefined;
  }

  /**
   * Open the dynamic lookup modal to search for items to add as relationships
   */
  openLookup() {

    this.modalRef = this.modalService.open(DsDynamicLookupRelationModalComponent, {
      size: 'lg'
    });
    const modalComp: DsDynamicLookupRelationModalComponent = this.modalRef.componentInstance;
    modalComp.repeatable = true;
    modalComp.isEditRelationship = true;
    modalComp.listId = this.listId;
    modalComp.item = this.item;
    modalComp.relationshipType = this.relationshipType;
    modalComp.currentItemIsLeftItem$ = this.currentItemIsLeftItem$;
    modalComp.toAdd = [];
    modalComp.toRemove = [];
    modalComp.isPending = false;

    this.item.owningCollection.pipe(
      getFirstSucceededRemoteDataPayload()
    ).subscribe((collection: Collection) => {
      modalComp.collection = collection;
    });

    modalComp.select = (...selectableObjects: SearchResult<Item>[]) => {
      selectableObjects.forEach((searchResult) => {
        const relatedItem: Item = searchResult.indexableObject;

        const foundIndex = modalComp.toRemove.findIndex( el => el.uuid === relatedItem.uuid);

        if (foundIndex !== -1) {
          modalComp.toRemove.splice(foundIndex,1);
        } else {

          this.getRelationFromId(relatedItem)
            .subscribe((relationship: Relationship) => {
              if (!relationship ) {
                modalComp.toAdd.push(searchResult);
              } else {
                const foundIndexRemove = modalComp.toRemove.findIndex( el => el.indexableObject.uuid === relatedItem.uuid);
                if (foundIndexRemove !== -1) {
                  modalComp.toRemove.splice(foundIndexRemove,1);
                }
              }

              this.loading$.next(true);
              // emit the last page again to trigger a fieldupdates refresh
              this.relationshipsRd$.next(this.relationshipsRd$.getValue());
            });
        }
      });
    };
    modalComp.deselect = (...selectableObjects: SearchResult<Item>[]) => {
      selectableObjects.forEach((searchResult) => {
        const relatedItem: Item = searchResult.indexableObject;

        const foundIndex = modalComp.toAdd.findIndex( el => el.indexableObject.uuid === relatedItem.uuid);

        if (foundIndex !== -1) {
          modalComp.toAdd.splice(foundIndex,1);
        } else {
          modalComp.toRemove.push(searchResult);
        }
      });
    };



    modalComp.submitEv = () => {

      const subscriptions = [];

      modalComp.toAdd.forEach((searchResult: SearchResult<Item>) => {
        const relatedItem = searchResult.indexableObject;
        subscriptions.push(this.relationshipService.getNameVariant(this.listId, relatedItem.uuid).pipe(
          map((nameVariant) => {
          const update = {
            uuid: this.relationshipType.id + '-' + searchResult.indexableObject.uuid,
            nameVariant,
            type: this.relationshipType,
            relatedItem,
          } as RelationshipIdentifiable;
          this.objectUpdatesService.saveAddFieldUpdate(this.url, update);
          return update;
        })
        ));
      });

      modalComp.toRemove.forEach( (searchResult) => {
        subscriptions.push(this.relationshipService.getNameVariant(this.listId, searchResult.indexableObjectuuid).pipe(
          switchMap((nameVariant) => {
            return this.getRelationFromId(searchResult.indexableObject).pipe(
              map( (relationship: Relationship) => {
                const update = {
                  uuid: relationship.id,
                  nameVariant,
                  type: this.relationshipType,
                  relationship,
                } as RelationshipIdentifiable;
                this.objectUpdatesService.saveRemoveFieldUpdate(this.url,update);
                return update;
              })
            );
          })
        ));
      });

      observableCombineLatest(subscriptions).subscribe( (res) => {
        // Wait until the states changes since there are multiple items
        setTimeout( () => {
          this.submit.emit();
        },1000);

        modalComp.isPending = true;
      });
    };


    modalComp.discardEv = () => {
      modalComp.toAdd.forEach( (searchResult) => {
        this.selectableListService.deselectSingle(this.listId,searchResult);
      });

      modalComp.toRemove.forEach( (searchResult) => {
        this.selectableListService.selectSingle(this.listId,searchResult);
      });

      modalComp.toAdd = [];
      modalComp.toRemove = [];
    };

    this.relatedEntityType$
      .pipe(take(1))
      .subscribe((relatedEntityType) => {
        modalComp.relationshipOptions = Object.assign(
          new RelationshipOptions(), {
            relationshipType: relatedEntityType.label,
            searchConfiguration: relatedEntityType.label.toLowerCase(),
            nameVariants: 'true',
          }
        );
      });

    this.selectableListService.deselectAll(this.listId);
  }

  getRelationFromId(relatedItem) {
    return this.currentItemIsLeftItem$.pipe(
      take(1),
      switchMap( isLeft => {
        let apiCall;
        if (isLeft) {
          apiCall = this.relationshipService.searchByItemsAndType( this.relationshipType.id, this.item.uuid, this.relationshipType.leftwardType ,[relatedItem.id] ).pipe(
                      getFirstSucceededRemoteData(),
                      getRemoteDataPayload(),
                    );
        } else {
          apiCall = this.relationshipService.searchByItemsAndType( this.relationshipType.id, this.item.uuid, this.relationshipType.rightwardType ,[relatedItem.id] ).pipe(
                      getFirstSucceededRemoteData(),
                      getRemoteDataPayload(),
                    );
        }

        return apiCall.pipe(
          map( (res: PaginatedList<Relationship>) => res.page[0])
        );
      }
    ));
  }



  /**
   * Get the existing field updates regarding a relationship with a given item
   * @param relatedItem The item for which to get the existing field updates
   */
  private getFieldUpdatesForRelatedItem(relatedItem: Item): Observable<RelationshipIdentifiable[]> {
    return this.updates$.pipe(
      take(1),
      map((updates) => Object.values(updates)
        .map((update) => update.field as RelationshipIdentifiable)
        .filter((field) => field.relationship)
      ),
      mergeMap((identifiables) =>
        observableCombineLatest(
          identifiables.map((identifiable) => this.getRelatedItem(identifiable.relationship))
        ).pipe(
          defaultIfEmpty([]),
          map((relatedItems) => {
            return identifiables.filter( (identifiable, index) => {
                return relatedItems[index].uuid === relatedItem.uuid;
            });
          }
          ),
        )
      )
    );
  }

  /**
   * Check if the given item is related with the item we are editing relationships
   * @param relatedItem The item for which to get the existing field updates
   */
  private getIsRelatedItem(relatedItem: Item): Observable<boolean> {

    return this.currentItemIsLeftItem$.pipe(
      take(1),
      map( isLeft => {
        if (isLeft) {
          const listOfRelatedItems = this.item.allMetadataValues( 'relation.' + this.relationshipType.leftwardType );
          return !!listOfRelatedItems.find( (uuid) => uuid === relatedItem.uuid );
        } else {
          const listOfRelatedItems = this.item.allMetadataValues( 'relation.' + this.relationshipType.rightwardType );
          return !!listOfRelatedItems.find( (uuid) => uuid === relatedItem.uuid );
        }
      })
    );
  }

  /**
   * Get the related item for a given relationship
   * @param relationship  The relationship for which to get the related item
   */
  private getRelatedItem(relationship: Relationship): Observable<Item> {
    return this.relationshipService.isLeftItem(relationship, this.item).pipe(
      switchMap((isLeftItem) => isLeftItem ? relationship.rightItem : relationship.leftItem),
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    ) as Observable<Item>;
  }

  ngOnInit(): void {

    // store the left and right type of the relationship in a single observable
    this.relationshipLeftAndRightType$ = observableCombineLatest([
      this.relationshipType.leftType,
      this.relationshipType.rightType,
    ].map((type) => type.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    ))) as Observable<[ItemType, ItemType]>;

    this.relatedEntityType$ = this.relationshipLeftAndRightType$.pipe(
      map((relatedTypes: ItemType[]) => relatedTypes.find((relatedType) => relatedType.uuid !== this.itemType.uuid)),
      hasValueOperator()
    );

    this.relatedEntityType$.pipe(
      take(1)
    ).subscribe(
      (relatedEntityType) => this.listId = `edit-relationship-${this.itemType.id}-${relatedEntityType.id}`
    );

    this.currentItemIsLeftItem$ = this.relationshipLeftAndRightType$.pipe(
      map(([leftType, rightType]: [ItemType, ItemType]) => {
        if (leftType.id === this.itemType.id) {
          return true;
        }

        if (rightType.id === this.itemType.id) {
          return false;
        }

        // should never happen...
        console.warn(`The item ${this.item.uuid} is not on the right or the left side of relationship type ${this.relationshipType.uuid}`);
        return undefined;
      })
    );


    // initialize the pagination options
    this.paginationConfig = new PaginationComponentOptions();
    this.paginationConfig.id = `er${this.relationshipType.id}`;
    this.paginationConfig.pageSize = 5;
    this.paginationConfig.currentPage = 1;

    // get the pagination params from the route
    const currentPagination$ = this.paginationService.getCurrentPagination(
      this.paginationConfig.id,
      this.paginationConfig
    ).pipe(
      tap(() => this.loading$.next(true))
    );

    // this adds thumbnail images when required by configuration
    let linksToFollow: FollowLinkConfig<Relationship>[] = itemLinksToFollow(this.fetchThumbnail);

    this.subs.push(
      observableCombineLatest([
        currentPagination$,
        this.currentItemIsLeftItem$,
      ]).pipe(
        switchMap(([currentPagination, currentItemIsLeftItem]: [PaginationComponentOptions, boolean]) =>
          // get the relationships for the current item, relationshiptype and page
          this.relationshipService.getItemRelationshipsByLabel(
            this.item,
            currentItemIsLeftItem ? this.relationshipType.leftwardType : this.relationshipType.rightwardType,
            {
              elementsPerPage: currentPagination.pageSize,
              currentPage: currentPagination.currentPage
            },
            false,
            true,
            ...linksToFollow
          )),
      ).subscribe((rd: RemoteData<PaginatedList<Relationship>>) => {
        this.relationshipsRd$.next(rd);
      })
    );

    // keep isLastPage$ up to date based on relationshipsRd$
    this.subs.push(this.relationshipsRd$.pipe(
      hasValueOperator(),
      getAllSucceededRemoteData()
    ).subscribe((rd: RemoteData<PaginatedList<Relationship>>) => {
      this.isLastPage$.next(hasNoValue(rd.payload._links.next));
    }));

    this.subs.push(this.relationshipsRd$.pipe(
      hasValueOperator(),
      getAllSucceededRemoteData(),
      switchMap((rd: RemoteData<PaginatedList<Relationship>>) =>
        // emit each relationship in the page separately
        observableFrom(rd.payload.page).pipe(
          mergeMap((relationship: Relationship) =>
            // check for each relationship whether it's the left item
            this.relationshipService.isLeftItem(relationship, this.item).pipe(
              // emit an array containing both the relationship and whether it's the left item,
              // as we'll need both
              map((isLeftItem: boolean) => [relationship, isLeftItem])
            )
          ),
          map(([relationship, isLeftItem]: [Relationship, boolean]) => {
            // turn it into a RelationshipIdentifiable, an
            const nameVariant =
              isLeftItem ? relationship.rightwardValue : relationship.leftwardValue;
            return {
              uuid: relationship.id,
              type: this.relationshipType,
              relationship,
              nameVariant,
            } as RelationshipIdentifiable;
          }),
          // wait until all relationships have been processed, and emit them all as a single array
          toArray(),
          // if the pipe above completes without emitting anything, emit an empty array instead
          defaultIfEmpty([])
      )),
      switchMap((nextFields: RelationshipIdentifiable[]) => {
        // Get a list that contains the unsaved changes for the page, as well as the page of
        // RelationshipIdentifiables, as a single list of FieldUpdates
        return this.objectUpdatesService.getFieldUpdates(this.url, nextFields).pipe(
          map((fieldUpdates: FieldUpdates) => {
            const fieldUpdatesFiltered: FieldUpdates = {};
            this.nbAddedFields$.next(0);
            // iterate over the fieldupdates and filter out the ones that pertain to this
            // relationshiptype
            Object.keys(fieldUpdates).forEach((uuid) => {
              if (hasValue(fieldUpdates[uuid])) {
                const field = fieldUpdates[uuid].field as RelationshipIdentifiable;
                // only include fieldupdates regarding this RelationshipType
                if (field.type.id === this.relationshipType.id) {
                  // if it's a newly added relationship
                  if (fieldUpdates[uuid].changeType === FieldChangeType.ADD) {
                    // increase the counter that tracks new relationships
                    this.nbAddedFields$.next(this.nbAddedFields$.getValue() + 1);
                    if (this.isLastPage$.getValue() === true) {
                      // only include newly added relationships to the output if we're on the last
                      // page
                      fieldUpdatesFiltered[uuid] = fieldUpdates[uuid];
                    }
                  } else {
                    // include all others
                    fieldUpdatesFiltered[uuid] = fieldUpdates[uuid];
                  }
                }
              }
            });
            return fieldUpdatesFiltered;
          }),
        );
      }),
      startWith({}),
    ).subscribe((updates: FieldUpdates) => {
      this.loading$.next(false);
      this.updates$.next(updates);
    }));
  }

  ngOnDestroy(): void {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }
}
