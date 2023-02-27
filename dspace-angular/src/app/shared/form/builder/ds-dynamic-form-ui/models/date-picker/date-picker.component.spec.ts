// Load the implementations that should be tested
import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, inject, TestBed, waitForAsync, } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { DynamicFormLayoutService, DynamicFormValidationService } from '@ng-dynamic-forms/core';

import { DsDatePickerComponent } from './date-picker.component';
import { DynamicDsDatePickerModel } from './date-picker.model';
import { createTestComponent } from '../../../../../testing/utils.test';
import {
  mockDynamicFormLayoutService,
  mockDynamicFormValidationService
} from '../../../../../testing/dynamic-form-mock-services';


export const DATE_TEST_GROUP = new FormGroup({
  date: new FormControl()
});

export const DATE_TEST_MODEL_CONFIG = {
  disabled: false,
  errorMessages: { required: 'You must enter at least the year.' },
  id: 'date',
  label: 'Date',
  name: 'date',
  placeholder: 'Date',
  readOnly: false,
  required: true,
  toggleIcon: 'fas fa-calendar'
};

describe('DsDatePickerComponent test suite', () => {

  let testComp: TestComponent;
  let dateComp: DsDatePickerComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let dateFixture: ComponentFixture<DsDatePickerComponent>;
  let html;

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      imports: [
        NgbModule
      ],
      declarations: [
        DsDatePickerComponent,
        TestComponent,
      ], // declare the test component
      providers: [
        ChangeDetectorRef,
        DsDatePickerComponent,
        { provide: DynamicFormLayoutService, useValue: mockDynamicFormLayoutService },
        { provide: DynamicFormValidationService, useValue: mockDynamicFormValidationService }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

  }));

  describe('', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
           <ds-date-picker
            [bindId]='bindId'
            [group]='group'
            [model]='model'
            [legend]='legend'
            (blur)='onBlur($event)'
            (change)='onValueChange($event)'
            (focus)='onFocus($event)'></ds-date-picker>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    it('should create DsDatePickerComponent', inject([DsDatePickerComponent], (app: DsDatePickerComponent) => {

      expect(app).toBeDefined();
    }));

  });

  describe('', () => {
    describe('when init model value is empty', () => {
      beforeEach(() => {

        dateFixture = TestBed.createComponent(DsDatePickerComponent);
        dateComp = dateFixture.componentInstance; // FormComponent test instance
        dateComp.group = DATE_TEST_GROUP;
        dateComp.model = new DynamicDsDatePickerModel(DATE_TEST_MODEL_CONFIG);
        dateFixture.detectChanges();
      });

      it('should init component properly', () => {
        expect(dateComp.initialYear).toBeDefined();
        expect(dateComp.initialMonth).toBeDefined();
        expect(dateComp.initialDay).toBeDefined();
        expect(dateComp.maxYear).toBeDefined();
        expect(dateComp.disabledMonth).toBeTruthy();
        expect(dateComp.disabledDay).toBeTruthy();
      });

      it('should set year and enable month field when year field is entered', () => {
        const event = {
          field: 'year',
          value: '1983'
        };
        dateComp.onChange(event);

        expect(dateComp.year).toEqual('1983');
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeTruthy();
      });

      it('should set month and enable day field when month field is entered', () => {
        const event = {
          field: 'month',
          value: '11'
        };

        dateComp.year = '1983';
        dateComp.disabledMonth = false;
        dateFixture.detectChanges();

        dateComp.onChange(event);

        expect(dateComp.year).toEqual('1983');
        expect(dateComp.month).toEqual('11');
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeFalsy();
      });

      it('should set day when day field is entered', () => {
        const event = {
          field: 'day',
          value: '18'
        };

        dateComp.year = '1983';
        dateComp.month = '11';
        dateComp.disabledMonth = false;
        dateComp.disabledDay = false;
        dateFixture.detectChanges();

        dateComp.onChange(event);

        expect(dateComp.year).toEqual('1983');
        expect(dateComp.month).toEqual('11');
        expect(dateComp.day).toEqual('18');
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeFalsy();
      });

      it('should emit blur Event onBlur', () => {
        spyOn(dateComp.blur, 'emit');
        dateComp.onBlur(new Event('blur'));
        expect(dateComp.blur.emit).toHaveBeenCalled();
      });

      it('should emit focus Event onFocus', () => {
        spyOn(dateComp.focus, 'emit');
        dateComp.onFocus(new Event('focus'));
        expect(dateComp.focus.emit).toHaveBeenCalled();
      });
    });

    describe('when init model value is not empty', () => {
      beforeEach(() => {

        dateFixture = TestBed.createComponent(DsDatePickerComponent);
        dateComp = dateFixture.componentInstance; // FormComponent test instance
        dateComp.group = DATE_TEST_GROUP;
        dateComp.model = new DynamicDsDatePickerModel(DATE_TEST_MODEL_CONFIG);
        dateComp.model.value = '1983-11-18';
        dateFixture.detectChanges();
      });

      it('should init component properly', () => {
        expect(dateComp.initialYear).toBeDefined();
        expect(dateComp.initialMonth).toBeDefined();
        expect(dateComp.initialDay).toBeDefined();
        expect(dateComp.maxYear).toBeDefined();
        expect(dateComp.year).toBe(1983);
        expect(dateComp.month).toBe(11);
        expect(dateComp.day).toBe(18);
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeFalsy();
      });

      it('should disable month and day fields when year field is canceled', () => {
        const event = {
          field: 'year',
          value: null
        };
        dateComp.onChange(event);

        expect(dateComp.year).not.toBeDefined();
        expect(dateComp.month).not.toBeDefined();
        expect(dateComp.day).not.toBeDefined();
        expect(dateComp.disabledMonth).toBeTruthy();
        expect(dateComp.disabledDay).toBeTruthy();
      });

      it('should disable day field when month field is canceled', () => {
        const event = {
          field: 'month',
          value: null
        };
        dateComp.onChange(event);

        expect(dateComp.year).toBe(1983);
        expect(dateComp.month).not.toBeDefined();
        expect(dateComp.day).not.toBeDefined();
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeTruthy();
      });

      it('should not disable day field when day field is canceled', () => {
        const event = {
          field: 'day',
          value: null
        };
        dateComp.onChange(event);

        expect(dateComp.year).toBe(1983);
        expect(dateComp.month).toBe(11);
        expect(dateComp.day).not.toBeDefined();
        expect(dateComp.disabledMonth).toBeFalsy();
        expect(dateComp.disabledDay).toBeFalsy();
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

  group = DATE_TEST_GROUP;

  model = new DynamicDsDatePickerModel(DATE_TEST_MODEL_CONFIG);

  showErrorMessages = false;

}
