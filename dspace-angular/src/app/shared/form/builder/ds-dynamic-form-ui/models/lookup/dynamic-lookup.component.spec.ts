// Load the implementations that should be tested
import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, fakeAsync, inject, TestBed, tick, waitForAsync, } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { of as observableOf } from 'rxjs';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { DynamicFormsNGBootstrapUIModule } from '@ng-dynamic-forms/ui-ng-bootstrap';
import { DynamicFormLayoutService, DynamicFormsCoreModule, DynamicFormValidationService } from '@ng-dynamic-forms/core';

import { VocabularyOptions } from '../../../../../../core/submission/vocabularies/models/vocabulary-options.model';
import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { VocabularyServiceStub } from '../../../../../testing/vocabulary-service.stub';
import { DsDynamicLookupComponent } from './dynamic-lookup.component';
import { DynamicLookupModel, DynamicLookupModelConfig } from './dynamic-lookup.model';
import { FormFieldMetadataValueObject } from '../../../models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { createTestComponent } from '../../../../../testing/utils.test';
import { DynamicLookupNameModel } from './dynamic-lookup-name.model';
import { AuthorityConfidenceStateDirective } from '../../../../directives/authority-confidence-state.directive';
import { ObjNgFor } from '../../../../../utils/object-ngfor.pipe';
import {
  mockDynamicFormLayoutService,
  mockDynamicFormValidationService
} from '../../../../../testing/dynamic-form-mock-services';

let LOOKUP_TEST_MODEL_CONFIG: DynamicLookupModelConfig = {
  vocabularyOptions: {
    name: 'RPAuthority',
    closed: false
  } as VocabularyOptions,
  disabled: false,
  errorMessages: { required: 'Required field.' },
  id: 'lookup',
  label: 'Author',
  maxOptions: 10,
  name: 'lookup',
  placeholder: 'Author',
  readOnly: false,
  required: true,
  repeatable: true,
  validators: { required: null },
  value: undefined,
  metadataFields: [],
  submissionId: '1234',
  hasSelectableMetadata: false
};

let LOOKUP_NAME_TEST_MODEL_CONFIG = {
  vocabularyOptions: {
    name: 'RPAuthority',
    closed: false
  } as VocabularyOptions,
  disabled: false,
  errorMessages: { required: 'Required field.' },
  id: 'lookupName',
  label: 'Author',
  maxOptions: 10,
  name: 'lookupName',
  placeholder: 'Author',
  readOnly: false,
  required: true,
  repeatable: true,
  validators: { required: null },
  value: undefined,
  metadataFields: [],
  submissionId: '1234',
  hasSelectableMetadata: false
};

let LOOKUP_TEST_GROUP = new FormGroup({
  lookup: new FormControl(),
  lookupName: new FormControl()
});

describe('Dynamic Lookup component', () => {
  function init() {
    LOOKUP_TEST_MODEL_CONFIG = {
      vocabularyOptions: {
        name: 'RPAuthority',
        closed: false
      } as VocabularyOptions,
      disabled: false,
      errorMessages: { required: 'Required field.' },
      id: 'lookup',
      label: 'Author',
      maxOptions: 10,
      name: 'lookup',
      placeholder: 'Author',
      readOnly: false,
      required: true,
      repeatable: true,
      validators: { required: null },
      value: undefined,
      metadataFields: [],
      submissionId: '1234',
      hasSelectableMetadata: false
    };

    LOOKUP_NAME_TEST_MODEL_CONFIG = {
      vocabularyOptions: {
        name: 'RPAuthority',
        closed: false
      } as VocabularyOptions,
      disabled: false,
      errorMessages: { required: 'Required field.' },
      id: 'lookupName',
      label: 'Author',
      maxOptions: 10,
      name: 'lookupName',
      placeholder: 'Author',
      readOnly: false,
      required: true,
      repeatable: true,
      validators: { required: null },
      value: undefined,
      metadataFields: [],
      submissionId: '1234',
      hasSelectableMetadata: false
    };

    LOOKUP_TEST_GROUP = new FormGroup({
      lookup: new FormControl(),
      lookupName: new FormControl()
    });

  }

  let testComp: TestComponent;
  let lookupComp: DsDynamicLookupComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let lookupFixture: ComponentFixture<DsDynamicLookupComponent>;
  let html;
  let vocabularyServiceStub: VocabularyServiceStub;

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    vocabularyServiceStub = new VocabularyServiceStub();
    TestBed.configureTestingModule({
      imports: [
        DynamicFormsCoreModule,
        DynamicFormsNGBootstrapUIModule,
        FormsModule,
        InfiniteScrollModule,
        ReactiveFormsModule,
        NgbModule,
        TranslateModule.forRoot()
      ],
      declarations: [
        DsDynamicLookupComponent,
        TestComponent,
        AuthorityConfidenceStateDirective,
        ObjNgFor
      ], // declare the test component
      providers: [
        ChangeDetectorRef,
        DsDynamicLookupComponent,
        { provide: VocabularyService, useValue: vocabularyServiceStub },
        { provide: DynamicFormLayoutService, useValue: mockDynamicFormLayoutService },
        { provide: DynamicFormValidationService, useValue: mockDynamicFormValidationService }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });
  }));

  beforeEach(() => {
    init();
  });

  describe('DynamicLookUpComponent', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
      <ds-dynamic-lookup
        [bindId]="bindId"
        [group]="group"
        [model]="model"
        [showErrorMessages]="showErrorMessages"
        (blur)="onBlur($event)"
        (change)="onValueChange($event)"
        (focus)="onFocus($event)"></ds-dynamic-lookup>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });
    afterEach(() => {
      testFixture.destroy();
      testComp = null;
    });
    it('should create DsDynamicLookupComponent', inject([DsDynamicLookupComponent], (app: DsDynamicLookupComponent) => {
      expect(app).toBeDefined();
    }));

    describe('when model is DynamicLookupModel', () => {

      describe('', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupModel(LOOKUP_TEST_MODEL_CONFIG);
          lookupFixture.detectChanges();
        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should render only an input element', () => {
          const de = lookupFixture.debugElement.queryAll(By.css('input.form-control'));
          expect(de.length).toBe(1);
        });

      });

      describe('and init model value is empty', () => {
        beforeEach(() => {
          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupModel(LOOKUP_TEST_MODEL_CONFIG);
          lookupFixture.detectChanges();
        });

        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });

        it('should init component properly', () => {
          expect(lookupComp.firstInputValue).toBe('');
          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = de[0].nativeElement;
          const editBtnEl = de[1].nativeElement;
          expect(searchBtnEl.disabled).toBe(true);
          expect(editBtnEl.disabled).toBe(true);
          expect(editBtnEl.textContent.trim()).toBe('form.edit');
        });

        it('should return search results', fakeAsync(() => {
          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const btnEl = de[0].nativeElement;
          const results = vocabularyServiceStub.getList();

          lookupComp.firstInputValue = 'test';
          lookupFixture.detectChanges();

          btnEl.click();
          tick();
          lookupFixture.detectChanges();
          expect(lookupComp.optionsList).toEqual(results);

        }));

        it('should select a results entry properly', fakeAsync(() => {
          let de = lookupFixture.debugElement.queryAll(By.css('button'));
          const btnEl = de[0].nativeElement;
          const selectedValue = Object.assign(new VocabularyEntry(), {
            authority: 1,
            display: 'one',
            value: 1
          });
          spyOn(lookupComp.change, 'emit');
          lookupComp.firstInputValue = 'test';
          lookupFixture.detectChanges();
          btnEl.click();
          tick();
          lookupFixture.detectChanges();
          de = lookupFixture.debugElement.queryAll(By.css('button.dropdown-item'));
          const entryEl = de[0].nativeElement;
          entryEl.click();
          lookupFixture.detectChanges();
          expect(lookupComp.firstInputValue).toEqual('one');
          expect(lookupComp.model.value).toEqual(selectedValue);
          expect(lookupComp.change.emit).toHaveBeenCalled();
        }));

        it('should set model.value on input type when VocabularyOptions.closed is false', fakeAsync(() => {
          lookupComp.firstInputValue = 'test';
          lookupFixture.detectChanges();

          lookupComp.onChange(new Event('change'));
          expect(lookupComp.model.value).toEqual(new FormFieldMetadataValueObject('test'));

        }));

        it('should not set model.value on input type when VocabularyOptions.closed is true', () => {
          lookupComp.model.vocabularyOptions.closed = true;
          lookupComp.firstInputValue = 'test';
          lookupFixture.detectChanges();

          lookupComp.onChange(new Event('change'));
          expect(lookupComp.model.value).not.toBeDefined();

        });

      });

      describe('and init model value is not empty', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupModel(LOOKUP_TEST_MODEL_CONFIG);
          const entry = observableOf(Object.assign(new VocabularyEntry(), {
            authority: null,
            value: 'test',
            display: 'testDisplay'
          }));
          spyOn((lookupComp as any).vocabularyService, 'getVocabularyEntryByValue').and.returnValue(entry);
          (lookupComp.model as any).value = new FormFieldMetadataValueObject('test', null, null, 'testDisplay');
          lookupFixture.detectChanges();

          // spyOn(store, 'dispatch');
        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should init component properly', fakeAsync(() => {
          tick();
          expect(lookupComp.firstInputValue).toBe('testDisplay');
          expect((lookupComp as any).vocabularyService.getVocabularyEntryByValue).toHaveBeenCalled();
        }));

        it('should have search button disabled on edit mode', () => {
          lookupComp.editMode = true;
          lookupFixture.detectChanges();

          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = de[0].nativeElement;
          const saveBtnEl = de[1].nativeElement;
          expect(searchBtnEl.disabled).toBe(true);
          expect(saveBtnEl.disabled).toBe(false);
          expect(saveBtnEl.textContent.trim()).toBe('form.save');

        });
      });
      describe('and init model value is not empty with authority', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupModel(LOOKUP_TEST_MODEL_CONFIG);
          const entry = observableOf(Object.assign(new VocabularyEntry(), {
            authority: 'test001',
            value: 'test',
            display: 'testDisplay'
          }));
          spyOn((lookupComp as any).vocabularyService, 'getVocabularyEntryByID').and.returnValue(entry);
          lookupComp.model.value = new FormFieldMetadataValueObject('test', null, 'test001', 'testDisplay');
          lookupFixture.detectChanges();

          // spyOn(store, 'dispatch');
        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should init component properly', fakeAsync(() => {
          tick();
          expect(lookupComp.firstInputValue).toBe('testDisplay');
          expect((lookupComp as any).vocabularyService.getVocabularyEntryByID).toHaveBeenCalled();
        }));

        it('should have search button disabled on edit mode', () => {
          lookupComp.editMode = true;
          lookupFixture.detectChanges();

          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = de[0].nativeElement;
          const saveBtnEl = de[1].nativeElement;
          expect(searchBtnEl.disabled).toBe(true);
          expect(saveBtnEl.disabled).toBe(false);
          expect(saveBtnEl.textContent.trim()).toBe('form.save');

        });
      });
    });

    describe('when model is DynamicLookupNameModel', () => {

      describe('', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupNameModel(LOOKUP_NAME_TEST_MODEL_CONFIG);
          lookupFixture.detectChanges();

          // spyOn(store, 'dispatch');
        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should render two input element', () => {
          const de = lookupFixture.debugElement.queryAll(By.css('input.form-control'));
          const deBtn = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = deBtn[0].nativeElement;
          const editBtnEl = deBtn[1].nativeElement;

          expect(de.length).toBe(2);
          expect(searchBtnEl.disabled).toBe(true);
          expect(editBtnEl.disabled).toBe(true);
          expect(editBtnEl.textContent.trim()).toBe('form.edit');
        });

      });

      describe('and init model value is empty', () => {

        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupNameModel(LOOKUP_NAME_TEST_MODEL_CONFIG);
          lookupFixture.detectChanges();
        });

        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });

        it('should select a results entry properly', fakeAsync(() => {
          const payload = [
            Object.assign(new VocabularyEntry(), {
              authority: 1,
              display: 'Name, Lastname',
              value: 1
            }),
            Object.assign(new VocabularyEntry(), {
              authority: 2,
              display: 'NameTwo, LastnameTwo',
              value: 2
            }),
          ];
          let de = lookupFixture.debugElement.queryAll(By.css('button'));
          const btnEl = de[0].nativeElement;
          const selectedValue = Object.assign(new VocabularyEntry(), {
            authority: 1,
            display: 'Name, Lastname',
            value: 1
          });
          spyOn(lookupComp.change, 'emit');
          vocabularyServiceStub.setNewPayload(payload);
          lookupComp.firstInputValue = 'test';
          lookupFixture.detectChanges();
          btnEl.click();
          tick();
          lookupFixture.detectChanges();
          de = lookupFixture.debugElement.queryAll(By.css('button.dropdown-item'));
          const entryEl = de[0].nativeElement;
          entryEl.click();

          expect(lookupComp.firstInputValue).toEqual('Name');
          expect(lookupComp.secondInputValue).toEqual('Lastname');
          expect(lookupComp.model.value).toEqual(selectedValue);
          expect(lookupComp.change.emit).toHaveBeenCalled();
        }));
      });

      describe('and init model value is not empty', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupNameModel(LOOKUP_NAME_TEST_MODEL_CONFIG);
          lookupComp.model.value = new FormFieldMetadataValueObject('Name, Lastname', null, 'test001');
          const entry = observableOf(Object.assign(new VocabularyEntry(), {
            authority: null,
            value: 'Name, Lastname',
            display: 'Name, Lastname'
          }));
          spyOn((lookupComp as any).vocabularyService, 'getVocabularyEntryByValue').and.returnValue(entry);
          (lookupComp.model as any).value = new FormFieldMetadataValueObject('Name, Lastname', null, null, 'Name, Lastname');
          lookupFixture.detectChanges();

        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should init component properly', fakeAsync(() => {
          tick();
          expect(lookupComp.firstInputValue).toBe('Name');
          expect(lookupComp.secondInputValue).toBe('Lastname');
          expect((lookupComp as any).vocabularyService.getVocabularyEntryByValue).toHaveBeenCalled();
        }));

        it('should have search button disabled on edit mode', () => {
          lookupComp.editMode = true;
          lookupFixture.detectChanges();

          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = de[0].nativeElement;
          const saveBtnEl = de[1].nativeElement;
          expect(searchBtnEl.disabled).toBe(true);
          expect(saveBtnEl.disabled).toBe(false);
          expect(saveBtnEl.textContent.trim()).toBe('form.save');

        });
      });

      describe('and init model value is not empty with authority', () => {
        beforeEach(() => {

          lookupFixture = TestBed.createComponent(DsDynamicLookupComponent);
          lookupComp = lookupFixture.componentInstance; // FormComponent test instance
          lookupComp.group = LOOKUP_TEST_GROUP;
          lookupComp.model = new DynamicLookupNameModel(LOOKUP_NAME_TEST_MODEL_CONFIG);
          lookupComp.model.value = new FormFieldMetadataValueObject('Name, Lastname', null, 'test001');
          const entry = observableOf(Object.assign(new VocabularyEntry(), {
            authority: 'test001',
            value: 'Name, Lastname',
            display: 'Name, Lastname'
          }));
          spyOn((lookupComp as any).vocabularyService, 'getVocabularyEntryByID').and.returnValue(entry);
          lookupComp.model.value = new FormFieldMetadataValueObject('Name, Lastname', null, 'test001', 'Name, Lastname');
          lookupFixture.detectChanges();

        });
        afterEach(() => {
          lookupFixture.destroy();
          lookupComp = null;
        });
        it('should init component properly', fakeAsync(() => {
          tick();
          expect(lookupComp.firstInputValue).toBe('Name');
          expect(lookupComp.secondInputValue).toBe('Lastname');
          expect((lookupComp as any).vocabularyService.getVocabularyEntryByID).toHaveBeenCalled();
        }));

        it('should have search button disabled on edit mode', () => {
          lookupComp.editMode = true;
          lookupFixture.detectChanges();

          const de = lookupFixture.debugElement.queryAll(By.css('button'));
          const searchBtnEl = de[0].nativeElement;
          const saveBtnEl = de[1].nativeElement;
          expect(searchBtnEl.disabled).toBe(true);
          expect(saveBtnEl.disabled).toBe(false);
          expect(saveBtnEl.textContent.trim()).toBe('form.save');

        });
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

  group: FormGroup = LOOKUP_TEST_GROUP;

  inputLookupModelConfig = LOOKUP_TEST_MODEL_CONFIG;

  model = new DynamicLookupModel(this.inputLookupModelConfig);
}
