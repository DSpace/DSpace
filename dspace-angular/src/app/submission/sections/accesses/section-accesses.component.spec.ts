import { FormService } from '../../../shared/form/form.service';
import { ComponentFixture, inject, TestBed } from '@angular/core/testing';

import { SubmissionSectionAccessesComponent } from './section-accesses.component';
import { SectionsService } from '../sections.service';
import { SectionsServiceStub } from '../../../shared/testing/sections-service.stub';

import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { getMockFormBuilderService } from '../../../shared/mocks/form-builder-service.mock';
import { SubmissionAccessesConfigDataService } from '../../../core/config/submission-accesses-config-data.service';
import {
  getSubmissionAccessesConfigNotChangeDiscoverableService,
  getSubmissionAccessesConfigService
} from '../../../shared/mocks/section-accesses-config.service.mock';
import { SectionAccessesService } from './section-accesses.service';
import { SectionFormOperationsService } from '../form/section-form-operations.service';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import {
  SubmissionJsonPatchOperationsService
} from '../../../core/submission/submission-json-patch-operations.service';
import { getSectionAccessesService } from '../../../shared/mocks/section-accesses.service.mock';
import { getMockFormOperationsService } from '../../../shared/mocks/form-operations-service.mock';
import { getMockTranslateService } from '../../../shared/mocks/translate.service.mock';
import {
  SubmissionJsonPatchOperationsServiceStub
} from '../../../shared/testing/submission-json-patch-operations-service.stub';
import { BrowserModule } from '@angular/platform-browser';

import { of as observableOf } from 'rxjs';
import { Store } from '@ngrx/store';
import { FormComponent } from '../../../shared/form/form.component';
import {
  DynamicCheckboxModel,
  DynamicDatePickerModel,
  DynamicFormArrayModel,
  DynamicSelectModel
} from '@ng-dynamic-forms/core';
import { AppState } from '../../../app.reducer';
import { getMockFormService } from '../../../shared/mocks/form-service.mock';
import { mockAccessesFormData } from '../../../shared/mocks/submission.mock';
import { accessConditionChangeEvent, checkboxChangeEvent } from '../../../shared/testing/form-event.stub';

describe('SubmissionSectionAccessesComponent', () => {
  let component: SubmissionSectionAccessesComponent;
  let fixture: ComponentFixture<SubmissionSectionAccessesComponent>;

  const sectionsServiceStub = new SectionsServiceStub();
  const builderService: FormBuilderService = getMockFormBuilderService();
  const submissionAccessesConfigService = getSubmissionAccessesConfigService();
  const sectionAccessesService = getSectionAccessesService();
  const sectionFormOperationsService = getMockFormOperationsService();
  const operationsBuilder = jasmine.createSpyObj('operationsBuilder', {
    add: undefined,
    remove: undefined,
    replace: undefined,
  });

  let formService: any;
  let formbuilderService: any;

  const storeStub = jasmine.createSpyObj('store', ['dispatch']);

  const sectionData = {
    header: 'submit.progressbar.accessCondition',
    config: 'http://localhost:8080/server/api/config/submissionaccessoptions/AccessConditionDefaultConfiguration',
    mandatory: true,
    sectionType: 'accessCondition',
    collapsed: false,
    enabled: true,
    data: {
      discoverable: true,
      accessConditions: []
    },
    errorsToShow: [],
    serverValidationErrors: [],
    isLoading: false,
    isValid: true
  };

  describe('First with canChangeDiscoverable true', () => {

    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          BrowserModule,
          TranslateModule.forRoot()
        ],
        declarations: [SubmissionSectionAccessesComponent, FormComponent],
        providers: [
          { provide: SectionsService, useValue: sectionsServiceStub },
          { provide: SubmissionAccessesConfigDataService, useValue: submissionAccessesConfigService },
          { provide: SectionAccessesService, useValue: sectionAccessesService },
          { provide: SectionFormOperationsService, useValue: sectionFormOperationsService },
          { provide: JsonPatchOperationsBuilder, useValue: operationsBuilder },
          { provide: TranslateService, useValue: getMockTranslateService() },
          { provide: FormService, useValue: getMockFormService() },
          { provide: Store, useValue: storeStub },
          { provide: SubmissionJsonPatchOperationsService, useValue: SubmissionJsonPatchOperationsServiceStub },
          { provide: 'sectionDataProvider', useValue: sectionData },
          { provide: 'submissionIdProvider', useValue: '1508' },
          FormBuilderService
        ]
      })
        .compileComponents();
    });

    beforeEach(inject([Store], (store: Store<AppState>) => {
      fixture = TestBed.createComponent(SubmissionSectionAccessesComponent);
      component = fixture.componentInstance;
      formService = TestBed.inject(FormService);
      formbuilderService = TestBed.inject(FormBuilderService);
      formService.validateAllFormFields.and.callFake(() => null);
      formService.isValid.and.returnValue(observableOf(true));
      formService.getFormData.and.returnValue(observableOf(mockAccessesFormData));
      fixture.detectChanges();
    }));


    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have created formModel', () => {
      expect(component.formModel).toBeTruthy();
    });

    it('should have formModel length should be 2', () => {
      expect(component.formModel.length).toEqual(2);
    });

    it('formModel should have 1 model type checkbox and 1 model type array', () => {
      expect(component.formModel[0] instanceof DynamicCheckboxModel).toBeTrue();
      expect(component.formModel[1] instanceof DynamicFormArrayModel).toBeTrue();
    });

    it('formModel type array should have formgroup with 1 input and 2 datepickers', () => {
      const formModel: any = component.formModel[1];
      const formGroup = formModel.groupFactory()[0].group;

      expect(formGroup[0] instanceof DynamicSelectModel).toBeTrue();
      expect(formGroup[1] instanceof DynamicDatePickerModel).toBeTrue();
      expect(formGroup[2] instanceof DynamicDatePickerModel).toBeTrue();
    });

    it('should have set maxStartDate and maxEndDate properly', () => {
      const maxStartDate = {year: 2024, month: 12, day: 20};
      const maxEndDate = {year: 2022, month: 6, day: 20};

      const startDateModel = formbuilderService.findById('startDate', component.formModel);
      expect(startDateModel.max).toEqual(maxStartDate);
      const endDateModel = formbuilderService.findById('endDate', component.formModel);
      expect(endDateModel.max).toEqual(maxEndDate);
    });

    it('when checkbox changed it should call operationsBuilder replace function', () => {
      component.onChange(checkboxChangeEvent);
      fixture.detectChanges();

      expect(operationsBuilder.replace).toHaveBeenCalled();
    });

    it('when dropdown select changed it should call operationsBuilder add function', () => {
      component.onChange(accessConditionChangeEvent);
      fixture.detectChanges();
      expect(operationsBuilder.add).toHaveBeenCalled();
    });
  });

  describe('when canDescoverable is false', () => {



    beforeEach(async () => {
      await TestBed.configureTestingModule({
        imports: [
          BrowserModule,
          TranslateModule.forRoot()
        ],
        declarations: [SubmissionSectionAccessesComponent, FormComponent],
        providers: [
          { provide: SectionsService, useValue: sectionsServiceStub },
          { provide: FormBuilderService, useValue: builderService },
          { provide: SubmissionAccessesConfigDataService, useValue: getSubmissionAccessesConfigNotChangeDiscoverableService() },
          { provide: SectionAccessesService, useValue: sectionAccessesService },
          { provide: SectionFormOperationsService, useValue: sectionFormOperationsService },
          { provide: JsonPatchOperationsBuilder, useValue: operationsBuilder },
          { provide: TranslateService, useValue: getMockTranslateService() },
          { provide: FormService, useValue: getMockFormService() },
          { provide: Store, useValue: storeStub },
          { provide: SubmissionJsonPatchOperationsService, useValue: SubmissionJsonPatchOperationsServiceStub },
          { provide: 'sectionDataProvider', useValue: sectionData },
          { provide: 'submissionIdProvider', useValue: '1508' },
        ]
      })
        .compileComponents();
    });

    beforeEach(inject([Store], (store: Store<AppState>) => {
      fixture = TestBed.createComponent(SubmissionSectionAccessesComponent);
      component = fixture.componentInstance;
      formService = TestBed.inject(FormService);
      formService.validateAllFormFields.and.callFake(() => null);
      formService.isValid.and.returnValue(observableOf(true));
      formService.getFormData.and.returnValue(observableOf(mockAccessesFormData));
      fixture.detectChanges();
    }));


    it('should have formModel length should be 1', () => {
      expect(component.formModel.length).toEqual(1);
    });

    it('formModel should have only 1 model type array', () => {
      expect(component.formModel[0] instanceof DynamicFormArrayModel).toBeTrue();
    });

  });
});
