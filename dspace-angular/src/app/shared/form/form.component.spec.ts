import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync, } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import {
  DynamicFormArrayModel,
  DynamicFormControlEvent,
  DynamicFormControlModel,
  DynamicFormValidationService,
  DynamicInputModel
} from '@ng-dynamic-forms/core';
import { Store, StoreModule } from '@ngrx/store';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';

import { FormComponent } from './form.component';
import { FormService } from './form.service';
import { FormBuilderService } from './builder/form-builder.service';
import { FormState } from './form.reducer';
import { FormChangeAction, FormStatusChangeAction } from './form.actions';
import { StoreMock } from '../testing/store.mock';
import { FormFieldMetadataValueObject } from './builder/models/form-field-metadata-value.model';
import { createTestComponent } from '../testing/utils.test';
import { BehaviorSubject } from 'rxjs';
import { storeModuleConfig } from '../../app.reducer';

let TEST_FORM_MODEL;

let TEST_FORM_MODEL_WITH_ARRAY;

let config;
let formState: FormState;
let html;
let store: StoreMock<FormState>;

function init() {
  TEST_FORM_MODEL = [
    new DynamicInputModel(
      {
        id: 'dc_title',
        label: 'Title',
        placeholder: 'Title',
        validators: {
          required: null
        },
        errorMessages: {
          required: 'You must enter a main title for this item.'
        }
      }
    ),

    new DynamicInputModel(
      {
        id: 'dc_title_alternative',
        label: 'Other Titles',
        placeholder: 'Other Titles',
      }
    ),

    new DynamicInputModel(
      {
        id: 'dc_publisher',
        label: 'Publisher',
        placeholder: 'Publisher',
      }
    ),

    new DynamicInputModel(
      {
        id: 'dc_identifier_citation',
        label: 'Citation',
        placeholder: 'Citation',
      }
    ),

    new DynamicInputModel(
      {
        id: 'dc_identifier_issn',
        label: 'Identifiers',
        placeholder: 'Identifiers',
      }
    ),
  ];

  TEST_FORM_MODEL_WITH_ARRAY = [
    new DynamicFormArrayModel({

      id: 'bootstrapFormArray',
      initialCount: 1,
      label: 'Form Array',
      groupFactory: () => {
        return [
          new DynamicInputModel({
            id: 'bootstrapArrayGroupInput',
            placeholder: 'example array group input',
            readOnly: false
          })
        ];
      }
    })
  ];
  config = {
    form: {
      validatorMap: {
        required: 'required',
        regex: 'pattern'
      }
    }
  } as any;

  formState = {
    testForm: {
      data: {
        dc_title: null,
        dc_title_alternative: null,
        dc_publisher: null,
        dc_identifier_citation: null,
        dc_identifier_issn: null
      },
      valid: false,
      errors: [],
      touched: {}
    }
  };

}

describe('FormComponent test suite', () => {
  let testComp: TestComponent;
  let formComp: FormComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let formFixture: ComponentFixture<FormComponent>;

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    init();
    /* TODO make sure these files use mocks instead of real services/components https://github.com/DSpace/dspace-angular/issues/281 */
    TestBed.configureTestingModule({
      imports: [
        BrowserModule,
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        NgbModule,
        StoreModule.forRoot({}, storeModuleConfig),
        TranslateModule.forRoot()
      ],
      declarations: [
        FormComponent,
        TestComponent,
      ], // declare the test component
      providers: [
        ChangeDetectorRef,
        DynamicFormValidationService,
        FormBuilderService,
        FormComponent,
        FormService,
        { provide: Store, useClass: StoreMock }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

  }));

  describe('', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
        <ds-form *ngIf="formModel" #formRef="formComponent"
                 [formId]="formId"
                 [formModel]="formModel"
                 [displaySubmit]="displaySubmit"
                 [displayCancel]="displayCancel"></ds-form>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;

    });
    afterEach(() => {
      testFixture.destroy();
      testComp = null;
      html = undefined;
    });
    it('should create FormComponent', inject([FormComponent], (app: FormComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('', () => {
    let form;
    let valid;
    beforeEach(() => {

      formFixture = TestBed.createComponent(FormComponent);
      store = TestBed.inject(Store as any);
      formComp = formFixture.componentInstance; // FormComponent test instance
      formComp.formId = 'testForm';
      formComp.formModel = TEST_FORM_MODEL;
      formComp.displaySubmit = false;
      formComp.displayCancel = false;
      form = new BehaviorSubject(formState);
      valid = new BehaviorSubject(false);
      spyOn((formComp as any).formService, 'getForm').and.returnValue(form);
      spyOn((formComp as any).formService, 'isValid').and.returnValue(valid);
      formFixture.detectChanges();
      spyOn(store, 'dispatch');
    });

    afterEach(() => {
      formFixture.destroy();
      formComp = null;
    });

    it('should dispatch a FormStatusChangeAction when Form group status changes', () => {
      const control = formComp.formGroup.get(['dc_title']);
      control.setValue('Test Title');
      expect(store.dispatch).toHaveBeenCalledWith(new FormStatusChangeAction('testForm', formComp.formGroup.valid));
    });

    it('should display form errors when errors are added to the state', () => {
      const errors = [{
        fieldId: 'dc_title',
        fieldIndex: 0,
        message: 'error.validation.required'
      }];
      formState.testForm.errors = errors;
      form.next(formState.testForm);
      formFixture.detectChanges();

      expect((formComp as any).formErrors).toEqual(errors);

    });

    it('should remove form errors when errors are empty in the state', () => {
      (formComp as any).formErrors = [{
        fieldId: 'dc_title',
        message: 'error.validation.required'
      }];
      const errors = [];

      formState.testForm.errors = errors;
      form.next(formState.testForm);
      formFixture.detectChanges();

      expect((formComp as any).formErrors).toEqual(errors);

    });

    it('should dispatch FormChangeAction on form change', inject([FormBuilderService], (service: FormBuilderService) => {
      const event = {
        $event: new FormFieldMetadataValueObject('Test Title'),
        context: null,
        control: formComp.formGroup.get('dc_title'),
        group: formComp.formGroup,
        model: formComp.formModel[0],
        type: 'change'
      } as DynamicFormControlEvent;

      spyOn(formComp.change, 'emit');

      formComp.onChange(event);

      expect(store.dispatch).toHaveBeenCalledWith(new FormChangeAction('testForm', service.getValueFromModel(formComp.formModel)));
      expect(formComp.change.emit).toHaveBeenCalled();
    }));

    it('should emit change on form change', inject([FormBuilderService], (service: FormBuilderService) => {
      const event = {
        $event: new FormFieldMetadataValueObject('Test Title'),
        context: null,
        control: formComp.formGroup.get('dc_title'),
        group: formComp.formGroup,
        model: formComp.formModel[0],
        type: 'change'
      } as DynamicFormControlEvent;

      spyOn(formComp.change, 'emit');

      formComp.onChange(event);

      expect(formComp.change.emit).toHaveBeenCalled();
    }));

    it('should not emit change Event on form change when emitChange is false', inject([FormBuilderService], (service: FormBuilderService) => {
      const event = {
        $event: new FormFieldMetadataValueObject('Test Title'),
        context: null,
        control: formComp.formGroup.get('dc_title'),
        group: formComp.formGroup,
        model: formComp.formModel[0],
        type: 'change'
      } as DynamicFormControlEvent;

      formComp.emitChange = false;
      spyOn(formComp.change, 'emit');

      formComp.onChange(event);

      expect(formComp.change.emit).not.toHaveBeenCalled();
    }));

    it('should emit blur Event on blur', () => {
      const event = {
        $event: new FocusEvent('blur'),
        context: null,
        control: formComp.formGroup.get('dc_title'),
        group: formComp.formGroup,
        model: formComp.formModel[0],
        type: 'blur'
      } as DynamicFormControlEvent;

      spyOn(formComp.blur, 'emit');

      formComp.onBlur(event);

      expect(formComp.blur.emit).toHaveBeenCalled();
    });

    it('should emit focus Event on focus', () => {
      const event = {
        $event: new FocusEvent('focus'),
        context: null,
        control: formComp.formGroup.get('dc_title'),
        group: formComp.formGroup,
        model: formComp.formModel[0],
        type: 'focus'
      } as DynamicFormControlEvent;

      spyOn(formComp.focus, 'emit');

      formComp.onFocus(event);

      expect(formComp.focus.emit).toHaveBeenCalled();
    });

    it('should return Observable of form status', () => {

      const control = formComp.formGroup.get(['dc_title']);
      control.setValue('Test Title');
      valid.next(true);
      formFixture.detectChanges();

      formComp.isValid().subscribe((v) => {
        expect(v).toBe(true);
      });
    });

    it('should emit submit Event on form submit whether the form is valid', () => {

      const control = formComp.formGroup.get(['dc_title']);
      control.setValue('Test Title');
      formState.testForm.valid = true;
      spyOn(formComp.submitForm, 'emit');

      form.next(formState.testForm);
      formFixture.detectChanges();

      formComp.onSubmit();
      expect(formComp.submitForm.emit).toHaveBeenCalled();
    });

    it('should not emit submit Event on form submit whether the form is not valid', () => {

      spyOn((formComp as any).formService, 'validateAllFormFields');

      form.next(formState.testForm);
      formFixture.detectChanges();

      formComp.onSubmit();
      expect((formComp as any).formService.validateAllFormFields).toHaveBeenCalled();
    });

    it('should reset form group', () => {

      spyOn(formComp.formGroup, 'reset');

      formComp.reset();

      expect(formComp.formGroup.reset).toHaveBeenCalled();
    });
  });

  describe('', () => {
    init();
    beforeEach(() => {
      formFixture = TestBed.createComponent(FormComponent);
      store = TestBed.inject(Store as any);
      formComp = formFixture.componentInstance; // FormComponent test instance
      formComp.formId = 'testFormArray';
      formComp.formModel = TEST_FORM_MODEL_WITH_ARRAY;
      formComp.displaySubmit = false;
      formComp.displayCancel = false;
      formFixture.detectChanges();
      spyOn(store, 'dispatch');
    });

    afterEach(() => {
      formFixture.destroy();
      formComp = null;
    });

    it('should return ReadOnly property from array item', inject([FormBuilderService], (service: FormBuilderService) => {
      const readOnly = formComp.isItemReadOnly(formComp.formModel[0] as DynamicFormArrayModel, 0);

      expect(readOnly).toBe(false);
    }));

    it('should dispatch FormChangeAction when an item has been added to an array', inject([FormBuilderService], (service: FormBuilderService) => {
      formComp.insertItem(new Event('click'), formComp.formModel[0] as DynamicFormArrayModel, 0);

      expect(store.dispatch).toHaveBeenCalledWith(new FormChangeAction('testFormArray', service.getValueFromModel(formComp.formModel)));
    }));

    it('should emit addArrayItem Event when an item has been added to an array', inject([FormBuilderService], (service: FormBuilderService) => {
      spyOn(formComp.addArrayItem, 'emit');

      formComp.insertItem(new Event('click'), formComp.formModel[0] as DynamicFormArrayModel, 0);

      expect(formComp.addArrayItem.emit).toHaveBeenCalled();
    }));

    it('should dispatch FormChangeAction when an item has been removed from an array', inject([FormBuilderService], (service: FormBuilderService) => {
      formComp.removeItem(new Event('click'), formComp.formModel[0] as DynamicFormArrayModel, 0);

      expect(store.dispatch).toHaveBeenCalledWith(new FormChangeAction('testFormArray', service.getValueFromModel(formComp.formModel)));
    }));

    it('should emit removeArrayItem Event when an item has been removed from an array', inject([FormBuilderService], (service: FormBuilderService) => {
      spyOn(formComp.removeArrayItem, 'emit');

      formComp.removeItem(new Event('click'), formComp.formModel[0] as DynamicFormArrayModel, 0);

      expect(formComp.removeArrayItem.emit).toHaveBeenCalled();
    }));
  });
});

// declare a test component
@Component({
  selector: 'ds-test-cmp',
  template: ``
})
class TestComponent {

  public formId;
  public formModel: DynamicFormControlModel[];
  public displaySubmit = false;
  public displayCancel = false;

  constructor() {
    this.formId = 'testForm';
    this.formModel = TEST_FORM_MODEL;
  }

}
