import {
  MetadataRegistryCancelFieldAction,
  MetadataRegistryCancelSchemaAction,
  MetadataRegistryDeselectAllFieldAction,
  MetadataRegistryDeselectAllSchemaAction,
  MetadataRegistryDeselectFieldAction,
  MetadataRegistryDeselectSchemaAction,
  MetadataRegistryEditFieldAction,
  MetadataRegistryEditSchemaAction,
  MetadataRegistrySelectFieldAction,
  MetadataRegistrySelectSchemaAction
} from './metadata-registry.actions';
import { metadataRegistryReducer, MetadataRegistryState } from './metadata-registry.reducers';
import { MetadataSchema } from '../../../core/metadata/metadata-schema.model';
import { MetadataField } from '../../../core/metadata/metadata-field.model';

class NullAction extends MetadataRegistryEditSchemaAction {
  type = null;

  constructor() {
    super(undefined);
  }
}

const schema: MetadataSchema = Object.assign(new MetadataSchema(),
  {
    id: 'schema-id',
    _links: {
      self: {
        href: 'http://rest.self/schema/dc'
      },
    },
    prefix: 'dc',
    namespace: 'http://dublincore.org/documents/dcmi-terms/'
  });

const schema2: MetadataSchema = Object.assign(new MetadataSchema(),
  {
    id: 'another-schema-id',
    _links: {
      self: {
        href: 'http://rest.self/schema/dcterms',
      },
    },
    prefix: 'dcterms',
    namespace: 'http://purl.org/dc/terms/'
  });

const field: MetadataField = Object.assign(new MetadataField(),
  {
    id: 'author-field-id',
    _links: {
      self: {
        href: 'http://rest.self/field/author',
      },
    },
    element: 'contributor',
    qualifier: 'author',
    scopeNote: 'Author of an item',
    schema: schema
  });

const field2: MetadataField = Object.assign(new MetadataField(),
  {
    id: 'title-field-id',
    _links: {
      self: {
        href: 'http://rest.self/field/title',
      },
    },
    element: 'title',
    qualifier: null,
    scopeNote: 'Title of an item',
    schema: schema
  });

const initialState: MetadataRegistryState = {
  editSchema: null,
  selectedSchemas: [],
  editField: null,
  selectedFields: []
};

const editState: MetadataRegistryState = {
  editSchema: schema,
  selectedSchemas: [],
  editField: field,
  selectedFields: []
};

const selectState: MetadataRegistryState = {
  editSchema: null,
  selectedSchemas: [schema2],
  editField: null,
  selectedFields: [field2]
};

const moreSelectState: MetadataRegistryState = {
  editSchema: null,
  selectedSchemas: [schema, schema2],
  editField: null,
  selectedFields: [field, field2]
};

describe('metadataRegistryReducer', () => {

  it('should return the current state when no valid actions have been made', () => {
    const state = initialState;
    const action = new NullAction();
    const newState = metadataRegistryReducer(state, action);

    expect(newState).toEqual(state);
  });

  it('should start with an the initial state', () => {
    const state = initialState;
    const action = new NullAction();
    const initState = metadataRegistryReducer(undefined, action);

    expect(initState).toEqual(state);
  });

  it('should update the current state to change the editSchema to a new schema when MetadataRegistryEditSchemaAction is dispatched', () => {
    const state = editState;
    const action = new MetadataRegistryEditSchemaAction(schema2);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.editSchema).toEqual(schema2);
  });

  it('should update the current state to remove the editSchema from the state when MetadataRegistryCancelSchemaAction is dispatched', () => {
    const state = editState;
    const action = new MetadataRegistryCancelSchemaAction();
    const newState = metadataRegistryReducer(state, action);

    expect(newState.editSchema).toEqual(null);
  });

  it('should update the current state to add a given schema to the selectedSchemas when MetadataRegistrySelectSchemaAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistrySelectSchemaAction(schema);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedSchemas).toContain(schema);
    expect(newState.selectedSchemas).toContain(schema2);
  });

  it('should update the current state to remove a given schema to the selectedSchemas when MetadataRegistryDeselectSchemaAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistryDeselectSchemaAction(schema2);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedSchemas).toEqual([]);
  });

  it('should update the current state to remove a given schema to the selectedSchemas when MetadataRegistryDeselectAllSchemaAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistryDeselectAllSchemaAction();
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedSchemas).toEqual([]);
  });

  it('should update the current state to change the editField to a new field when MetadataRegistryEditFieldAction is dispatched', () => {
    const state = editState;
    const action = new MetadataRegistryEditFieldAction(field2);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.editField).toEqual(field2);
  });

  it('should update the current state to remove the editField from the state when MetadataRegistryCancelFieldAction is dispatched', () => {
    const state = editState;
    const action = new MetadataRegistryCancelFieldAction();
    const newState = metadataRegistryReducer(state, action);

    expect(newState.editField).toEqual(null);
  });

  it('should update the current state to add a given field to the selectedFields when MetadataRegistrySelectFieldAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistrySelectFieldAction(field);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedFields).toContain(field);
    expect(newState.selectedFields).toContain(field2);
  });

  it('should update the current state to remove a given field to the selectedFields when MetadataRegistryDeselectFieldAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistryDeselectFieldAction(field2);
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedFields).toEqual([]);
  });

  it('should update the current state to remove a given field to the selectedFields when MetadataRegistryDeselectAllFieldAction is dispatched', () => {
    const state = selectState;
    const action = new MetadataRegistryDeselectAllFieldAction();
    const newState = metadataRegistryReducer(state, action);

    expect(newState.selectedFields).toEqual([]);
  });
});
