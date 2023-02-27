import { Inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { filter, map, mergeMap, switchMap, take } from 'rxjs/operators';
import { BehaviorSubject, Observable } from 'rxjs';
import { RelationshipDataService } from '../../../../../core/data/relationship-data.service';
import {
  getRemoteDataPayload,
  getFirstSucceededRemoteData, DEBOUNCE_TIME_OPERATOR
} from '../../../../../core/shared/operators';
import {
  AddRelationshipAction,
  RelationshipAction,
  RelationshipActionTypes,
  UpdateRelationshipAction,
  UpdateRelationshipNameVariantAction
} from './relationship.actions';
import { Item } from '../../../../../core/shared/item.model';
import { hasNoValue, hasValue, hasValueOperator } from '../../../../empty.util';
import { Relationship } from '../../../../../core/shared/item-relationships/relationship.model';
import { RelationshipType } from '../../../../../core/shared/item-relationships/relationship-type.model';
import { RelationshipTypeDataService } from '../../../../../core/data/relationship-type-data.service';
import { SubmissionObjectDataService } from '../../../../../core/submission/submission-object-data.service';
import { SaveSubmissionSectionFormSuccessAction } from '../../../../../submission/objects/submission-objects.actions';
import { SubmissionObject } from '../../../../../core/submission/models/submission-object.model';
import { SubmissionState } from '../../../../../submission/submission.reducers';
import { Store } from '@ngrx/store';
import { ObjectCacheService } from '../../../../../core/cache/object-cache.service';
import { RequestService } from '../../../../../core/data/request.service';
import { ServerSyncBufferActionTypes } from '../../../../../core/cache/server-sync-buffer.actions';
import { JsonPatchOperationsActionTypes } from '../../../../../core/json-patch/json-patch-operations.actions';
import { followLink } from '../../../../utils/follow-link-config.model';
import { RemoteData } from '../../../../../core/data/remote-data';
import { NotificationsService } from '../../../../notifications/notifications.service';
import { SelectableListService } from '../../../../object-list/selectable-list/selectable-list.service';
import { TranslateService } from '@ngx-translate/core';

const DEBOUNCE_TIME = 500;

/**
 * NGRX effects for RelationshipEffects
 */
@Injectable()
export class RelationshipEffects {
  /**
   * Map that keeps track of the latest RelationshipEffects for each relationship's composed identifier
   */
  private debounceMap: {
    [identifier: string]: BehaviorSubject<string>
  } = {};

  private nameVariantUpdates: {
    [identifier: string]: string
  } = {};

  private initialActionMap: {
    [identifier: string]: string
  } = {};

  private updateAfterPatchSubmissionId: string;

  /**
   * Effect that makes sure all last fired RelationshipActions' types are stored in the map of this service, with the object uuid as their key
   */
   mapLastActions$ = createEffect(() => this.actions$
    .pipe(
      ofType(RelationshipActionTypes.ADD_RELATIONSHIP, RelationshipActionTypes.REMOVE_RELATIONSHIP),
      map((action: RelationshipAction) => {
          const { item1, item2, submissionId, relationshipType } = action.payload;
          const identifier: string = this.createIdentifier(item1, item2, relationshipType);
          if (hasNoValue(this.debounceMap[identifier])) {
            this.initialActionMap[identifier] = action.type;
            this.debounceMap[identifier] = new BehaviorSubject<string>(action.type);
            this.debounceMap[identifier].pipe(
              this.debounceTime(DEBOUNCE_TIME),
              take(1)
            ).subscribe(
              (type) => {
                if (this.initialActionMap[identifier] === type) {
                  if (type === RelationshipActionTypes.ADD_RELATIONSHIP) {
                    let nameVariant = (action as AddRelationshipAction).payload.nameVariant;
                    if (hasValue(this.nameVariantUpdates[identifier])) {
                      nameVariant = this.nameVariantUpdates[identifier];
                      delete this.nameVariantUpdates[identifier];
                    }
                    this.addRelationship(item1, item2, relationshipType, submissionId, nameVariant);
                  } else {
                    this.removeRelationship(item1, item2, relationshipType, submissionId);
                  }
                }
                delete this.debounceMap[identifier];
                delete this.initialActionMap[identifier];

              }
            );
          } else {
            this.debounceMap[identifier].next(action.type);
          }
        }
      )
    ), { dispatch: false });

  /**
   * Updates the namevariant in a relationship
   * If the relationship is currently being added or removed, it will add the name variant to an update map so it will be sent with the next add request instead
   * Otherwise the update is done immediately
   */
   updateNameVariantsActions$ = createEffect(() => this.actions$
    .pipe(
      ofType(RelationshipActionTypes.UPDATE_NAME_VARIANT),
      map((action: UpdateRelationshipNameVariantAction) => {
          const { item1, item2, relationshipType, submissionId, nameVariant } = action.payload;
          const identifier: string = this.createIdentifier(item1, item2, relationshipType);
          const inProgress = hasValue(this.debounceMap[identifier]);
          if (inProgress) {
            this.nameVariantUpdates[identifier] = nameVariant;
          } else {
            this.relationshipService.updateNameVariant(item1, item2, relationshipType, nameVariant).pipe(
              filter((relationshipRD: RemoteData<Relationship>) => hasValue(relationshipRD.payload)),
              take(1)
            ).subscribe((c) => {
              this.updateAfterPatchSubmissionId = submissionId;
              this.relationshipService.refreshRelationshipItemsInCache(item1);
              this.relationshipService.refreshRelationshipItemsInCache(item2);
            });
          }
        }
      )
    ), { dispatch: false });

  /**
   * Save the latest submission ID, to make sure it's updated when the patch is finished
   */
   updateRelationshipActions$ = createEffect(() => this.actions$
    .pipe(
      ofType(RelationshipActionTypes.UPDATE_RELATIONSHIP),
      map((action: UpdateRelationshipAction) => {
        this.updateAfterPatchSubmissionId = action.payload.submissionId;
      })
    ), { dispatch: false });

  /**
   * Save the submission object with ID updateAfterPatchSubmissionId
   */
   saveSubmissionSection = createEffect(() => this.actions$
    .pipe(
      ofType(ServerSyncBufferActionTypes.EMPTY, JsonPatchOperationsActionTypes.COMMIT_JSON_PATCH_OPERATIONS),
      filter(() => hasValue(this.updateAfterPatchSubmissionId)),
      switchMap(() => this.refreshWorkspaceItemInCache(this.updateAfterPatchSubmissionId)),
      map((submissionObject) => new SaveSubmissionSectionFormSuccessAction(this.updateAfterPatchSubmissionId, [submissionObject], false))
    ));

  constructor(private actions$: Actions,
              private relationshipService: RelationshipDataService,
              private relationshipTypeService: RelationshipTypeDataService,
              private submissionObjectService: SubmissionObjectDataService,
              private store: Store<SubmissionState>,
              private objectCache: ObjectCacheService,
              private requestService: RequestService,
              private notificationsService: NotificationsService,
              private translateService: TranslateService,
              private selectableListService: SelectableListService,
              @Inject(DEBOUNCE_TIME_OPERATOR) private debounceTime: <T>(dueTime: number) => (source: Observable<T>) => Observable<T>,
  ) {
  }

  private createIdentifier(item1: Item, item2: Item, relationshipType: string): string {
    return `${item1.uuid}-${item2.uuid}-${relationshipType}`;
  }

  private addRelationship(item1: Item, item2: Item, relationshipType: string, submissionId: string, nameVariant?: string) {
    const type1: string = item1.firstMetadataValue('dspace.entity.type');
    const type2: string = item2.firstMetadataValue('dspace.entity.type');
    return this.relationshipTypeService.getRelationshipTypeByLabelAndTypes(relationshipType, type1, type2)
      .pipe(
        mergeMap((type: RelationshipType) => {
          if (type === null) {
            return [null];
          } else {
            const isSwitched = type.rightwardType === relationshipType;
            if (isSwitched) {
              return this.relationshipService.addRelationship(type.id, item2, item1, nameVariant, undefined);
            } else {
              return this.relationshipService.addRelationship(type.id, item1, item2, undefined, nameVariant);
            }
          }
        }),
        take(1),
        switchMap((rd: RemoteData<Relationship>) => {
          if (hasNoValue(rd) || rd.hasFailed) {
            // An error occurred, deselect the object from the selectable list and display an error notification
            const listId = `list-${submissionId}-${relationshipType}`;
            this.selectableListService.findSelectedByCondition(listId, (object: any) => hasValue(object.indexableObject) && object.indexableObject.uuid === item2.uuid).pipe(
              take(1),
              hasValueOperator()
            ).subscribe((selected) => {
              this.selectableListService.deselectSingle(listId, selected);
            });
            let errorContent;
            if (hasNoValue(rd)) {
              errorContent = this.translateService.instant('relationships.add.error.relationship-type.content', { type: relationshipType });
            } else {
              errorContent = this.translateService.instant('relationships.add.error.server.content');
            }
            this.notificationsService.error(this.translateService.instant('relationships.add.error.title'), errorContent);
          }
          return this.refreshWorkspaceItemInCache(submissionId);
        }),
      ).subscribe((submissionObject: SubmissionObject) => this.store.dispatch(new SaveSubmissionSectionFormSuccessAction(submissionId, [submissionObject], false)));
  }

  private removeRelationship(item1: Item, item2: Item, relationshipType: string, submissionId: string) {
    this.relationshipService.getRelationshipByItemsAndLabel(item1, item2, relationshipType).pipe(
      mergeMap((relationship: Relationship) => this.relationshipService.deleteRelationship(relationship.id, 'none')),
      take(1),
      switchMap(() => this.refreshWorkspaceItemInCache(submissionId)),
    ).subscribe((submissionObject: SubmissionObject) => {
      this.store.dispatch(new SaveSubmissionSectionFormSuccessAction(submissionId, [submissionObject], false));
    });
  }

  /**
   * Make sure the SubmissionObject is refreshed in the cache after being used
   * @param submissionId The ID of the submission object
   */
  private refreshWorkspaceItemInCache(submissionId: string): Observable<SubmissionObject> {
    return this.submissionObjectService.findById(submissionId, false, false, followLink('item')).pipe(getFirstSucceededRemoteData(), getRemoteDataPayload());
  }
}
