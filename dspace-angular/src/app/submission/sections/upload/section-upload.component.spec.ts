import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

import { TranslateModule } from '@ngx-translate/core';
import { cold } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';

import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createTestComponent } from '../../../shared/testing/utils.test';
import { SubmissionObjectState } from '../../objects/submission-objects.reducer';
import { SubmissionService } from '../../submission.service';
import { SubmissionServiceStub } from '../../../shared/testing/submission-service.stub';
import { SectionsService } from '../sections.service';
import { SectionsServiceStub } from '../../../shared/testing/sections-service.stub';
import { SubmissionFormsConfigDataService } from '../../../core/config/submission-forms-config-data.service';
import { SectionDataObject } from '../models/section-data.model';
import { SectionsType } from '../sections-type';
import {
  mockGroup,
  mockSubmissionCollectionId,
  mockSubmissionId,
  mockSubmissionState,
  mockUploadConfigResponse,
  mockUploadConfigResponseNotRequired,
  mockUploadFiles,
} from '../../../shared/mocks/submission.mock';
import { SubmissionUploadsConfigDataService } from '../../../core/config/submission-uploads-config-data.service';
import { SectionUploadService } from './section-upload.service';
import { SubmissionSectionUploadComponent } from './section-upload.component';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { Collection } from '../../../core/shared/collection.model';
import { ResourcePolicy } from '../../../core/resource-policy/models/resource-policy.model';
import { ResourcePolicyDataService } from '../../../core/resource-policy/resource-policy-data.service';
import { Group } from '../../../core/eperson/models/group.model';
import { getMockSectionUploadService } from '../../../shared/mocks/section-upload.service.mock';
import { SubmissionUploadsModel } from '../../../core/config/models/config-submission-uploads.model';
import { buildPaginatedList } from '../../../core/data/paginated-list.model';
import { PageInfo } from '../../../core/shared/page-info.model';

function getMockSubmissionUploadsConfigService(): SubmissionFormsConfigDataService {
  return jasmine.createSpyObj('SubmissionUploadsConfigService', {
    getConfigAll: jasmine.createSpy('getConfigAll'),
    getConfigByHref: jasmine.createSpy('getConfigByHref'),
    getConfigByName: jasmine.createSpy('getConfigByName'),
    getConfigBySearch: jasmine.createSpy('getConfigBySearch'),
    findByHref: jasmine.createSpy('findByHref')
  });
}

function getMockCollectionDataService(): CollectionDataService {
  return jasmine.createSpyObj('CollectionDataService', {
    findById: jasmine.createSpy('findById')
  });
}

function getMockGroupEpersonService(): GroupDataService {
  return jasmine.createSpyObj('GroupDataService', {
    findById: jasmine.createSpy('findById'),

  });
}

function getMockResourcePolicyService(): ResourcePolicyDataService {
  return jasmine.createSpyObj('ResourcePolicyService', {
    findByHref: jasmine.createSpy('findByHref')
  });
}

let sectionObject: SectionDataObject;

describe('SubmissionSectionUploadComponent test suite', () => {

  let comp: SubmissionSectionUploadComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionSectionUploadComponent>;
  let submissionServiceStub: SubmissionServiceStub;
  let sectionsServiceStub: SectionsServiceStub;
  let collectionDataService: any;
  let groupService: any;
  let resourcePolicyService: any;
  let uploadsConfigService: any;
  let bitstreamService: any;

  let submissionId: string;
  let collectionId: string;
  let submissionState: SubmissionObjectState;
  let mockCollection: Collection;
  let mockDefaultAccessCondition: ResourcePolicy;
  let prepareComp;

  beforeEach(waitForAsync(() => {
    sectionObject = {
      config: 'https://dspace7.4science.it/or2018/api/config/submissionforms/upload',
      mandatory: true,
      data: {
        files: []
      },
      errorsToShow: [],
      serverValidationErrors: [],
      header: 'submit.progressbar.describe.upload',
      id: 'upload-id',
      sectionType: SectionsType.Upload
    };
    submissionId = mockSubmissionId;
    collectionId = mockSubmissionCollectionId;
    submissionState = Object.assign({}, mockSubmissionState[mockSubmissionId]) as any;
    mockCollection = Object.assign(new Collection(), {
      name: 'Community 1-Collection 1',
      id: collectionId,
      metadata: [
        {
          key: 'dc.title',
          language: 'en_US',
          value: 'Community 1-Collection 1'
        }],
      _links: {
        defaultAccessConditions: collectionId + '/defaultAccessConditions'
      }
    });

    mockDefaultAccessCondition = Object.assign(new ResourcePolicy(), {
      name: null,
      groupUUID: '11cc35e5-a11d-4b64-b5b9-0052a5d15509',
      id: 20,
      uuid: 'resource-policy-20'
    });
    uploadsConfigService = getMockSubmissionUploadsConfigService();

    submissionServiceStub = new SubmissionServiceStub();

    collectionDataService = getMockCollectionDataService();

    resourcePolicyService = getMockResourcePolicyService();

    groupService = getMockGroupEpersonService();

    bitstreamService = getMockSectionUploadService();

    uploadsConfigService = getMockSubmissionUploadsConfigService();

    prepareComp = () => {
      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));

      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new Collection(), mockCollection, {
        defaultAccessConditions: createSuccessfulRemoteDataObject$(mockDefaultAccessCondition)
      })));

      resourcePolicyService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(mockDefaultAccessCondition));

      uploadsConfigService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(
        buildPaginatedList(new PageInfo(), [mockUploadConfigResponse as any]))
      );

      groupService.findById.and.returnValues(
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup)),
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup))
      );

      bitstreamService.getUploadedFileList.and.returnValue(observableOf([]));
    };

    TestBed.configureTestingModule({
      imports: [
        BrowserModule,
        CommonModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        SubmissionSectionUploadComponent,
        TestComponent
      ],
      providers: [
        { provide: CollectionDataService, useValue: collectionDataService },
        { provide: GroupDataService, useValue: groupService },
        { provide: ResourcePolicyDataService, useValue: resourcePolicyService },
        { provide: SubmissionUploadsConfigDataService, useValue: uploadsConfigService },
        { provide: SectionsService, useClass: SectionsServiceStub },
        { provide: SubmissionService, useValue: submissionServiceStub },
        { provide: SectionUploadService, useValue: bitstreamService },
        { provide: 'sectionDataProvider', useValue: sectionObject },
        { provide: 'submissionIdProvider', useValue: submissionId },
        ChangeDetectorRef,
        SubmissionSectionUploadComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      prepareComp();

      const html = `
        <ds-submission-section-upload></ds-submission-section-upload>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionSectionUploadComponent', inject([SubmissionSectionUploadComponent], (app: SubmissionSectionUploadComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionSectionUploadComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      sectionsServiceStub = TestBed.inject(SectionsService as any);
    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    it('should init component properly', () => {

      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));

      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new Collection(), mockCollection, {
        defaultAccessConditions: createSuccessfulRemoteDataObject$(mockDefaultAccessCondition)
      })));

      resourcePolicyService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(mockDefaultAccessCondition));

      uploadsConfigService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new SubmissionUploadsModel(), mockUploadConfigResponse)));

      groupService.findById.and.returnValues(
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup)),
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup))
      );

      bitstreamService.getUploadedFileList.and.returnValue(observableOf([]));

      comp.onSectionInit();

      const expectedGroupsMap = new Map([
        [mockUploadConfigResponse.accessConditionOptions[1].name, [mockGroup as any]],
        [mockUploadConfigResponse.accessConditionOptions[2].name, [mockGroup as any]],
      ]);

      expect(comp.collectionId).toBe(collectionId);
      expect(comp.collectionName).toBe(mockCollection.name);
      expect(comp.availableAccessConditionOptions.length).toBe(4);
      expect(comp.availableAccessConditionOptions).toEqual(mockUploadConfigResponse.accessConditionOptions as any);
      expect(comp.required$.getValue()).toBe(true);
      expect(compAsAny.subs.length).toBe(2);
      expect(compAsAny.fileList).toEqual([]);
      expect(compAsAny.fileIndexes).toEqual([]);
      expect(compAsAny.fileNames).toEqual([]);

    });

    it('should init file list properly', () => {

      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));

      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));

      resourcePolicyService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(mockDefaultAccessCondition));

      uploadsConfigService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new SubmissionUploadsModel(), mockUploadConfigResponse)));

      groupService.findById.and.returnValues(
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup)),
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup))
      );

      bitstreamService.getUploadedFileList.and.returnValue(observableOf(mockUploadFiles));

      comp.onSectionInit();

      const expectedGroupsMap = new Map([
        [mockUploadConfigResponse.accessConditionOptions[1].name, [mockGroup as any]],
        [mockUploadConfigResponse.accessConditionOptions[2].name, [mockGroup as any]],
      ]);

      expect(comp.collectionId).toBe(collectionId);
      expect(comp.collectionName).toBe(mockCollection.name);
      expect(comp.availableAccessConditionOptions.length).toBe(4);
      expect(comp.availableAccessConditionOptions).toEqual(mockUploadConfigResponse.accessConditionOptions as any);
      expect(comp.required$.getValue()).toBe(true);
      expect(compAsAny.subs.length).toBe(2);
      expect(compAsAny.fileList).toEqual(mockUploadFiles);
      expect(compAsAny.fileIndexes).toEqual(['123456-test-upload']);
      expect(compAsAny.fileNames).toEqual(['123456-test-upload.jpg']);

    });

    it('should properly read the section status when required is true', () => {
      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));

      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));

      resourcePolicyService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(mockDefaultAccessCondition));

      uploadsConfigService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new SubmissionUploadsModel(), mockUploadConfigResponse)));

      groupService.findById.and.returnValues(
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup)),
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup))
      );

      bitstreamService.getUploadedFileList.and.returnValue(cold('-a-b', {
        a: [],
        b: mockUploadFiles
      }));

      comp.onSectionInit();

      expect(comp.required$.getValue()).toBe(true);

      expect(compAsAny.getSectionStatus()).toBeObservable(cold('-c-d', {
        c: false,
        d: true
      }));
    });

    it('should properly read the section status when required is false', () => {
      submissionServiceStub.getSubmissionObject.and.returnValue(observableOf(submissionState));

      collectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));

      resourcePolicyService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(mockDefaultAccessCondition));

      uploadsConfigService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(Object.assign(new SubmissionUploadsModel(), mockUploadConfigResponseNotRequired)));

      groupService.findById.and.returnValues(
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup)),
        createSuccessfulRemoteDataObject$(Object.assign(new Group(), mockGroup))
      );

      bitstreamService.getUploadedFileList.and.returnValue(cold('-a-b', {
        a: [],
        b: mockUploadFiles
      }));

      comp.onSectionInit();

      expect(comp.required$.getValue()).toBe(false);

      expect(compAsAny.getSectionStatus()).toBeObservable(cold('-c-d', {
        c: true,
        d: true
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
