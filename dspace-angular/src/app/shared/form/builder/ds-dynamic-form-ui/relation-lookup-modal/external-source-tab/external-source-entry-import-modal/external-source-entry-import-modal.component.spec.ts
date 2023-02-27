import { ExternalSourceEntryImportModalComponent, ImportType } from './external-source-entry-import-modal.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NgbActiveModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { LookupRelationService } from '../../../../../../../core/data/lookup-relation.service';
import { ExternalSourceEntry } from '../../../../../../../core/shared/external-source-entry.model';
import { Item } from '../../../../../../../core/shared/item.model';
import { ItemSearchResult } from '../../../../../../object-collection/shared/item-search-result.model';
import { Collection } from '../../../../../../../core/shared/collection.model';
import { RelationshipOptions } from '../../../../models/relationship-options.model';
import { SelectableListService } from '../../../../../../object-list/selectable-list/selectable-list.service';
import { ItemDataService } from '../../../../../../../core/data/item-data.service';
import { NotificationsService } from '../../../../../../notifications/notifications.service';
import { createSuccessfulRemoteDataObject$ } from '../../../../../../remote-data.utils';
import { createPaginatedList } from '../../../../../../testing/utils.test';

describe('DsDynamicLookupRelationExternalSourceTabComponent', () => {
  let component: ExternalSourceEntryImportModalComponent;
  let fixture: ComponentFixture<ExternalSourceEntryImportModalComponent>;
  let lookupRelationService: LookupRelationService;
  let selectService: SelectableListService;
  let itemService: ItemDataService;
  let notificationsService: NotificationsService;
  let modalStub: NgbActiveModal;

  const uri = 'https://orcid.org/0001-0001-0001-0001';
  const entry = Object.assign(new ExternalSourceEntry(), {
    id: '0001-0001-0001-0001',
    display: 'John Doe',
    value: 'John, Doe',
    metadata: {
      'dc.identifier.uri': [
        {
          value: uri
        }
      ]
    }
  });

  const label = 'Author';
  const relationship = Object.assign(new RelationshipOptions(), { relationshipType: 'isAuthorOfPublication' });
  const submissionCollection = Object.assign(new Collection(), { uuid: '9398affe-a977-4992-9a1d-6f00908a259f' });
  const submissionItem = Object.assign(new Item(), { uuid: '26224069-5f99-412a-9e9b-7912a7e35cb1' });
  const item1 = Object.assign(new Item(), { uuid: 'e1c51c69-896d-42dc-8221-1d5f2ad5516e' });
  const item2 = Object.assign(new Item(), { uuid: 'c8279647-1acc-41ae-b036-951d5f65649b' });
  const item3 = Object.assign(new Item(), { uuid: 'c3bcbff5-ec0c-4831-8e4c-94b9c933ccac' });
  const searchResult1 = Object.assign(new ItemSearchResult(), { indexableObject: item1 });
  const searchResult2 = Object.assign(new ItemSearchResult(), { indexableObject: item2 });
  const searchResult3 = Object.assign(new ItemSearchResult(), { indexableObject: item3 });
  const importedItem = Object.assign(new Item(), { uuid: '5d0098fc-344a-4067-a57d-457092b72e82' });

  function init() {
    lookupRelationService = jasmine.createSpyObj('lookupRelationService', {
      getLocalResults: createSuccessfulRemoteDataObject$(createPaginatedList([searchResult1, searchResult2, searchResult3])),
      removeLocalResultsCache: {}
    });
    selectService = jasmine.createSpyObj('selectService', ['deselectAll']);
    notificationsService = jasmine.createSpyObj('notificationsService', ['success']);
    itemService = jasmine.createSpyObj('itemService', {
      importExternalSourceEntry: createSuccessfulRemoteDataObject$(importedItem)
    });
    modalStub = jasmine.createSpyObj('modal', ['close']);
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [ExternalSourceEntryImportModalComponent],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([]), NgbModule],
      providers: [
        { provide: LookupRelationService, useValue: lookupRelationService },
        { provide: SelectableListService, useValue: selectService },
        { provide: NotificationsService, useValue: notificationsService },
        { provide: ItemDataService, useValue: itemService },
        { provide: NgbActiveModal, useValue: modalStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExternalSourceEntryImportModalComponent);
    component = fixture.componentInstance;
    component.externalSourceEntry = entry;
    component.label = label;
    component.relationship = relationship;
    component.item = submissionItem;
    fixture.detectChanges();
  });

  describe('close', () => {
    beforeEach(() => {
      component.close();
    });

    it('should close the modal', () => {
      expect(modalStub.close).toHaveBeenCalled();
    });
  });

  describe('selectEntity', () => {
    const entity = Object.assign(new Item(), { uuid: 'd8698de5-5b05-4ea4-9d02-da73803a50f9' });

    beforeEach(() => {
      component.selectEntity(entity);
    });

    it('should set selected entity', () => {
      expect(component.selectedEntity).toBe(entity);
    });

    it('should set the import type to local entity', () => {
      expect(component.selectedImportType).toEqual(ImportType.LocalEntity);
    });
  });

  describe('deselectEntity', () => {
    const entity = Object.assign(new Item(), { uuid: 'd8698de5-5b05-4ea4-9d02-da73803a50f9' });

    beforeEach(() => {
      component.selectedImportType = ImportType.LocalEntity;
      component.selectedEntity = entity;
      component.deselectEntity();
    });

    it('should remove the selected entity', () => {
      expect(component.selectedEntity).toBeUndefined();
    });

    it('should set the import type to none', () => {
      expect(component.selectedImportType).toEqual(ImportType.None);
    });
  });

  describe('selectNewEntity', () => {
    describe('when current import type is set to new entity', () => {
      beforeEach(() => {
        component.selectedImportType = ImportType.NewEntity;
        component.selectNewEntity();
      });

      it('should set the import type to none', () => {
        expect(component.selectedImportType).toEqual(ImportType.None);
      });
    });

    describe('when current import type is not set to new entity', () => {
      beforeEach(() => {
        component.selectedImportType = ImportType.None;
        component.selectNewEntity();
      });

      it('should set the import type to new entity', () => {
        expect(component.selectedImportType).toEqual(ImportType.NewEntity);
      });

      it('should deselect the entity and authority list', () => {
        expect(selectService.deselectAll).toHaveBeenCalledWith(component.entityListId);
        expect(selectService.deselectAll).toHaveBeenCalledWith(component.authorityListId);
      });
    });
  });

  describe('selectNewAuthority', () => {
    describe('when current import type is set to new authority', () => {
      beforeEach(() => {
        component.selectedImportType = ImportType.NewAuthority;
        component.selectNewAuthority();
      });

      it('should set the import type to none', () => {
        expect(component.selectedImportType).toEqual(ImportType.None);
      });
    });

    describe('when current import type is not set to new authority', () => {
      beforeEach(() => {
        component.selectedImportType = ImportType.None;
        component.selectNewAuthority();
      });

      it('should set the import type to new authority', () => {
        expect(component.selectedImportType).toEqual(ImportType.NewAuthority);
      });

      it('should deselect the entity and authority list', () => {
        expect(selectService.deselectAll).toHaveBeenCalledWith(component.entityListId);
        expect(selectService.deselectAll).toHaveBeenCalledWith(component.authorityListId);
      });
    });
  });
});
