/* eslint-disable max-classes-per-file */
import { type } from '../../../shared/ngrx/type';
import { Action } from '@ngrx/store';
import { INotification } from '../../../shared/notifications/models/notification.model';
import { PatchOperationService } from './patch-operation-service/patch-operation.service';
import { GenericConstructor } from '../../shared/generic-constructor';
import { Identifiable } from './identifiable.model';
import { FieldChangeType } from './field-change-type.model';

/**
 * The list of ObjectUpdatesAction type definitions
 */
export const ObjectUpdatesActionTypes = {
  INITIALIZE_FIELDS: type('dspace/core/cache/object-updates/INITIALIZE_FIELDS'),
  SET_EDITABLE_FIELD: type('dspace/core/cache/object-updates/SET_EDITABLE_FIELD'),
  SET_VALID_FIELD: type('dspace/core/cache/object-updates/SET_VALID_FIELD'),
  ADD_FIELD: type('dspace/core/cache/object-updates/ADD_FIELD'),
  SELECT_VIRTUAL_METADATA: type('dspace/core/cache/object-updates/SELECT_VIRTUAL_METADATA'),
  DISCARD: type('dspace/core/cache/object-updates/DISCARD'),
  REINSTATE: type('dspace/core/cache/object-updates/REINSTATE'),
  REMOVE: type('dspace/core/cache/object-updates/REMOVE'),
  REMOVE_ALL: type('dspace/core/cache/object-updates/REMOVE_ALL'),
  REMOVE_FIELD: type('dspace/core/cache/object-updates/REMOVE_FIELD')
};


/**
 * An ngrx action to initialize a new page's fields in the ObjectUpdates state
 */
export class InitializeFieldsAction implements Action {
  type = ObjectUpdatesActionTypes.INITIALIZE_FIELDS;
  payload: {
    url: string,
    fields: Identifiable[],
    lastModified: Date,
    patchOperationService?: GenericConstructor<PatchOperationService>
  };

  /**
   * Create a new InitializeFieldsAction
   *
   * @param url
   *    the unique url of the page for which the fields are being initialized
   * @param fields The identifiable fields of which the updates are kept track of
   * @param lastModified The last modified date of the object that belongs to the page
   * @param patchOperationService A {@link PatchOperationService} used for creating a patch
   */
  constructor(
    url: string,
    fields: Identifiable[],
    lastModified: Date,
    patchOperationService?: GenericConstructor<PatchOperationService>
  ) {
    this.payload = { url, fields, lastModified, patchOperationService };
  }
}

/**
 * An ngrx action to add a new field update in the ObjectUpdates state for a certain page url
 */
export class AddFieldUpdateAction implements Action {
  type = ObjectUpdatesActionTypes.ADD_FIELD;
  payload: {
    url: string,
    field: Identifiable,
    changeType: FieldChangeType,
  };

  /**
   * Create a new AddFieldUpdateAction
   *
   * @param url
   *    the unique url of the page for which a field update is added
   * @param field The identifiable field of which a new update is added
   * @param changeType The update's change type
   */
  constructor(
    url: string,
    field: Identifiable,
    changeType: FieldChangeType) {
    this.payload = { url, field, changeType };
  }
}

/**
 * An ngrx action to select/deselect virtual metadata in the ObjectUpdates state for a certain page url
 */
export class SelectVirtualMetadataAction implements Action {

  type = ObjectUpdatesActionTypes.SELECT_VIRTUAL_METADATA;
  payload: {
    url: string,
    source: string,
    uuid: string,
    select: boolean;
  };

  /**
   * Create a new SelectVirtualMetadataAction
   *
   * @param url
   *    the unique url of the page for which a field update is added
   * @param source
   *    the id of the relationship which adds the virtual metadata
   * @param uuid
   *    the id of the item which has the virtual metadata
   * @param select
   *    whether to select or deselect the virtual metadata to be saved as real metadata
   */
  constructor(
    url: string,
    source: string,
    uuid: string,
    select: boolean,
  ) {
    this.payload = { url, source, uuid, select: select};
  }
}

/**
 * An ngrx action to set the editable state of an existing field in the ObjectUpdates state for a certain page url
 */
export class SetEditableFieldUpdateAction implements Action {
  type = ObjectUpdatesActionTypes.SET_EDITABLE_FIELD;
  payload: {
    url: string,
    uuid: string,
    editable: boolean,
  };

  /**
   * Create a new SetEditableFieldUpdateAction
   *
   * @param url
   *    the unique url of the page
   * @param fieldUUID The UUID of the field of which
   * @param editable The new editable value for the field
   */
  constructor(
    url: string,
    fieldUUID: string,
    editable: boolean) {
    this.payload = { url, uuid: fieldUUID, editable };
  }
}

/**
 * An ngrx action to set the isValid state of an existing field in the ObjectUpdates state for a certain page url
 */
export class SetValidFieldUpdateAction implements Action {
  type = ObjectUpdatesActionTypes.SET_VALID_FIELD;
  payload: {
    url: string,
    uuid: string,
    isValid: boolean,
  };

  /**
   * Create a new SetValidFieldUpdateAction
   *
   * @param url
   *    the unique url of the page
   * @param fieldUUID The UUID of the field of which
   * @param isValid The new isValid value for the field
   */
  constructor(
    url: string,
    fieldUUID: string,
    isValid: boolean) {
    this.payload = { url, uuid: fieldUUID, isValid };
  }
}

/**
 * An ngrx action to discard all existing updates in the ObjectUpdates state for a certain page url
 */
export class DiscardObjectUpdatesAction implements Action {
  type = ObjectUpdatesActionTypes.DISCARD;
  payload: {
    url: string,
    notification: INotification,
    discardAll: boolean;
  };

  /**
   * Create a new DiscardObjectUpdatesAction
   *
   * @param url
   *    the unique url of the page for which the changes should be discarded
   * @param notification The notification that is raised when changes are discarded
   * @param discardAll  discard all
   */
  constructor(
    url: string,
    notification: INotification,
    discardAll = false
  ) {
    this.payload = { url, notification, discardAll };
  }
}

/**
 * An ngrx action to reinstate all previously discarded updates in the ObjectUpdates state for a certain page url
 */
export class ReinstateObjectUpdatesAction implements Action {
  type = ObjectUpdatesActionTypes.REINSTATE;
  payload: {
    url: string
  };

  /**
   * Create a new ReinstateObjectUpdatesAction
   *
   * @param url
   *    the unique url of the page for which the changes should be reinstated
   */
  constructor(
    url: string
  ) {
    this.payload = { url };
  }
}

/**
 * An ngrx action to remove all previously discarded updates in the ObjectUpdates state for a certain page url
 */
export class RemoveObjectUpdatesAction implements Action {
  type = ObjectUpdatesActionTypes.REMOVE;
  payload: {
    url: string
  };

  /**
   * Create a new RemoveObjectUpdatesAction
   *
   * @param url
   *    the unique url of the page for which the changes should be removed
   */
  constructor(
    url: string
  ) {
    this.payload = { url };
  }
}

/**
 * An ngrx action to remove all previously discarded updates in the ObjectUpdates state
 */
export class RemoveAllObjectUpdatesAction implements Action {
  type = ObjectUpdatesActionTypes.REMOVE_ALL;
}

/**
 * An ngrx action to remove a single field update in the ObjectUpdates state for a certain page url and field uuid
 */
export class RemoveFieldUpdateAction implements Action {
  type = ObjectUpdatesActionTypes.REMOVE_FIELD;
  payload: {
    url: string,
    uuid: string
  };

  /**
   * Create a new RemoveObjectUpdatesAction
   *
   * @param url
   *    the unique url of the page for which a field's change should be removed
   * @param uuid The UUID of the field for which the change should be removed
   */
  constructor(
    url: string,
    uuid: string
  ) {
    this.payload = { url, uuid };
  }
}


/**
 * A type to encompass all ObjectUpdatesActions
 */
export type ObjectUpdatesAction
  = AddFieldUpdateAction
  | InitializeFieldsAction
  | DiscardObjectUpdatesAction
  | ReinstateObjectUpdatesAction
  | RemoveObjectUpdatesAction
  | RemoveFieldUpdateAction
  | RemoveAllObjectUpdatesAction
  | SelectVirtualMetadataAction
  | SetEditableFieldUpdateAction
  | SetValidFieldUpdateAction;
