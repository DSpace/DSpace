import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { NgxPaginationModule } from 'ngx-pagination';
import { cold } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';

import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createTestComponent } from '../../../shared/testing/utils.test';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { SubmissionService } from '../../submission.service';
import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import { SectionsService } from '../sections.service';
import { SectionsServiceStub } from '../../../shared/testing/sections-service.stub';
import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { getMockFormOperationsService } from '../../../shared/mocks/form-operations-service.mock';
import { getMockFormService } from '../../../shared/mocks/form-service.mock';
import { FormService } from '../../../shared/form/form.service';
import { SubmissionFormsConfigDataService } from '../../../core/config/submission-forms-config-data.service';
import { SectionDataObject } from '../models/section-data.model';
import { SectionsType } from '../sections-type';
import { mockSubmissionCollectionId, mockSubmissionId } from '../../../shared/mocks/submission.mock';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { SubmissionSectionIdentifiersComponent } from './section-identifiers.component';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { SectionFormOperationsService } from '../form/section-form-operations.service';
import { SubmissionScopeType } from '../../../core/submission/submission-scope-type';
import { License } from '../../../core/shared/license.model';
import { Collection } from '../../../core/shared/collection.model';
import { ObjNgFor } from '../../../shared/utils/object-ngfor.pipe';
import { VarDirective } from '../../../shared/utils/var.directive';
import { WorkspaceitemSectionIdentifiersObject } from '../../../core/submission/models/workspaceitem-section-identifiers.model';
import { Item } from '../../../core/shared/item.model';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { PaginationServiceStub } from '../../../shared/testing/pagination-service.stub';

function getMockSubmissionFormsConfigService(): SubmissionFormsConfigDataService {
  return jasmine.createSpyObj('FormOperationsService', {
    getConfigAll: jasmine.createSpy('getConfigAll'),
    getConfigByHref: jasmine.createSpy('getConfigByHref'),
    getConfigByName: jasmine.createSpy('getConfigByName'),
    getConfigBySearch: jasmine.createSpy('getConfigBySearch')
  });
}

function getMockCollectionDataService(): CollectionDataService {
  return jasmine.createSpyObj('CollectionDataService', {
    findById: jasmine.createSpy('findById'),
    findByHref: jasmine.createSpy('findByHref')
  });
}

const mockItem = Object.assign(new Item(), {
  id: 'fake-match-id',
  handle: 'fake/handle',
  metadata: {
    'dc.title': [
      {
        language: null,
        value: 'mockmatch'
      }
    ]
  },
});

// Mock identifier data to use with tests
const identifierData: WorkspaceitemSectionIdentifiersObject = {
  identifiers: [{
    value: 'https://doi.org/10.33515/dspace-61',
    identifierType: 'doi',
    identifierStatus: 'TO_BE_REGISTERED',
    type: 'identifier'
  },
  {
    value: '123456789/418',
    identifierType: 'handle',
    identifierStatus: null,
    type: 'identifier'
  }
  ],
  displayTypes: ['doi', 'handle']
};

// Mock section object to use with tests
const sectionObject: SectionDataObject = {
  config: 'https://dspace.org/api/config/submissionforms/identifiers',
  mandatory: true,
  opened: true,
  data: identifierData,
  errorsToShow: [],
  serverValidationErrors: [],
  header: 'submission.sections.submit.progressbar.identifiers',
  id: 'identifiers',
  sectionType: SectionsType.Identifiers,
  sectionVisibility: null
};

describe('SubmissionSectionIdentifiersComponent test suite', () => {
  let comp: SubmissionSectionIdentifiersComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionSectionIdentifiersComponent>;
  let submissionServiceStub: any = new SubmissionServiceStub();
  const sectionsServiceStub: any = new SectionsServiceStub();
  let formService: any;
  let formOperationsService: any;
  let formBuilderService: any;
  let collectionDataService: any;

  const submissionId = mockSubmissionId;
  const collectionId = mockSubmissionCollectionId;
  const jsonPatchOpBuilder: any = jasmine.createSpyObj('jsonPatchOpBuilder', {
    add: jasmine.createSpy('add'),
    replace: jasmine.createSpy('replace'),
    remove: jasmine.createSpy('remove'),
  });

  const licenseText = 'License text';
  const mockCollection = Object.assign(new Collection(), {
    name: 'Community 1-Collection 1',
    id: collectionId,
    metadata: [
      {
        key: 'dc.title',
        language: 'en_US',
        value: 'Community 1-Collection 1'
      }],
    license: createSuccessfulRemoteDataObject$(Object.assign(new License(), { text: licenseText }))
  });
  const paginationService = new PaginationServiceStub();

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NgxPaginationModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [
        SubmissionSectionIdentifiersComponent,
        TestComponent,
        ObjNgFor,
        VarDirective,
      ],
      providers: [
        { provide: CollectionDataService, useValue: getMockCollectionDataService() },
        { provide: SectionFormOperationsService, useValue: getMockFormOperationsService() },
        { provide: FormService, useValue: getMockFormService() },
        { provide: JsonPatchOperationsBuilder, useValue: jsonPatchOpBuilder },
        { provide: SubmissionFormsConfigDataService, useValue: getMockSubmissionFormsConfigService() },
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: SectionsService, useClass: SectionsServiceStub },
        { provide: SubmissionService, useClass: SubmissionServiceStub },
        { provide: 'collectionIdProvider', useValue: collectionId },
        { provide: 'sectionDataProvider', useValue: sectionObject },
        { provide: 'submissionIdProvider', useValue: submissionId },
        { provide: PaginationService, useValue: paginationService },
        ChangeDetectorRef,
        FormBuilderService,
        SubmissionSectionIdentifiersComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then();
  }));

  // First test to check the correct component creation
  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      sectionsServiceStub.isSectionReadOnly.and.returnValue(observableOf(false));
      sectionsServiceStub.getSectionErrors.and.returnValue(observableOf([]));
      sectionsServiceStub.getSectionData.and.returnValue(observableOf(identifierData));
      const html = `<ds-submission-section-identifiers></ds-submission-section-identifiers>`;
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionSectionIdentifiersComponent', inject([SubmissionSectionIdentifiersComponent], (idComp: SubmissionSectionIdentifiersComponent) => {
      expect(idComp).toBeDefined();
    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionSectionIdentifiersComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      submissionServiceStub = TestBed.inject(SubmissionService);
      formService = TestBed.inject(FormService);
      formBuilderService = TestBed.inject(FormBuilderService);
      formOperationsService = TestBed.inject(SectionFormOperationsService);
      collectionDataService = TestBed.inject(CollectionDataService);
      compAsAny.pathCombiner = new JsonPatchOperationPathCombiner('sections', sectionObject.id);
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    // Test initialisation of the submission section
    it('Should init section properly', () => {
      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));
      sectionsServiceStub.getSectionErrors.and.returnValue(observableOf([]));
      sectionsServiceStub.isSectionReadOnly.and.returnValue(observableOf(false));
      compAsAny.submissionService.getSubmissionScope.and.returnValue(SubmissionScopeType.WorkspaceItem);
      spyOn(comp, 'getSectionStatus').and.returnValue(observableOf(true));
      spyOn(comp, 'getIdentifierData').and.returnValue(observableOf(identifierData));
      expect(comp.isLoading).toBeTruthy();
      comp.onSectionInit();
      fixture.detectChanges();
      expect(comp.isLoading).toBeFalsy();
    });

    // The following tests look for proper logic in the getSectionStatus() implementation
    // These are very simple as we don't really have a 'false' state unless we're still loading
    it('Should return TRUE if the isLoading is FALSE', () => {
      compAsAny.isLoading = false;
      expect(compAsAny.getSectionStatus()).toBeObservable(cold('(a|)', {
        a: true
      }));
    });
    it('Should return FALSE if the identifier data is missing handle', () => {
      compAsAny.isLoadin = true;
      expect(compAsAny.getSectionStatus()).toBeObservable(cold('(a|)', {
        a: false
      }));
    });
  });

});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

}
