import {
  MetadataRegistryAction,
  MetadataRegistryActionTypes,
  MetadataRegistryDeselectFieldAction,
  MetadataRegistryDeselectSchemaAction,
  MetadataRegistryEditFieldAction,
  MetadataRegistryEditSchemaAction,
  MetadataRegistrySelectFieldAction,
  MetadataRegistrySelectSchemaAction
} from './metadata-registry.actions';
import { MetadataField } from '../../../core/metadata/metadata-field.model';
import { MetadataSchema } from '../../../core/metadata/metadata-schema.model';

/**
 * The metadata registry state.
 * @interface MetadataRegistryState
 */
export interface MetadataRegistryState {
  editSchema: MetadataSchema;
  selectedSchemas: MetadataSchema[];
  editField: MetadataField;
  selectedFields: MetadataField[];
}

/**
 * The initial state.
 */
const initialState: MetadataRegistryState = {
  editSchema: null,
  selectedSchemas: [],
  editField: null,
  selectedFields: []
};

/**
 * Reducer that handles MetadataRegistryActions to modify metadata schema and/or field states
 * @param state   The current MetadataRegistryState
 * @param action  The MetadataRegistryAction to perform on the state
 */
export function metadataRegistryReducer(state = initialState, action: MetadataRegistryAction): MetadataRegistryState {

  switch (action.type) {

    case MetadataRegistryActionTypes.EDIT_SCHEMA: {
      return Object.assign({}, state, {
        editSchema: (action as MetadataRegistryEditSchemaAction).schema
      });
    }

    case MetadataRegistryActionTypes.CANCEL_EDIT_SCHEMA: {
      return Object.assign({}, state, {
        editSchema: null
      });
    }

    case MetadataRegistryActionTypes.SELECT_SCHEMA: {
      return Object.assign({}, state, {
        selectedSchemas: [...state.selectedSchemas, (action as MetadataRegistrySelectSchemaAction).schema]
      });
    }

    case MetadataRegistryActionTypes.DESELECT_SCHEMA: {
      return Object.assign({}, state, {
        selectedSchemas: state.selectedSchemas.filter(
          (selectedSchema) => selectedSchema !== (action as MetadataRegistryDeselectSchemaAction).schema
        )
      });
    }

    case MetadataRegistryActionTypes.DESELECT_ALL_SCHEMA: {
      return Object.assign({}, state, {
        selectedSchemas: []
      });
    }

    case MetadataRegistryActionTypes.EDIT_FIELD: {
      return Object.assign({}, state, {
        editField: (action as MetadataRegistryEditFieldAction).field
      });
    }

    case MetadataRegistryActionTypes.CANCEL_EDIT_FIELD: {
      return Object.assign({}, state, {
        editField: null
      });
    }

    case MetadataRegistryActionTypes.SELECT_FIELD: {
      return Object.assign({}, state, {
        selectedFields: [...state.selectedFields, (action as MetadataRegistrySelectFieldAction).field]
      });
    }

    case MetadataRegistryActionTypes.DESELECT_FIELD: {
      return Object.assign({}, state, {
        selectedFields: state.selectedFields.filter(
          (selectedField) => selectedField !== (action as MetadataRegistryDeselectFieldAction).field
        )
      });
    }

    case MetadataRegistryActionTypes.DESELECT_ALL_FIELD: {
      return Object.assign({}, state, {
        selectedFields: []
      });
    }

    default:
      return state;
  }
}
