import { ChangeDetectorRef, Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, inject, TestBed, waitForAsync } from '@angular/core/testing';

import { of as observableOf } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { DynamicCheckboxModel, DynamicFormControlEvent, DynamicFormControlEventType } from '@ng-dynamic-forms/core';

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
import {
  mockLicenseParsedErrors,
  mockSubmissionCollectionId,
  mockSubmissionId
} from '../../../shared/mocks/submission.mock';
import { FormComponent } from '../../../shared/form/form.component';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { SubmissionSectionLicenseComponent } from './section-license.component';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { SectionFormOperationsService } from '../form/section-form-operations.service';
import { Collection } from '../../../core/shared/collection.model';
import { License } from '../../../core/shared/license.model';
import { FormFieldMetadataValueObject } from '../../../shared/form/builder/models/form-field-metadata-value.model';
import { cold } from 'jasmine-marbles';

const collectionId = mockSubmissionCollectionId;
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

function getMockSubmissionFormsConfigService(): SubmissionFormsConfigDataService {
  return jasmine.createSpyObj('FormOperationsService', {
    getConfigAll: jasmine.createSpy('getConfigAll'),
    getConfigByHref: jasmine.createSpy('getConfigByHref'),
    getConfigByName: jasmine.createSpy('getConfigByName'),
    getConfigBySearch: jasmine.createSpy('getConfigBySearch')
  });
}

const sectionObject: SectionDataObject = {
  config: 'https://dspace7.4science.it/or2018/api/config/submissionforms/license',
  mandatory: true,
  data: {
    url: null,
    acceptanceDate: null,
    granted: false
  },
  errorsToShow: [],
  serverValidationErrors: [],
  header: 'submit.progressbar.describe.license',
  id: 'license',
  sectionType: SectionsType.License
};

const dynamicFormControlEvent: DynamicFormControlEvent = {
  $event: new Event('change'),
  context: null,
  control: null,
  group: null,
  model: null,
  type: DynamicFormControlEventType.Change
};

describe('SubmissionSectionLicenseComponent test suite', () => {

  let comp: SubmissionSectionLicenseComponent;
  let compAsAny: any;
  let fixture: ComponentFixture<SubmissionSectionLicenseComponent>;
  let submissionServiceStub: SubmissionServiceStub;
  let formService: any;
  let formOperationsService: any;
  let formBuilderService: any;

  const sectionsServiceStub: any = new SectionsServiceStub();
  const submissionId = mockSubmissionId;

  const pathCombiner = new JsonPatchOperationPathCombiner('sections', sectionObject.id);
  const jsonPatchOpBuilder: any = jasmine.createSpyObj('jsonPatchOpBuilder', {
    add: jasmine.createSpy('add'),
    replace: jasmine.createSpy('replace'),
    remove: jasmine.createSpy('remove'),
  });

  const mockCollectionDataService = jasmine.createSpyObj('CollectionDataService', {
    findById: jasmine.createSpy('findById'),
    findByHref: jasmine.createSpy('findByHref')
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        FormComponent,
        SubmissionSectionLicenseComponent,
        TestComponent
      ],
      providers: [
        { provide: CollectionDataService, useValue: mockCollectionDataService },
        { provide: SectionFormOperationsService, useValue: getMockFormOperationsService() },
        { provide: FormService, useValue: getMockFormService() },
        { provide: JsonPatchOperationsBuilder, useValue: jsonPatchOpBuilder },
        { provide: SubmissionFormsConfigDataService, useValue: getMockSubmissionFormsConfigService() },
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: SectionsService, useValue: sectionsServiceStub },
        { provide: SubmissionService, useClass: SubmissionServiceStub },
        { provide: 'collectionIdProvider', useValue: collectionId },
        { provide: 'sectionDataProvider', useValue: Object.assign({}, sectionObject) },
        { provide: 'submissionIdProvider', useValue: submissionId },
        ChangeDetectorRef,
        FormBuilderService,
        SubmissionSectionLicenseComponent
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents().then();
  }));

  describe('', () => {
    let testComp: TestComponent;
    let testFixture: ComponentFixture<TestComponent>;

    // synchronous beforeEach
    beforeEach(() => {
      mockCollectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));
      sectionsServiceStub.isSectionReadOnly.and.returnValue(observableOf(false));
      sectionsServiceStub.getSectionErrors.and.returnValue(observableOf([]));

      const html = `
        <ds-submission-section-license></ds-submission-section-license>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });

    it('should create SubmissionSectionLicenseComponent', inject([SubmissionSectionLicenseComponent], (app: SubmissionSectionLicenseComponent) => {

      expect(app).toBeDefined();

    }));
  });

  describe('', () => {
    beforeEach(() => {
      fixture = TestBed.createComponent(SubmissionSectionLicenseComponent);
      comp = fixture.componentInstance;
      compAsAny = comp;
      submissionServiceStub = TestBed.inject(SubmissionService as any);
      formService = TestBed.inject(FormService);
      formBuilderService = TestBed.inject(FormBuilderService);
      formOperationsService = TestBed.inject(SectionFormOperationsService);

      compAsAny.pathCombiner = new JsonPatchOperationPathCombiner('sections', sectionObject.id);

    });

    afterEach(() => {
      fixture.destroy();
      comp = null;
      compAsAny = null;
    });

    describe('', () => {
      beforeEach(() => {
        mockCollectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));
        sectionsServiceStub.getSectionErrors.and.returnValue(observableOf([]));
        sectionsServiceStub.isSectionReadOnly.and.returnValue(observableOf(false));
      });

      it('should init section properly', () => {

        spyOn(compAsAny, 'getSectionStatus');

        comp.onSectionInit();

        const model = formBuilderService.findById('granted', comp.formModel);

        expect(compAsAny.subs.length).toBe(2);
        expect(comp.formModel).toBeDefined();
        expect(model.value).toBeFalsy();
        expect(comp.licenseText$).toBeObservable(cold('(ab|)', {
          a: '',
          b: licenseText
        }));
      });

      it('should set checkbox value to true', () => {
        comp.sectionData.data = {
          url: 'url',
          acceptanceDate: Date.now(),
          granted: true
        } as any;

        spyOn(compAsAny, 'getSectionStatus');

        comp.onSectionInit();

        const model = formBuilderService.findById('granted', comp.formModel);

        expect(compAsAny.subs.length).toBe(2);
        expect(comp.formModel).toBeDefined();
        expect(model.value).toBeTruthy();
        expect(comp.licenseText$).toBeObservable(cold('(ab|)', {
          a: '',
          b: licenseText
        }));
      });

      it('should have status true when checkbox is selected', () => {
        fixture.detectChanges();
        const model = formBuilderService.findById('granted', comp.formModel);

        (model as DynamicCheckboxModel).value = true;

        compAsAny.getSectionStatus().subscribe((status) => {
          expect(status).toBeTruthy();
        });
      });

      it('should have status false when checkbox is not selected', () => {
        fixture.detectChanges();
        const model = formBuilderService.findById('granted', comp.formModel);

        compAsAny.getSectionStatus().subscribe((status) => {
          expect(status).toBeFalsy();
        });

        (model as DynamicCheckboxModel).value = false;
      });

    });

    describe('', () => {
      beforeEach(() => {
        mockCollectionDataService.findById.and.returnValue(createSuccessfulRemoteDataObject$(mockCollection));
        sectionsServiceStub.getSectionErrors.and.returnValue(observableOf(mockLicenseParsedErrors.license));
        sectionsServiceStub.isSectionReadOnly.and.returnValue(observableOf(false));
      });

      it('should set section errors properly', () => {
        comp.onSectionInit();
        const expectedErrors = mockLicenseParsedErrors.license;

        expect(sectionsServiceStub.checkSectionErrors).toHaveBeenCalled();
        expect(comp.sectionData.errors).toEqual(expectedErrors);

      });

      it('should remove any section\'s errors when checkbox is selected', () => {
        comp.sectionData.data = {
          url: 'url',
          acceptanceDate: Date.now(),
          granted: true
        } as any;

        comp.onSectionInit();

        expect(sectionsServiceStub.dispatchRemoveSectionErrors).toHaveBeenCalled();

      });
    });

    describe('', () => {
      let event;
      beforeEach(() => {
        event = dynamicFormControlEvent;
        formOperationsService.getFieldPathSegmentedFromChangeEvent.and.returnValue('granted');
      });

      it('should dispatch a json-path add operation when checkbox is selected', () => {

        formOperationsService.getFieldValueFromChangeEvent.and.returnValue(new FormFieldMetadataValueObject(true));

        comp.onChange(event);

        expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(pathCombiner.getPath('granted'), 'true', false, true);
        expect(sectionsServiceStub.dispatchRemoveSectionErrors).toHaveBeenCalled();
      });

      it('should dispatch a json-path remove operation when checkbox is not selected', () => {

        formOperationsService.getFieldValueFromChangeEvent.and.returnValue(null);

        comp.onChange(event);

        expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('granted'));
      });
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
