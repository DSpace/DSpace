import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Store, StoreModule } from '@ngrx/store';
import { TranslateModule } from '@ngx-translate/core';
import { Observable, of as observableOf } from 'rxjs';
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
} from '../../admin/admin-registries/metadata-registry/metadata-registry.actions';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { StoreMock } from '../../shared/testing/store.mock';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { MetadataField } from '../metadata/metadata-field.model';
import { MetadataSchema } from '../metadata/metadata-schema.model';
import { RegistryService } from './registry.service';
import { storeModuleConfig } from '../../app.reducer';
import { MetadataSchemaDataService } from '../data/metadata-schema-data.service';
import { MetadataFieldDataService } from '../data/metadata-field-data.service';
import { createNoContentRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { RemoteData } from '../data/remote-data';
import { NoContent } from '../shared/NoContent.model';
import { FindListOptions } from '../data/find-list-options.model';

@Component({ template: '' })
class DummyComponent {
}

describe('RegistryService', () => {
  let registryService: RegistryService;
  let mockStore;
  let metadataSchemaService: MetadataSchemaDataService;
  let metadataFieldService: MetadataFieldDataService;

  let options: FindListOptions;
  let mockSchemasList: MetadataSchema[];
  let mockFieldsList: MetadataField[];

  function init() {
    options = Object.assign(new FindListOptions(), {
      currentPage: 1,
      elementsPerPage: 20
    });

    mockSchemasList = [
      Object.assign(new MetadataSchema(), {
        id: 1,
        _links: {
          self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadataschemas/1' }
        },
        prefix: 'dc',
        namespace: 'http://dublincore.org/documents/dcmi-terms/',
        type: MetadataSchema.type
      }),
      Object.assign(new MetadataSchema(), {
        id: 2,
        _links: {
          self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadataschemas/2' }
        },
        prefix: 'mock',
        namespace: 'http://dspace.org/mockschema',
        type: MetadataSchema.type
      })
    ];

    mockFieldsList = [
      Object.assign(new MetadataField(),
        {
          id: 1,
          _links: {
            self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadatafields/8' }
          },
          element: 'contributor',
          qualifier: 'advisor',
          scopeNote: null,
          schema: createSuccessfulRemoteDataObject$(mockSchemasList[0]),
          type: MetadataField.type
        }),
      Object.assign(new MetadataField(),
        {
          id: 2,
          _links: {
            self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadatafields/9' }
          },
          element: 'contributor',
          qualifier: 'author',
          scopeNote: null,
          schema: createSuccessfulRemoteDataObject$(mockSchemasList[0]),
          type: MetadataField.type
        }),
      Object.assign(new MetadataField(),
        {
          id: 3,
          _links: {
            self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadatafields/10' }
          },
          element: 'contributor',
          qualifier: 'editor',
          scopeNote: 'test scope note',
          schema: createSuccessfulRemoteDataObject$(mockSchemasList[1]),
          type: MetadataField.type
        }),
      Object.assign(new MetadataField(),
        {
          id: 4,
          _links: {
            self: { href: 'https://dspace7.4science.it/dspace-spring-rest/api/core/metadatafields/11' }
          },
          element: 'contributor',
          qualifier: 'illustrator',
          scopeNote: null,
          schema: createSuccessfulRemoteDataObject$(mockSchemasList[1]),
          type: MetadataField.type
        })
    ];

    metadataSchemaService = jasmine.createSpyObj('metadataSchemaService', {
      findAll: createSuccessfulRemoteDataObject$(createPaginatedList(mockSchemasList)),
      findById: createSuccessfulRemoteDataObject$(mockSchemasList[0]),
      createOrUpdateMetadataSchema: createSuccessfulRemoteDataObject$(mockSchemasList[0]),
      delete: createNoContentRemoteDataObject$(),
      clearRequests: observableOf('href')
    });

    metadataFieldService = jasmine.createSpyObj('metadataFieldService', {
      findAll: createSuccessfulRemoteDataObject$(createPaginatedList(mockFieldsList)),
      findById: createSuccessfulRemoteDataObject$(mockFieldsList[0]),
      create: createSuccessfulRemoteDataObject$(mockFieldsList[0]),
      put: createSuccessfulRemoteDataObject$(mockFieldsList[0]),
      delete: createNoContentRemoteDataObject$(),
      clearRequests: observableOf('href')
    });
  }

  beforeEach(() => {
    init();
    TestBed.configureTestingModule({
      imports: [CommonModule, StoreModule.forRoot({}, storeModuleConfig), TranslateModule.forRoot()],
      declarations: [
        DummyComponent
      ],
      providers: [
        { provide: Store, useClass: StoreMock },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: MetadataSchemaDataService, useValue: metadataSchemaService },
        { provide: MetadataFieldDataService, useValue: metadataFieldService },
        RegistryService
      ]
    });
    registryService = TestBed.inject(RegistryService);
    mockStore = TestBed.inject(Store);
  });

  describe('when requesting metadataschemas', () => {
    let result;

    beforeEach(() => {
      result = registryService.getMetadataSchemas(options);
    });

    it('should call metadataSchemaService.findAll', (done) => {
      result.subscribe(() => {
        expect(metadataSchemaService.findAll).toHaveBeenCalled();
        done();
      });
    });
  });

  describe('when requesting metadataschema by name', () => {
    let result;

    beforeEach(() => {
      result = registryService.getMetadataSchemaByPrefix(mockSchemasList[0].prefix);
    });

    it('should call metadataSchemaService.findById with the correct ID', (done) => {
      result.subscribe(() => {
        expect(metadataSchemaService.findById).toHaveBeenCalledWith(`${mockSchemasList[0].id}`, true, true);
        done();
      });
    });
  });

  describe('when dispatching to the store', () => {
    beforeEach(() => {
      spyOn(mockStore, 'dispatch');
    });

    describe('when calling editMetadataSchema', () => {
      beforeEach(() => {
        registryService.editMetadataSchema(mockSchemasList[0]);
      });

      it('should dispatch a MetadataRegistryEditSchemaAction with the correct schema', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryEditSchemaAction(mockSchemasList[0]));
      });
    });

    describe('when calling cancelEditMetadataSchema', () => {
      beforeEach(() => {
        registryService.cancelEditMetadataSchema();
      });

      it('should dispatch a MetadataRegistryCancelSchemaAction', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryCancelSchemaAction());
      });
    });

    describe('when calling selectMetadataSchema', () => {
      beforeEach(() => {
        registryService.selectMetadataSchema(mockSchemasList[0]);
      });

      it('should dispatch a MetadataRegistrySelectSchemaAction with the correct schema', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistrySelectSchemaAction(mockSchemasList[0]));
      });
    });

    describe('when calling deselectMetadataSchema', () => {
      beforeEach(() => {
        registryService.deselectMetadataSchema(mockSchemasList[0]);
      });

      it('should dispatch a MetadataRegistryDeselectSchemaAction with the correct schema', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryDeselectSchemaAction(mockSchemasList[0]));
      });
    });

    describe('when calling deselectAllMetadataSchema', () => {
      beforeEach(() => {
        registryService.deselectAllMetadataSchema();
      });

      it('should dispatch a MetadataRegistryDeselectAllSchemaAction', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryDeselectAllSchemaAction());
      });
    });

    describe('when calling editMetadataField', () => {
      beforeEach(() => {
        registryService.editMetadataField(mockFieldsList[0]);
      });

      it('should dispatch a MetadataRegistryEditFieldAction with the correct Field', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryEditFieldAction(mockFieldsList[0]));
      });
    });

    describe('when calling cancelEditMetadataField', () => {
      beforeEach(() => {
        registryService.cancelEditMetadataField();
      });

      it('should dispatch a MetadataRegistryCancelFieldAction', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryCancelFieldAction());
      });
    });

    describe('when calling selectMetadataField', () => {
      beforeEach(() => {
        registryService.selectMetadataField(mockFieldsList[0]);
      });

      it('should dispatch a MetadataRegistrySelectFieldAction with the correct Field', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistrySelectFieldAction(mockFieldsList[0]));
      });
    });

    describe('when calling deselectMetadataField', () => {
      beforeEach(() => {
        registryService.deselectMetadataField(mockFieldsList[0]);
      });

      it('should dispatch a MetadataRegistryDeselectFieldAction with the correct Field', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryDeselectFieldAction(mockFieldsList[0]));
      });
    });

    describe('when calling deselectAllMetadataField', () => {
      beforeEach(() => {
        registryService.deselectAllMetadataField();
      });

      it('should dispatch a MetadataRegistryDeselectAllFieldAction', () => {
        expect(mockStore.dispatch).toHaveBeenCalledWith(new MetadataRegistryDeselectAllFieldAction());
      });
    });
  });

  describe('when createOrUpdateMetadataSchema is called', () => {
    let result: Observable<MetadataSchema>;

    beforeEach(() => {
      result = registryService.createOrUpdateMetadataSchema(mockSchemasList[0]);
    });

    it('should return the created/updated metadata schema', (done) => {
      result.subscribe((schema: MetadataSchema) => {
        expect(schema).toEqual(mockSchemasList[0]);
        done();
      });
    });
  });

  describe('when createMetadataField is called', () => {
    let result: Observable<MetadataField>;

    beforeEach(() => {
      result = registryService.createMetadataField(mockFieldsList[0], mockSchemasList[0]);
    });

    it('should return the created metadata field', (done) => {
      result.subscribe((field: MetadataField) => {
        expect(field).toEqual(mockFieldsList[0]);
        done();
      });
    });
  });

  describe('when createMetadataField is called with a blank qualifier', () => {
    let result: Observable<MetadataField>;
    let metadataField: MetadataField;

    beforeEach(() => {
      metadataField = mockFieldsList[0];
      metadataField.qualifier = '';
      result = registryService.createMetadataField(metadataField, mockSchemasList[0]);
    });

    it('should return the created metadata field with a null qualifier', (done) => {
      metadataField.qualifier = null;
      result.subscribe((field: MetadataField) => {
        expect(field).toEqual(metadataField);
        done();
      });
    });
  });

  describe('when updateMetadataField is called', () => {
    let result: Observable<MetadataField>;

    beforeEach(() => {
      result = registryService.updateMetadataField(mockFieldsList[0]);
    });

    it('should return the updated metadata field', (done) => {
      result.subscribe((field: MetadataField) => {
        expect(field).toEqual(mockFieldsList[0]);
        done();
      });
    });
  });

  describe('when updateMetadataField is called with a blank qualifier', () => {
    let result: Observable<MetadataField>;
    let metadataField: MetadataField;

    beforeEach(() => {
      metadataField = mockFieldsList[0];
      metadataField.qualifier = '';
      result = registryService.updateMetadataField(metadataField);
    });

    it('should return the updated metadata field with a null qualifier', (done) => {
      metadataField.qualifier = null;
      result.subscribe((field: MetadataField) => {
        expect(field).toEqual(metadataField);
        done();
      });
    });
  });

  describe('when deleteMetadataSchema is called', () => {
    let result: Observable<RemoteData<NoContent>>;

    beforeEach(() => {
      result = registryService.deleteMetadataSchema(mockSchemasList[0].id);
    });

    it('should defer to MetadataSchemaDataService.delete', () => {
      expect(metadataSchemaService.delete).toHaveBeenCalledWith(`${mockSchemasList[0].id}`);
    });

    it('should return a successful response', () => {
      result.subscribe((response: RemoteData<NoContent>) => {
        expect(response.hasSucceeded).toBe(true);
      });
    });
  });

  describe('when deleteMetadataField is called', () => {
    let result: Observable<RemoteData<NoContent>>;

    beforeEach(() => {
      result = registryService.deleteMetadataField(mockFieldsList[0].id);
    });

    it('should defer to MetadataFieldDataService.delete', () => {
      expect(metadataFieldService.delete).toHaveBeenCalledWith(`${mockFieldsList[0].id}`);
    });

    it('should return a successful response', () => {
      result.subscribe((response: RemoteData<NoContent>) => {
        expect(response.hasSucceeded).toBe(true);
      });
    });
  });

  describe('when clearMetadataSchemaRequests is called', () => {
    beforeEach(() => {
      registryService.clearMetadataSchemaRequests();
    });

    it('should remove the requests related to metadata schemas from cache', () => {
      expect(metadataSchemaService.clearRequests).toHaveBeenCalled();
    });
  });

  describe('when clearMetadataFieldRequests is called', () => {
    beforeEach(() => {
      registryService.clearMetadataFieldRequests();
    });

    it('should remove the requests related to metadata fields from cache', () => {
      expect(metadataFieldService.clearRequests).toHaveBeenCalled();
    });
  });
});
