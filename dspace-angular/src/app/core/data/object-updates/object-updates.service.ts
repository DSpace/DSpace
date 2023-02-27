import { Injectable, Injector } from '@angular/core';
import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { coreSelector } from '../../core.selectors';
import {
  FieldState,
  OBJECT_UPDATES_TRASH_PATH,
  ObjectUpdatesEntry,
  ObjectUpdatesState,
  VirtualMetadataSource
} from './object-updates.reducer';
import { Observable } from 'rxjs';
import {
  AddFieldUpdateAction,
  DiscardObjectUpdatesAction,
  InitializeFieldsAction,
  ReinstateObjectUpdatesAction,
  RemoveFieldUpdateAction,
  SelectVirtualMetadataAction,
  SetEditableFieldUpdateAction,
  SetValidFieldUpdateAction
} from './object-updates.actions';
import { distinctUntilChanged, filter, map, switchMap } from 'rxjs/operators';
import {
  hasNoValue,
  hasValue,
  isEmpty,
  isNotEmpty,
  hasValueOperator
} from '../../../shared/empty.util';
import { INotification } from '../../../shared/notifications/models/notification.model';
import { Operation } from 'fast-json-patch';
import { PatchOperationService } from './patch-operation-service/patch-operation.service';
import { GenericConstructor } from '../../shared/generic-constructor';
import { Identifiable } from './identifiable.model';
import { FieldUpdates } from './field-updates.model';
import { FieldChangeType } from './field-change-type.model';
import { CoreState } from '../../core-state.model';

function objectUpdatesStateSelector(): MemoizedSelector<CoreState, ObjectUpdatesState> {
  return createSelector(coreSelector, (state: CoreState) => state['cache/object-updates']);
}

function filterByUrlObjectUpdatesStateSelector(url: string): MemoizedSelector<CoreState, ObjectUpdatesEntry> {
  return createSelector(objectUpdatesStateSelector(), (state: ObjectUpdatesState) => state[url]);
}

function filterByUrlAndUUIDFieldStateSelector(url: string, uuid: string): MemoizedSelector<CoreState, FieldState> {
  return createSelector(filterByUrlObjectUpdatesStateSelector(url), (state: ObjectUpdatesEntry) => state.fieldStates[uuid]);
}

function virtualMetadataSourceSelector(url: string, source: string): MemoizedSelector<CoreState, VirtualMetadataSource> {
  return createSelector(filterByUrlObjectUpdatesStateSelector(url), (state: ObjectUpdatesEntry) => state.virtualMetadataSources[source]);
}

/**
 * Service that dispatches and reads from the ObjectUpdates' state in the store
 */
@Injectable()
export class ObjectUpdatesService {
  constructor(private store: Store<CoreState>,
              private injector: Injector) {
  }

  /**
   * Method to dispatch an InitializeFieldsAction to the store
   * @param url The page's URL for which the changes are being mapped
   * @param fields The initial fields for the page's object
   * @param lastModified The date the object was last modified
   * @param patchOperationService A {@link PatchOperationService} used for creating a patch
   */
  initialize(url, fields: Identifiable[], lastModified: Date, patchOperationService?: GenericConstructor<PatchOperationService>): void {
    this.store.dispatch(new InitializeFieldsAction(url, fields, lastModified, patchOperationService));
  }

  /**
   * Method to dispatch an AddFieldUpdateAction to the store
   * @param url The page's URL for which the changes are saved
   * @param field An updated field for the page's object
   * @param changeType The last type of change applied to this field
   */
  private saveFieldUpdate(url: string, field: Identifiable, changeType: FieldChangeType) {
    this.store.dispatch(new AddFieldUpdateAction(url, field, changeType));
  }

  /**
   * Request the ObjectUpdatesEntry state for a specific URL
   * @param url The URL to filter by
   */
  private getObjectEntry(url: string): Observable<ObjectUpdatesEntry> {
    return this.store.pipe(select(filterByUrlObjectUpdatesStateSelector(url)));
  }

  /**
   * Request the getFieldState state for a specific URL and UUID
   * @param url The URL to filter by
   * @param uuid The field's UUID to filter by
   */
  private getFieldState(url: string, uuid: string): Observable<FieldState> {
    return this.store.pipe(select(filterByUrlAndUUIDFieldStateSelector(url, uuid)));
  }

  /**
   * Method that combines the state's updates with the initial values (when there's no update) to create
   * a FieldUpdates object
   * @param url The URL of the page for which the FieldUpdates should be requested
   * @param initialFields The initial values of the fields
   * @param ignoreStates  Ignore the fieldStates to loop over the fieldUpdates instead
   */
  getFieldUpdates(url: string, initialFields: Identifiable[], ignoreStates?: boolean): Observable<FieldUpdates> {
    const objectUpdates = this.getObjectEntry(url);
    return objectUpdates.pipe(
      switchMap((objectEntry) => {
        const fieldUpdates: FieldUpdates = {};
        if (hasValue(objectEntry)) {
          Object.keys(ignoreStates ? objectEntry.fieldUpdates : objectEntry.fieldStates).forEach((uuid) => {
            fieldUpdates[uuid] = objectEntry.fieldUpdates[uuid];
          });
        }
        return this.getFieldUpdatesExclusive(url, initialFields).pipe(
          map((fieldUpdatesExclusive) => {
            Object.keys(fieldUpdatesExclusive).forEach((uuid) => {
              fieldUpdates[uuid] = fieldUpdatesExclusive[uuid];
            });
            return fieldUpdates;
          })
        );
      }),
    );
  }

  /**
   * Method that combines the state's updates (excluding updates that aren't part of the initialFields) with
   * the initial values (when there's no update) to create a FieldUpdates object
   * @param url The URL of the page for which the FieldUpdates should be requested
   * @param initialFields The initial values of the fields
   */
  getFieldUpdatesExclusive(url: string, initialFields: Identifiable[]): Observable<FieldUpdates> {
    const objectUpdates = this.getObjectEntry(url);
    return objectUpdates.pipe(
      hasValueOperator(),
      map((objectEntry) => {
      const fieldUpdates: FieldUpdates = {};
      for (const object of initialFields) {
        let fieldUpdate = objectEntry.fieldUpdates[object.uuid];
        if (isEmpty(fieldUpdate)) {
          fieldUpdate = { field: object, changeType: undefined };
        }
        fieldUpdates[object.uuid] = fieldUpdate;
      }
      return fieldUpdates;
    }));
  }

  /**
   * Method to check if a specific field is currently editable in the store
   * @param url The URL of the page on which the field resides
   * @param uuid The UUID of the field
   */
  isEditable(url: string, uuid: string): Observable<boolean> {
    const fieldState$ = this.getFieldState(url, uuid);
    return fieldState$.pipe(
      filter((fieldState) => hasValue(fieldState)),
      map((fieldState) => fieldState.editable),
      distinctUntilChanged()
    );
  }

  /**
   * Method to check if a specific field is currently valid in the store
   * @param url The URL of the page on which the field resides
   * @param uuid The UUID of the field
   */
  isValid(url: string, uuid: string): Observable<boolean> {
    const fieldState$ = this.getFieldState(url, uuid);
    return fieldState$.pipe(
      filter((fieldState) => hasValue(fieldState)),
      map((fieldState) => fieldState.isValid),
      distinctUntilChanged()
    );
  }

  /**
   * Method to check if a specific page is currently valid in the store
   * @param url The URL of the page
   */
  isValidPage(url: string): Observable<boolean> {
    const objectUpdates = this.getObjectEntry(url);
    return objectUpdates.pipe(
      map((entry: ObjectUpdatesEntry) => {
        return Object.values(entry.fieldStates).findIndex((state: FieldState) => !state.isValid) < 0;
      }),
      distinctUntilChanged()
    );
  }

  /**
   * Calls the saveFieldUpdate method with FieldChangeType.ADD
   * @param url The page's URL for which the changes are saved
   * @param field An updated field for the page's object
   */
  saveAddFieldUpdate(url: string, field: Identifiable) {
    this.saveFieldUpdate(url, field, FieldChangeType.ADD);
  }

  /**
   * Calls the saveFieldUpdate method with FieldChangeType.REMOVE
   * @param url The page's URL for which the changes are saved
   * @param field An updated field for the page's object
   */
  saveRemoveFieldUpdate(url: string, field: Identifiable) {
    this.saveFieldUpdate(url, field, FieldChangeType.REMOVE);
  }

  /**
   * Calls the saveFieldUpdate method with FieldChangeType.UPDATE
   * @param url The page's URL for which the changes are saved
   * @param field An updated field for the page's object
   */
  saveChangeFieldUpdate(url: string, field: Identifiable) {
    this.saveFieldUpdate(url, field, FieldChangeType.UPDATE);
  }

  /**
   * Check whether the virtual metadata of a given item is selected to be saved as real metadata
   * @param url           The URL of the page on which the field resides
   * @param relationship  The id of the relationship for which to check whether the virtual metadata is selected to be
   *                      saved as real metadata
   * @param item          The id of the item for which to check whether the virtual metadata is selected to be
   *                      saved as real metadata
   */
  isSelectedVirtualMetadata(url: string, relationship: string, item: string): Observable<boolean> {

    return this.store
      .pipe(
        select(virtualMetadataSourceSelector(url, relationship)),
        map((virtualMetadataSource) => virtualMetadataSource && virtualMetadataSource[item]),
    );
  }

  /**
   * Method to dispatch a SelectVirtualMetadataAction to the store
   * @param url The page's URL for which the changes are saved
   * @param relationship the relationship for which virtual metadata is selected
   * @param uuid the selection identifier, can either be the item uuid or the relationship type uuid
   * @param selected whether or not to select the virtual metadata to be saved
   */
  setSelectedVirtualMetadata(url: string, relationship: string, uuid: string, selected: boolean) {
    this.store.dispatch(new SelectVirtualMetadataAction(url, relationship, uuid, selected));
  }

  /**
   * Dispatches a SetEditableFieldUpdateAction to the store to set a field's editable state
   * @param url The URL of the page on which the field resides
   * @param uuid The UUID of the field that should be set
   * @param editable The new value of editable in the store for this field
   */
  setEditableFieldUpdate(url: string, uuid: string, editable: boolean) {
    this.store.dispatch(new SetEditableFieldUpdateAction(url, uuid, editable));
  }

  /**
   * Dispatches a SetValidFieldUpdateAction to the store to set a field's isValid state
   * @param url The URL of the page on which the field resides
   * @param uuid The UUID of the field that should be set
   * @param valid The new value of isValid in the store for this field
   */
  setValidFieldUpdate(url: string, uuid: string, valid: boolean) {
    this.store.dispatch(new SetValidFieldUpdateAction(url, uuid, valid));
  }

  /**
   * Method to dispatch an DiscardObjectUpdatesAction to the store
   * @param url The page's URL for which the changes should be discarded
   * @param undoNotification The notification which is should possibly be canceled
   */
  discardFieldUpdates(url: string, undoNotification: INotification) {
    this.store.dispatch(new DiscardObjectUpdatesAction(url, undoNotification));
  }

  /**
   * Method to dispatch a DiscardObjectUpdatesAction to the store with discardAll set to true
   * @param url The page's URL for which the changes should be discarded
   * @param undoNotification The notification which is should possibly be canceled
   */
  discardAllFieldUpdates(url: string, undoNotification: INotification) {
    this.store.dispatch(new DiscardObjectUpdatesAction(url, undoNotification, true));
  }

  /**
   * Method to dispatch an ReinstateObjectUpdatesAction to the store
   * @param url The page's URL for which the changes should be reinstated
   */
  reinstateFieldUpdates(url: string) {
    this.store.dispatch(new ReinstateObjectUpdatesAction(url));
  }

  /**
   * Method to dispatch an RemoveFieldUpdateAction to the store
   * @param url The page's URL for which the changes should be removed
   * @param uuid The UUID of the field that should be set
   */
  removeSingleFieldUpdate(url: string, uuid) {
    this.store.dispatch(new RemoveFieldUpdateAction(url, uuid));
  }

  /**
   * Method that combines the state's updates with the initial values (when there's no update) to create
   * a list of updates fields
   * @param url The URL of the page for which the updated fields should be requested
   * @param initialFields The initial values of the fields
   */
  getUpdatedFields(url: string, initialFields: Identifiable[]): Observable<Identifiable[]> {
    const objectUpdates = this.getObjectEntry(url);
    return objectUpdates.pipe(map((objectEntry) => {
      const fields: Identifiable[] = [];
      Object.keys(objectEntry.fieldStates).forEach((uuid) => {
        const fieldUpdate = objectEntry.fieldUpdates[uuid];
        if (hasNoValue(fieldUpdate) || fieldUpdate.changeType !== FieldChangeType.REMOVE) {
          let field;
          if (isNotEmpty(fieldUpdate)) {
            field = fieldUpdate.field;
          } else {
            field = initialFields.find((object: Identifiable) => object.uuid === uuid);
          }
          fields.push(field);
        }
      });
      return fields;
    }));
  }

  /**
   * Checks if the page currently has updates in the store or not
   * @param url The page's url to check for in the store
   */
  hasUpdates(url: string): Observable<boolean> {
    return this.getObjectEntry(url).pipe(map((objectEntry) => hasValue(objectEntry) && isNotEmpty(objectEntry.fieldUpdates)));
  }

  /**
   * Checks if the page currently is reinstatable in the store or not
   * @param url The page's url to check for in the store
   */
  isReinstatable(url: string): Observable<boolean> {
    return this.hasUpdates(url + OBJECT_UPDATES_TRASH_PATH);
  }

  /**
   * Request the current lastModified date stored for the updates in the store
   * @param url The page's url to check for in the store
   */
  getLastModified(url: string): Observable<Date> {
    return this.getObjectEntry(url).pipe(map((entry: ObjectUpdatesEntry) => entry.lastModified));
  }

  /**
   * Create a patch from the current object-updates state
   * The {@link ObjectUpdatesEntry} should contain a patchOperationService, in order to define how a patch should
   * be created. If it doesn't, an empty patch will be returned.
   * @param url The URL of the page for which the patch should be created
   */
  createPatch(url: string): Observable<Operation[]> {
    return this.getObjectEntry(url).pipe(
      map((entry) => {
        let patch = [];
        if (hasValue(entry.patchOperationService)) {
          patch = this.injector.get(entry.patchOperationService).fieldUpdatesToPatchOperations(entry.fieldUpdates);
        }
        return patch;
      })
    );
  }
}
