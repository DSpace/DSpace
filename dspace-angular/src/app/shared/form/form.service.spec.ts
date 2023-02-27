import { Store, StoreModule } from '@ngrx/store';
import { inject, TestBed, waitForAsync } from '@angular/core/testing';
import { AbstractControl, FormControl, FormGroup, Validators } from '@angular/forms';

import { DynamicFormControlModel, DynamicFormGroupModel, DynamicInputModel } from '@ng-dynamic-forms/core';

import { FormService } from './form.service';
import { FormBuilderService } from './builder/form-builder.service';
import { AppState } from '../../app.reducer';
import { formReducer } from './form.reducer';
import { getMockFormBuilderService } from '../mocks/form-builder-service.mock';

describe('FormService test suite', () => {
  const config = {
    form: {
      validatorMap: {
        required: 'required',
        regex: 'pattern'
      }
    }
  } as any;
  const formId = 'testForm';
  let service: FormService;
  let builderService: FormBuilderService;
  let formGroup: FormGroup;

  const formModel: DynamicFormControlModel[] = [
    new DynamicInputModel({ id: 'author', value: 'test' }),
    new DynamicInputModel({
      id: 'title',
      validators: {
        required: null
      },
      errorMessages: {
        required: 'Title is required'
      }
    }),
    new DynamicInputModel({ id: 'date' }),
    new DynamicInputModel({ id: 'description' }),
    new DynamicFormGroupModel({

      id: 'addressLocation',
      group: [
        new DynamicInputModel({

          id: 'zipCode',
          label: 'Zip Code',
          placeholder: 'ZIP'
        }),
        new DynamicInputModel({

          id: 'state',
          label: 'State',
          placeholder: 'State'
        }),
        new DynamicInputModel({

          id: 'city',
          label: 'City',
          placeholder: 'City'
        })
      ]
    }),
  ];

  let controls;

  const formData = {
    author: ['test'],
    title: null,
    date: null,
    description: null,
    addressLocation: {
      zipCode: null,
      state: null,
      city: null
    }
  };
  const formState = {
    testForm: {
      data: formData,
      valid: false,
      errors: [],
      touched: {}
    }
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        StoreModule.forRoot({ formReducer }, {
          runtimeChecks: {
            strictStateImmutability: false,
            strictActionImmutability: false
          }
        })
      ]
    }).compileComponents();
  }));

  beforeEach(inject([Store], (store: Store<AppState>) => {
      builderService = getMockFormBuilderService();
      store
        .subscribe((state) => {
          state.forms = formState;
        });
      const author: AbstractControl = new FormControl('test');
      const title: AbstractControl = new FormControl(undefined, Validators.required);
      const date: AbstractControl = new FormControl(undefined);
      const description: AbstractControl = new FormControl(undefined);

      const addressLocation: FormGroup = new FormGroup({
        zipCode: new FormControl(undefined),
        state: new FormControl(undefined),
        city: new FormControl(undefined),
      });

      formGroup = new FormGroup({ author, title, date, description, addressLocation });
      controls = { author, title, date, description , addressLocation };
      service = new FormService(builderService, store);
    })
  )
  ;

  it('should check whether form state is init', () => {
    service.isFormInitialized(formId).subscribe((init) => {
      expect(init).toBe(true);
    });
  });

  it('should return form status when isValid is called', () => {
    service.isValid(formId).subscribe((status) => {
      expect(status).toBe(false);
    });
  });

  it('should return form data when getFormData is called', () => {
    service.getFormData(formId).subscribe((data) => {
      expect(data).toBe(formData);
    });
  });

  it('should return form unique id', () => {
    const formId1 = service.getUniqueId(formId);
    const formId2 = service.getUniqueId(formId);

    expect(formId1).not.toEqual(formId2);
  });

  it('should validate all form fields', () => {
    service.validateAllFormFields(formGroup);
    expect(formGroup.controls.author.touched).toBe(true);
    expect(formGroup.controls.author.status).toBe('VALID');

    expect(formGroup.controls.title.touched).toBe(true);
    expect(formGroup.controls.title.status).toBe('INVALID');

    expect(formGroup.controls.date.touched).toBe(true);

    expect(formGroup.controls.description.touched).toBe(true);
  });

  it('should add error to field', () => {
    let control = controls.description;
    let model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'description');
    let errorKeys: string[];

    service.addErrorToField(control, model, 'Test error message');
    errorKeys = Object.keys(control.errors);

    expect(errorKeys.length).toBe(1);

    expect(control.hasError(errorKeys[0])).toBe(true);

    expect(formGroup.controls.description.touched).toBe(true);

    control = controls.title;
    model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'title');
    service.addErrorToField(control, model, 'error.required');
    errorKeys = Object.keys(control.errors);

    expect(errorKeys.length).toBe(1);

    expect(control.hasError(errorKeys[0])).toBe(true);

    expect(formGroup.controls.description.touched).toBe(true);
  });

  it('should add errors to fields of concat group', () => {
    (builderService as any).isConcatGroup.and.returnValue(true);

    let control = controls.addressLocation;
    let model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'addressLocation');
    let errorKeys: string[];

    service.addErrorToField(control, model, 'Test error message');

    // the group itself should get an error
    errorKeys = Object.keys(control.errors);
    expect(errorKeys.length).toBe(1);
    expect(control.hasError(errorKeys[0])).toBe(true);

    expect(control.touched).toBe(true);

    // the group's inputs should get an error
    Object.values(control.controls).forEach((subControl: AbstractControl) => {
      errorKeys = Object.keys(subControl.errors);
      expect(errorKeys.length).toBe(1);
      expect(subControl.hasError(errorKeys[0])).toBe(true);
      expect(subControl.touched).toBe(true);
    });

  });

  it('should remove error from field', () => {
    let control = controls.description;
    let model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'description');
    let errorKeys: string[];

    service.addErrorToField(control, model, 'Test error message');
    errorKeys = Object.keys(control.errors);

    service.removeErrorFromField(control, model, errorKeys[0]);

    expect(errorKeys.length).toBe(1);

    expect(control.hasError(errorKeys[0])).toBe(false);

    expect(formGroup.controls.description.touched).toBe(false);

    control = controls.title;
    model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'title');

    service.addErrorToField(control, model, 'error.required');

    service.removeErrorFromField(control, model, 'error.required');

    expect(errorKeys.length).toBe(1);

    expect(control.hasError(errorKeys[0])).toBe(false);

    expect(formGroup.controls.description.touched).toBe(false);
  });

  it('should remove errors from fields of concat group', () => {
    (builderService as any).isConcatGroup.and.returnValue(true);

    let control = controls.addressLocation;
    let model = formModel.find((mdl: DynamicFormControlModel) => mdl.id === 'addressLocation');
    let errorKeys: string[];

    service.addErrorToField(control, model, 'Test error message');
    errorKeys = Object.keys(control.errors);

    service.removeErrorFromField(control, model, errorKeys[0]);

    // the group itself should no longer have an error
    expect(errorKeys.length).toBe(1);
    expect(control.hasError(errorKeys[0])).toBe(false);
    expect(control.touched).toBe(false);

    // the group's inputs should no longer have an error
    Object.values(control.controls).forEach((subControl: AbstractControl) => {
      errorKeys = Object.keys(subControl.errors);
      expect(errorKeys.length).toBe(1);
      expect(subControl.hasError(errorKeys[0])).toBe(false);
      expect(subControl.touched).toBe(false);
    });
  });

  it('should reset form group', () => {
    const control = controls.author;

    service.resetForm(formGroup, formModel, formId);

    expect(control.value).toBeNull();
  });
})
;
