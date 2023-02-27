import {
  AddFieldUpdateAction,
  DiscardObjectUpdatesAction,
  InitializeFieldsAction,
  ObjectUpdatesAction,
  ObjectUpdatesActionTypes,
  ReinstateObjectUpdatesAction,
  RemoveFieldUpdateAction,
  RemoveObjectUpdatesAction,
  SelectVirtualMetadataAction,
  SetEditableFieldUpdateAction,
  SetValidFieldUpdateAction,
} from './object-updates.actions';
import { hasNoValue, hasValue } from '../../../shared/empty.util';
import { Relationship } from '../../shared/item-relationships/relationship.model';
import { PatchOperationService } from './patch-operation-service/patch-operation.service';
import { Item } from '../../shared/item.model';
import { RelationshipType } from '../../shared/item-relationships/relationship-type.model';
import { GenericConstructor } from '../../shared/generic-constructor';
import { Identifiable } from './identifiable.model';
import { FieldUpdates } from './field-updates.model';
import { FieldChangeType } from './field-change-type.model';

/**
 * Path where discarded objects are saved
 */
export const OBJECT_UPDATES_TRASH_PATH = '/trash';

/**
 * The state for a single field
 */
export interface FieldState {
  editable: boolean;
  isNew: boolean;
  isValid: boolean;
}

/**
 * A list of states for all the fields for a single page, mapped by uuid
 */
export interface FieldStates {
  [uuid: string]: FieldState;
}

/**
 * The states of all virtual metadata selections available for a single page, mapped by the relationship uuid
 */
export interface VirtualMetadataSources {
  [source: string]: VirtualMetadataSource;
}

/**
 * The selection of virtual metadata for a relationship, mapped by the uuid of either the item or the relationship type
 */
export interface VirtualMetadataSource {
  [uuid: string]: boolean;
}

export interface RelationshipIdentifiable extends Identifiable {
  nameVariant?: string;
  relatedItem: Item;
  relationship: Relationship;
  type: RelationshipType;
}

/**
 * A fieldupdate interface which represents a relationship selected to be deleted,
 * along with a selection of the virtual metadata to keep
 */
export interface DeleteRelationship extends RelationshipIdentifiable {
  keepLeftVirtualMetadata: boolean;
  keepRightVirtualMetadata: boolean;
}

/**
 * The updated state of a single page
 */
export interface ObjectUpdatesEntry {
  fieldStates: FieldStates;
  fieldUpdates: FieldUpdates;
  virtualMetadataSources: VirtualMetadataSources;
  lastModified: Date;
  patchOperationService?: GenericConstructor<PatchOperationService>;
}

/**
 * The updated state of all pages, mapped by the page URL
 */
export interface ObjectUpdatesState {
  [url: string]: ObjectUpdatesEntry;
}

/**
 * Initial state for an existing initialized field
 */
const initialFieldState = { editable: false, isNew: false, isValid: true };

/**
 * Initial state for a newly added field
 */
const initialNewFieldState = { editable: true, isNew: true, isValid: undefined };

// Object.create(null) ensures the object has no default js properties (e.g. `__proto__`)
const initialState = Object.create(null);

/**
 * Reducer method to calculate the next ObjectUpdates state, based on the current state and the ObjectUpdatesAction
 * @param state The current state
 * @param action The action to perform on the current state
 */
export function objectUpdatesReducer(state = initialState, action: ObjectUpdatesAction): ObjectUpdatesState {
  switch (action.type) {
    case ObjectUpdatesActionTypes.INITIALIZE_FIELDS: {
      return initializeFieldsUpdate(state, action as InitializeFieldsAction);
    }
    case ObjectUpdatesActionTypes.ADD_FIELD: {
      return addFieldUpdate(state, action as AddFieldUpdateAction);
    }
    case ObjectUpdatesActionTypes.SELECT_VIRTUAL_METADATA: {
      return selectVirtualMetadata(state, action as SelectVirtualMetadataAction);
    }
    case ObjectUpdatesActionTypes.DISCARD: {
      return discardObjectUpdates(state, action as DiscardObjectUpdatesAction);
    }
    case ObjectUpdatesActionTypes.REINSTATE: {
      return reinstateObjectUpdates(state, action as ReinstateObjectUpdatesAction);
    }
    case ObjectUpdatesActionTypes.REMOVE: {
      return removeObjectUpdates(state, action as RemoveObjectUpdatesAction);
    }
    case ObjectUpdatesActionTypes.REMOVE_ALL: {
      return removeAllObjectUpdates(state);
    }
    case ObjectUpdatesActionTypes.REMOVE_FIELD: {
      return removeFieldUpdate(state, action as RemoveFieldUpdateAction);
    }
    case ObjectUpdatesActionTypes.SET_EDITABLE_FIELD: {
      return setEditableFieldUpdate(state, action as SetEditableFieldUpdateAction);
    }
    case ObjectUpdatesActionTypes.SET_VALID_FIELD: {
      return setValidFieldUpdate(state, action as SetValidFieldUpdateAction);
    }
    default: {
      return state;
    }
  }
}

/**
 * Initialize the state for a specific url and store all its fields in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function initializeFieldsUpdate(state: any, action: InitializeFieldsAction) {
  const url: string = action.payload.url;
  const fields: Identifiable[] = action.payload.fields;
  const lastModifiedServer: Date = action.payload.lastModified;
  const patchOperationService: GenericConstructor<PatchOperationService> = action.payload.patchOperationService;
  const fieldStates = createInitialFieldStates(fields);
  const newPageState = Object.assign(
    {},
    state[url],
    { fieldStates: fieldStates },
    { fieldUpdates: {} },
    { virtualMetadataSources: {} },
    { lastModified: lastModifiedServer },
    { patchOperationService }
  );
  return Object.assign({}, state, { [url]: newPageState });
}

/**
 * Add a new update for a specific field to the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function addFieldUpdate(state: any, action: AddFieldUpdateAction) {
  const url: string = action.payload.url;
  const field: Identifiable = action.payload.field;
  const changeType: FieldChangeType = action.payload.changeType;
  const pageState: ObjectUpdatesEntry = state[url] || {fieldUpdates: {}};

  let states = pageState.fieldStates;
  if (changeType === FieldChangeType.ADD) {
    states = Object.assign({}, { [field.uuid]: initialNewFieldState }, pageState.fieldStates);
  }

  let fieldUpdate: any = pageState.fieldUpdates[field.uuid] || {};
  const newChangeType = determineChangeType(fieldUpdate.changeType, changeType);

  fieldUpdate = Object.assign({}, { field, changeType: newChangeType });

  const fieldUpdates = Object.assign({}, pageState.fieldUpdates, { [field.uuid]: fieldUpdate });

  const newPageState = Object.assign({}, pageState,
    { fieldStates: states },
    { fieldUpdates: fieldUpdates });
  return Object.assign({}, state, { [url]: newPageState });
}

/**
 * Update the selected virtual metadata in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function selectVirtualMetadata(state: any, action: SelectVirtualMetadataAction) {

  const url: string = action.payload.url;
  const source: string = action.payload.source;
  const uuid: string = action.payload.uuid;
  const select: boolean = action.payload.select;

  const pageState: ObjectUpdatesEntry = state[url] || {};

  const virtualMetadataSource = Object.assign(
    {},
    pageState.virtualMetadataSources[source],
    {
      [uuid]: select,
    },
  );

  const virtualMetadataSources = Object.assign(
    {},
    pageState.virtualMetadataSources,
    {
      [source]: virtualMetadataSource,
    },
  );

  const newPageState = Object.assign(
    {},
    pageState,
    {virtualMetadataSources: virtualMetadataSources},
  );

  return Object.assign(
    {},
    state,
    {
      [url]: newPageState,
    }
  );
}

/**
 * Discard all updates for a specific action's url in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function discardObjectUpdates(state: any, action: DiscardObjectUpdatesAction) {
  if (action.payload.discardAll) {
    let newState = Object.assign({}, state);
    Object.keys(state).filter((path: string) => !path.endsWith(OBJECT_UPDATES_TRASH_PATH)).forEach((path: string) => {
      newState = discardObjectUpdatesFor(path, newState);
    });
    return newState;
  } else {
    const url: string = action.payload.url;
    return discardObjectUpdatesFor(url, state);
  }
}

/**
 * Discard all updates for a specific action's url in the store
 * @param url   The action's url
 * @param state The current state
 */
function discardObjectUpdatesFor(url: string, state: any) {
  const pageState: ObjectUpdatesEntry = state[url];
  const newFieldStates = {};
  Object.keys(pageState.fieldStates).forEach((uuid: string) => {
    const fieldState: FieldState = pageState.fieldStates[uuid];
    if (!fieldState.isNew) {
      /* After discarding we don't want the reset fields to stay editable or invalid */
      newFieldStates[uuid] = Object.assign({}, fieldState, { editable: false, isValid: true });
    }
  });

  const discardedPageState = Object.assign({}, pageState, {
    fieldUpdates: {},
    fieldStates: newFieldStates
  });
  return Object.assign({}, state, { [url]: discardedPageState }, { [url + OBJECT_UPDATES_TRASH_PATH]: pageState });
}

/**
 * Reinstate all updates for a specific action's url in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function reinstateObjectUpdates(state: any, action: ReinstateObjectUpdatesAction) {
  const url: string = action.payload.url;
  const trashState = state[url + OBJECT_UPDATES_TRASH_PATH];

  const newState = Object.assign({}, state, { [url]: trashState });
  delete newState[url + OBJECT_UPDATES_TRASH_PATH];
  return newState;
}

/**
 * Remove all updates for a specific action's url in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function removeObjectUpdates(state: any, action: RemoveObjectUpdatesAction) {
  const url: string = action.payload.url;
  return removeObjectUpdatesByURL(state, url);
}

/**
 * Remove all updates for a specific url in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function removeObjectUpdatesByURL(state: any, url: string) {
  const newState = Object.assign({}, state);
  delete newState[url + OBJECT_UPDATES_TRASH_PATH];
  return newState;
}

/**
 * Remove all updates in the store
 * @param state The current state
 */
function removeAllObjectUpdates(state: any) {
  const newState = Object.assign({}, state);
  Object.keys(state).filter((path: string) => path.endsWith(OBJECT_UPDATES_TRASH_PATH)).forEach((path: string) => {
    delete newState[path];
  });
  return newState;
}

/**
 * Discard the update for a specific action's url and field UUID in the store
 * @param state The current state
 * @param action The action to perform on the current state
 */
function removeFieldUpdate(state: any, action: RemoveFieldUpdateAction) {
  const url: string = action.payload.url;
  const uuid: string = action.payload.uuid;
  let newPageState: ObjectUpdatesEntry = state[url];
  if (hasValue(newPageState)) {
    const newUpdates: FieldUpdates = Object.assign({}, newPageState.fieldUpdates);
    if (hasValue(newUpdates[uuid])) {
      delete newUpdates[uuid];
    }
    const newFieldStates: FieldStates = Object.assign({}, newPageState.fieldStates);
    if (hasValue(newFieldStates[uuid])) {
      /* When resetting, make field not editable */
      if (newFieldStates[uuid].isNew) {
        /* If this field was added, just throw it away */
        delete newFieldStates[uuid];
      } else {
        newFieldStates[uuid] = Object.assign({}, newFieldStates[uuid], { editable: false, isValid: true });
      }
    }
    newPageState = Object.assign({}, state[url], {
      fieldUpdates: newUpdates,
      fieldStates: newFieldStates
    });
  }
  return Object.assign({}, state, { [url]: newPageState });
}

/**
 * Determine the most prominent FieldChangeType, ordered as follows:
 * undefined < UPDATE < ADD < REMOVE
 * @param oldType The current type
 * @param newType The new type that should possibly override the new type
 */
function determineChangeType(oldType: FieldChangeType, newType: FieldChangeType): FieldChangeType {
  if (hasNoValue(newType)) {
    return oldType;
  }
  if (hasNoValue(oldType)) {
    return newType;
  }
  return oldType.valueOf() > newType.valueOf() ? oldType : newType;
}

/**
 * Set the editable state of a specific action's url and uuid to false or true
 * @param state The current state
 * @param action The action to perform on the current state
 */
function setEditableFieldUpdate(state: any, action: SetEditableFieldUpdateAction) {
  const url: string = action.payload.url;
  const uuid: string = action.payload.uuid;
  const editable: boolean = action.payload.editable;

  const pageState: ObjectUpdatesEntry = state[url];

  const fieldState = pageState.fieldStates[uuid];
  const newFieldState = Object.assign({}, fieldState, { editable });

  const newFieldStates = Object.assign({}, pageState.fieldStates, { [uuid]: newFieldState });

  const newPageState = Object.assign({}, pageState, { fieldStates: newFieldStates });

  return Object.assign({}, state, { [url]: newPageState });
}

/**
 * Set the isValid state of a specific action's url and uuid to false or true
 * @param state The current state
 * @param action The action to perform on the current state
 */
function setValidFieldUpdate(state: any, action: SetValidFieldUpdateAction) {
  const url: string = action.payload.url;
  const uuid: string = action.payload.uuid;
  const isValid: boolean = action.payload.isValid;

  const pageState: ObjectUpdatesEntry = state[url];

  const fieldState = pageState.fieldStates[uuid];
  const newFieldState = Object.assign({}, fieldState, { isValid });

  const newFieldStates = Object.assign({}, pageState.fieldStates, { [uuid]: newFieldState });

  const newPageState = Object.assign({}, pageState, { fieldStates: newFieldStates });

  return Object.assign({}, state, { [url]: newPageState });
}

/**
 * Method to create an initial FieldStates object based on a list of Identifiable objects
 * @param fields Identifiable objects
 */
function createInitialFieldStates(fields: Identifiable[]) {
  const uuids = fields.map((field: Identifiable) => field.uuid);
  const fieldStates = {};
  uuids.forEach((uuid: string) => fieldStates[uuid] = initialFieldState);
  return fieldStates;
}
