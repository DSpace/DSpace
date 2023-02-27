import {
  DsDynamicLookupRelationExternalSourceTabComponent
} from './dynamic-lookup-relation-external-source-tab.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../../../../utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { EventEmitter, NO_ERRORS_SCHEMA } from '@angular/core';
import { PaginatedSearchOptions } from '../../../../../search/models/paginated-search-options.model';
import { SearchConfigurationService } from '../../../../../../core/shared/search/search-configuration.service';
import { of as observableOf, EMPTY } from 'rxjs';
import {
  createFailedRemoteDataObject$,
  createPendingRemoteDataObject$,
  createSuccessfulRemoteDataObject$
} from '../../../../../remote-data.utils';
import { ExternalSourceDataService } from '../../../../../../core/data/external-source-data.service';
import { ExternalSource } from '../../../../../../core/shared/external-source.model';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { ExternalSourceEntry } from '../../../../../../core/shared/external-source-entry.model';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SelectableListService } from '../../../../../object-list/selectable-list/selectable-list.service';
import { Item } from '../../../../../../core/shared/item.model';
import { Collection } from '../../../../../../core/shared/collection.model';
import { RelationshipOptions } from '../../../models/relationship-options.model';
import { createPaginatedList } from '../../../../../testing/utils.test';
import { PaginationService } from '../../../../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../../../testing/pagination-service.stub';
import { ItemType } from '../../../../../../core/shared/item-relationships/item-type.model';
import {
  ThemedExternalSourceEntryImportModalComponent
} from './external-source-entry-import-modal/themed-external-source-entry-import-modal.component';

describe('DsDynamicLookupRelationExternalSourceTabComponent', () => {
  let component: DsDynamicLookupRelationExternalSourceTabComponent;
  let fixture: ComponentFixture<DsDynamicLookupRelationExternalSourceTabComponent>;
  let pSearchOptions;
  let externalSourceService;
  let selectableListService;
  let modalService;

  const itemType = Object.assign(new ItemType(), { label: 'Person' });
  const externalSource = {
    id: 'orcidV2',
    name: 'orcidV2',
    hierarchical: false,
    entityTypes: createSuccessfulRemoteDataObject$(createPaginatedList([itemType]))
  } as ExternalSource;
  const externalEntries = [
    Object.assign({
      id: '0001-0001-0001-0001',
      display: 'John Doe',
      value: 'John, Doe',
      metadata: {
        'dc.identifier.uri': [
          {
            value: 'https://orcid.org/0001-0001-0001-0001'
          }
        ]
      }
    }),
    Object.assign({
      id: '0001-0001-0001-0002',
      display: 'Sampson Megan',
      value: 'Sampson, Megan',
      metadata: {
        'dc.identifier.uri': [
          {
            value: 'https://orcid.org/0001-0001-0001-0002'
          }
        ]
      }
    }),
    Object.assign({
      id: '0001-0001-0001-0003',
      display: 'Edwards Anna',
      value: 'Edwards, Anna',
      metadata: {
        'dc.identifier.uri': [
          {
            value: 'https://orcid.org/0001-0001-0001-0003'
          }
        ]
      }
    })
  ] as ExternalSourceEntry[];
  const item = Object.assign(new Item(), { id: 'submission-item' });
  const collection = Object.assign(new Collection(), { id: 'submission-collection' });
  const relationship = Object.assign(new RelationshipOptions(), { relationshipType: 'isAuthorOfPublication' });
  const label = 'Author';

  function init() {
    pSearchOptions = new PaginatedSearchOptions({
      query: 'test'
    });
    externalSourceService = jasmine.createSpyObj('externalSourceService', {
      getExternalSourceEntries: createSuccessfulRemoteDataObject$(createPaginatedList(externalEntries))
    });
    selectableListService = jasmine.createSpyObj('selectableListService', ['selectSingle']);
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [DsDynamicLookupRelationExternalSourceTabComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([]), NgbModule, BrowserAnimationsModule],
      providers: [
        {
          provide: SearchConfigurationService, useValue: {
            paginatedSearchOptions: observableOf(pSearchOptions)
          }
        },
        { provide: ExternalSourceDataService, useValue: externalSourceService },
        { provide: SelectableListService, useValue: selectableListService },
        { provide: PaginationService, useValue: new PaginationServiceStub() }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsDynamicLookupRelationExternalSourceTabComponent);
    component = fixture.componentInstance;
    component.externalSource = externalSource;
    component.item = item;
    component.collection = collection;
    component.relationship = relationship;
    component.label = label;
    modalService = (component as any).modalService;
    fixture.detectChanges();
  });

  describe('when the external entries finished loading successfully', () => {
    it('should display a ds-viewable-collection component', () => {
      const viewableCollection = fixture.debugElement.query(By.css('ds-viewable-collection'));
      expect(viewableCollection).toBeDefined();
    });
  });

  describe('when the external entries are loading', () => {
    beforeEach(() => {
      component.entriesRD$ = createPendingRemoteDataObject$();
      fixture.detectChanges();
    });

    it('should not display a ds-viewable-collection component', () => {
      const viewableCollection = fixture.debugElement.query(By.css('ds-viewable-collection'));
      expect(viewableCollection).toBeNull();
    });

    it('should display a ds-themed-loading component', () => {
      const loading = fixture.debugElement.query(By.css('ds-themed-loading'));
      expect(loading).not.toBeNull();
    });
  });

  describe('when the external entries failed loading', () => {
    beforeEach(() => {
      component.entriesRD$ = createFailedRemoteDataObject$('server error', 500);
      fixture.detectChanges();
    });

    it('should not display a ds-viewable-collection component', () => {
      const viewableCollection = fixture.debugElement.query(By.css('ds-viewable-collection'));
      expect(viewableCollection).toBeNull();
    });

    it('should display a ds-error component', () => {
      const error = fixture.debugElement.query(By.css('ds-error'));
      expect(error).not.toBeNull();
    });
  });

  describe('when the external entries return an empty list', () => {
    beforeEach(() => {
      component.entriesRD$ = createSuccessfulRemoteDataObject$(createPaginatedList([]));
      fixture.detectChanges();
    });

    it('should not display a ds-viewable-collection component', () => {
      const viewableCollection = fixture.debugElement.query(By.css('ds-viewable-collection'));
      expect(viewableCollection).toBeNull();
    });

    it('should display a message the list is empty', () => {
      const empty = fixture.debugElement.query(By.css('#empty-external-entry-list'));
      expect(empty).not.toBeNull();
    });
  });

  describe('import', () => {
    beforeEach(() => {
      spyOn(modalService, 'open').and.returnValue(Object.assign({ componentInstance: Object.assign({ importedObject: new EventEmitter<any>(), compRef$: EMPTY }) }));
      component.modalRef = modalService.open(ThemedExternalSourceEntryImportModalComponent, { size: 'lg', container: 'ds-dynamic-lookup-relation-modal' });
      component.import(externalEntries[0]);
    });

    it('should open a new ExternalSourceEntryImportModalComponent', () => {
      expect(modalService.open).toHaveBeenCalledWith(ThemedExternalSourceEntryImportModalComponent, jasmine.any(Object));
    });
  });
});
