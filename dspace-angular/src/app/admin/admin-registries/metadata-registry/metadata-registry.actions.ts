/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';
import { type } from '../../../shared/ngrx/type';
import { MetadataSchema } from '../../../core/metadata/metadata-schema.model';
import { MetadataField } from '../../../core/metadata/metadata-field.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const MetadataRegistryActionTypes = {

  EDIT_SCHEMA: type('dspace/metadata-registry/EDIT_SCHEMA'),
  CANCEL_EDIT_SCHEMA: type('dspace/metadata-registry/CANCEL_SCHEMA'),
  SELECT_SCHEMA: type('dspace/metadata-registry/SELECT_SCHEMA'),
  DESELECT_SCHEMA: type('dspace/metadata-registry/DESELECT_SCHEMA'),
  DESELECT_ALL_SCHEMA: type('dspace/metadata-registry/DESELECT_ALL_SCHEMA'),

  EDIT_FIELD: type('dspace/metadata-registry/EDIT_FIELD'),
  CANCEL_EDIT_FIELD: type('dspace/metadata-registry/CANCEL_FIELD'),
  SELECT_FIELD: type('dspace/metadata-registry/SELECT_FIELD'),
  DESELECT_FIELD: type('dspace/metadata-registry/DESELECT_FIELD'),
  DESELECT_ALL_FIELD: type('dspace/metadata-registry/DESELECT_ALL_FIELD')
};

/**
 * Used to edit a metadata schema in the metadata registry
 */
export class MetadataRegistryEditSchemaAction implements Action {
  type = MetadataRegistryActionTypes.EDIT_SCHEMA;

  schema: MetadataSchema;

  constructor(registry: MetadataSchema) {
    this.schema = registry;
  }
}

/**
 * Used to cancel the editing of a metadata schema in the metadata registry
 */
export class MetadataRegistryCancelSchemaAction implements Action {
  type = MetadataRegistryActionTypes.CANCEL_EDIT_SCHEMA;
}

/**
 * Used to select a single metadata schema in the metadata registry
 */
export class MetadataRegistrySelectSchemaAction implements Action {
  type = MetadataRegistryActionTypes.SELECT_SCHEMA;

  schema: MetadataSchema;

  constructor(registry: MetadataSchema) {
    this.schema = registry;
  }
}

/**
 * Used to deselect a single metadata schema in the metadata registry
 */
export class MetadataRegistryDeselectSchemaAction implements Action {
  type = MetadataRegistryActionTypes.DESELECT_SCHEMA;

  schema: MetadataSchema;

  constructor(registry: MetadataSchema) {
    this.schema = registry;
  }
}

/**
 * Used to deselect all metadata schemas in the metadata registry
 */
export class MetadataRegistryDeselectAllSchemaAction implements Action {
  type = MetadataRegistryActionTypes.DESELECT_ALL_SCHEMA;
}

/**
 * Used to edit a metadata field in the metadata registry
 */
export class MetadataRegistryEditFieldAction implements Action {
  type = MetadataRegistryActionTypes.EDIT_FIELD;

  field: MetadataField;

  constructor(registry: MetadataField) {
    this.field = registry;
  }
}

/**
 * Used to cancel the editing of a metadata field in the metadata registry
 */
export class MetadataRegistryCancelFieldAction implements Action {
  type = MetadataRegistryActionTypes.CANCEL_EDIT_FIELD;
}

/**
 * Used to select a single metadata field in the metadata registry
 */
export class MetadataRegistrySelectFieldAction implements Action {
  type = MetadataRegistryActionTypes.SELECT_FIELD;

  field: MetadataField;

  constructor(registry: MetadataField) {
    this.field = registry;
  }
}

/**
 * Used to deselect a single metadata field in the metadata registry
 */
export class MetadataRegistryDeselectFieldAction implements Action {
  type = MetadataRegistryActionTypes.DESELECT_FIELD;

  field: MetadataField;

  constructor(registry: MetadataField) {
    this.field = registry;
  }
}

/**
 * Used to deselect all metadata fields in the metadata registry
 */
export class MetadataRegistryDeselectAllFieldAction implements Action {
  type = MetadataRegistryActionTypes.DESELECT_ALL_FIELD;
}


/**
 * Export a type alias of all actions in this action group
 * so that reducers can easily compose action types
 * These are all the actions to perform on the metadata registry state
 */
export type MetadataRegistryAction
  = MetadataRegistryEditSchemaAction
  | MetadataRegistryCancelSchemaAction
  | MetadataRegistrySelectSchemaAction
  | MetadataRegistryDeselectSchemaAction
  | MetadataRegistryEditFieldAction
  | MetadataRegistryCancelFieldAction
  | MetadataRegistrySelectFieldAction
  | MetadataRegistryDeselectFieldAction
  | MetadataRegistryDeselectAllSchemaAction
  | MetadataRegistryDeselectAllFieldAction;
