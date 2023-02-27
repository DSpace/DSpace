/* eslint-disable max-classes-per-file */
// Load the implementations that should be tested
import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, fakeAsync, inject, TestBed, tick, } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CdkTreeModule } from '@angular/cdk/tree';

import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { NgbModal, NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { DynamicFormLayoutService, DynamicFormsCoreModule, DynamicFormValidationService } from '@ng-dynamic-forms/core';
import { DynamicFormsNGBootstrapUIModule } from '@ng-dynamic-forms/ui-ng-bootstrap';
import { TranslateModule } from '@ngx-translate/core';

import { VocabularyOptions } from '../../../../../../core/submission/vocabularies/models/vocabulary-options.model';
import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { VocabularyServiceStub } from '../../../../../testing/vocabulary-service.stub';
import { DsDynamicOneboxComponent } from './dynamic-onebox.component';
import { DynamicOneboxModel } from './dynamic-onebox.model';
import { FormFieldMetadataValueObject } from '../../../models/form-field-metadata-value.model';
import { createTestComponent } from '../../../../../testing/utils.test';
import { AuthorityConfidenceStateDirective } from '../../../../directives/authority-confidence-state.directive';
import { ObjNgFor } from '../../../../../utils/object-ngfor.pipe';
import { VocabularyEntry } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { createSuccessfulRemoteDataObject$ } from '../../../../../remote-data.utils';
import { VocabularyTreeviewComponent } from '../../../../vocabulary-treeview/vocabulary-treeview.component';
import {
  mockDynamicFormLayoutService,
  mockDynamicFormValidationService
} from '../../../../../testing/dynamic-form-mock-services';

export let ONEBOX_TEST_GROUP;

export let ONEBOX_TEST_MODEL_CONFIG;


// Mock class for NgbModalRef
export class MockNgbModalRef {
  componentInstance = {
    vocabularyOptions: undefined,
    preloadLevel: undefined,
    selectedItem: undefined
  };
  result: Promise<any> = new Promise((resolve, reject) => resolve(true));
}

function init() {
  ONEBOX_TEST_GROUP = new FormGroup({
    onebox: new FormControl(),
  });

  ONEBOX_TEST_MODEL_CONFIG = {
    vocabularyOptions: {
      closed: false,
      name: 'vocabulary'
    } as VocabularyOptions,
    disabled: false,
    id: 'onebox',
    label: 'Conference',
    minChars: 3,
    name: 'onebox',
    placeholder: 'Conference',
    readOnly: false,
    required: false,
    repeatable: false,
    value: undefined
  };
}

describe('DsDynamicOneboxComponent test suite', () => {

  let scheduler: TestScheduler;
  let testComp: TestComponent;
  let oneboxComponent: DsDynamicOneboxComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let oneboxCompFixture: ComponentFixture<DsDynamicOneboxComponent>;
  let vocabularyServiceStub: any;
  let modalService: any;
  let html;
  let modal;
  const vocabulary = {
    id: 'vocabulary',
    name: 'vocabulary',
    scrollable: true,
    hierarchical: false,
    preloadLevel: 0,
    type: 'vocabulary',
    _links: {
      self: {
        url: 'self'
      },
      entries: {
        url: 'entries'
      }
    }
  };

  const hierarchicalVocabulary = {
    id: 'hierarchicalVocabulary',
    name: 'hierarchicalVocabulary',
    scrollable: true,
    hierarchical: true,
    preloadLevel: 2,
    type: 'vocabulary',
    _links: {
      self: {
        url: 'self'
      },
      entries: {
        url: 'entries'
      }
    }
  };

  // waitForAsync beforeEach
  beforeEach(() => {
    vocabularyServiceStub = new VocabularyServiceStub();

    modal = jasmine.createSpyObj('modal',
      {
        open: jasmine.createSpy('open'),
        close: jasmine.createSpy('close'),
        dismiss: jasmine.createSpy('dismiss'),
      }
    );
    init();
    TestBed.configureTestingModule({
      imports: [
        DynamicFormsCoreModule,
        DynamicFormsNGBootstrapUIModule,
        FormsModule,
        NgbModule,
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        CdkTreeModule
      ],
      declarations: [
        DsDynamicOneboxComponent,
        TestComponent,
        AuthorityConfidenceStateDirective,
        ObjNgFor,
        VocabularyTreeviewComponent
      ], // declare the test component
      providers: [
        ChangeDetectorRef,
        DsDynamicOneboxComponent,
        { provide: VocabularyService, useValue: vocabularyServiceStub },
        { provide: DynamicFormLayoutService, useValue: mockDynamicFormLayoutService },
        { provide: DynamicFormValidationService, useValue: mockDynamicFormValidationService },
        { provide: NgbModal, useValue: modal }
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();

  });

  describe('', () => {
    // synchronous beforeEach
    beforeEach(() => {
      html = `
      <ds-dynamic-onebox [bindId]="bindId"
                            [group]="group"
                            [model]="model"
                            (blur)="onBlur($event)"
                            (change)="onValueChange($event)"
                            (focus)="onFocus($event)"></ds-dynamic-onebox>`;

      spyOn(vocabularyServiceStub, 'findVocabularyById').and.returnValue(createSuccessfulRemoteDataObject$(vocabulary));
      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });

    afterEach(() => {
      testFixture.destroy();
    });
    it('should create DsDynamicOneboxComponent', inject([DsDynamicOneboxComponent], (app: DsDynamicOneboxComponent) => {
      expect(app).toBeDefined();
    }));
  });

  describe('Has not hierarchical vocabulary', () => {
    beforeEach(() => {
      spyOn(vocabularyServiceStub, 'findVocabularyById').and.returnValue(createSuccessfulRemoteDataObject$(vocabulary));
    });

    describe('when init model value is empty', () => {
      beforeEach(() => {

        oneboxCompFixture = TestBed.createComponent(DsDynamicOneboxComponent);
        oneboxComponent = oneboxCompFixture.componentInstance; // FormComponent test instance
        oneboxComponent.group = ONEBOX_TEST_GROUP;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        oneboxCompFixture.detectChanges();
      });

      afterEach(() => {
        oneboxCompFixture.destroy();
        oneboxComponent = null;
      });

      it('should init component properly', () => {
        expect(oneboxComponent.currentValue).not.toBeDefined();
      });

      it('should search when 3+ characters typed', fakeAsync(() => {

        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntriesByValue').and.callThrough();

        oneboxComponent.search(observableOf('test')).subscribe();

        tick(300);
        oneboxCompFixture.detectChanges();

        expect((oneboxComponent as any).vocabularyService.getVocabularyEntriesByValue).toHaveBeenCalled();
      }));

      it('should set model.value on input type when VocabularyOptions.closed is false', () => {
        const inputDe = oneboxCompFixture.debugElement.query(By.css('input.form-control'));
        const inputElement = inputDe.nativeElement;

        inputElement.value = 'test value';
        inputElement.dispatchEvent(new Event('input'));

        expect(oneboxComponent.inputValue).toEqual(new FormFieldMetadataValueObject('test value'));

      });

      it('should not set model.value on input type when VocabularyOptions.closed is true', () => {
        oneboxComponent.model.vocabularyOptions.closed = true;
        oneboxCompFixture.detectChanges();
        const inputDe = oneboxCompFixture.debugElement.query(By.css('input.form-control'));
        const inputElement = inputDe.nativeElement;

        inputElement.value = 'test value';
        inputElement.dispatchEvent(new Event('input'));

        expect(oneboxComponent.model.value).not.toBeDefined();

      });

      it('should emit blur Event onBlur when popup is closed', () => {
        spyOn(oneboxComponent.blur, 'emit');
        spyOn(oneboxComponent.instance, 'isPopupOpen').and.returnValue(false);
        oneboxComponent.onBlur(new Event('blur'));
        expect(oneboxComponent.blur.emit).toHaveBeenCalled();
      });

      it('should not emit blur Event onBlur when popup is opened', () => {
        spyOn(oneboxComponent.blur, 'emit');
        spyOn(oneboxComponent.instance, 'isPopupOpen').and.returnValue(true);
        const input = oneboxCompFixture.debugElement.query(By.css('input'));

        input.nativeElement.blur();
        expect(oneboxComponent.blur.emit).not.toHaveBeenCalled();
      });

      it('should emit change Event onBlur when VocabularyOptions.closed is false and inputValue is changed', () => {
        oneboxComponent.inputValue = 'test value';
        oneboxCompFixture.detectChanges();
        spyOn(oneboxComponent.blur, 'emit');
        spyOn(oneboxComponent.change, 'emit');
        spyOn(oneboxComponent.instance, 'isPopupOpen').and.returnValue(false);
        oneboxComponent.onBlur(new Event('blur',));
        expect(oneboxComponent.change.emit).toHaveBeenCalled();
        expect(oneboxComponent.blur.emit).toHaveBeenCalled();
      });

      it('should not emit change Event onBlur when VocabularyOptions.closed is false and inputValue is not changed', () => {
        oneboxComponent.inputValue = 'test value';
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        (oneboxComponent.model as any).value = 'test value';
        oneboxCompFixture.detectChanges();
        spyOn(oneboxComponent.blur, 'emit');
        spyOn(oneboxComponent.change, 'emit');
        spyOn(oneboxComponent.instance, 'isPopupOpen').and.returnValue(false);
        oneboxComponent.onBlur(new Event('blur',));
        expect(oneboxComponent.change.emit).not.toHaveBeenCalled();
        expect(oneboxComponent.blur.emit).toHaveBeenCalled();
      });

      it('should not emit change Event onBlur when VocabularyOptions.closed is false and inputValue is null', () => {
        oneboxComponent.inputValue = null;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        (oneboxComponent.model as any).value = 'test value';
        oneboxCompFixture.detectChanges();
        spyOn(oneboxComponent.blur, 'emit');
        spyOn(oneboxComponent.change, 'emit');
        spyOn(oneboxComponent.instance, 'isPopupOpen').and.returnValue(false);
        oneboxComponent.onBlur(new Event('blur',));
        expect(oneboxComponent.change.emit).not.toHaveBeenCalled();
        expect(oneboxComponent.blur.emit).toHaveBeenCalled();
      });

      it('should emit focus Event onFocus', () => {
        spyOn(oneboxComponent.focus, 'emit');
        oneboxComponent.onFocus(new Event('focus'));
        expect(oneboxComponent.focus.emit).toHaveBeenCalled();
      });

    });

    describe('when init model value is not empty', () => {
      beforeEach(() => {
        oneboxCompFixture = TestBed.createComponent(DsDynamicOneboxComponent);
        oneboxComponent = oneboxCompFixture.componentInstance; // FormComponent test instance
        oneboxComponent.group = ONEBOX_TEST_GROUP;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        const entry = observableOf(Object.assign(new VocabularyEntry(), {
          authority: null,
          value: 'test',
          display: 'testDisplay'
        }));
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByValue').and.returnValue(entry);
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByID').and.returnValue(entry);
        (oneboxComponent.model as any).value = new FormFieldMetadataValueObject('test', null, null, 'testDisplay');
        oneboxCompFixture.detectChanges();
      });

      afterEach(() => {
        oneboxCompFixture.destroy();
        oneboxComponent = null;
      });

      it('should init component properly', fakeAsync(() => {
        tick();
        expect(oneboxComponent.currentValue).toEqual(new FormFieldMetadataValueObject('test', null, null, 'testDisplay'));
        expect((oneboxComponent as any).vocabularyService.getVocabularyEntryByValue).toHaveBeenCalled();
      }));

      it('should emit change Event onChange and currentValue is empty', () => {
        oneboxComponent.currentValue = null;
        spyOn(oneboxComponent.change, 'emit');
        oneboxComponent.onChange(new Event('change'));
        expect(oneboxComponent.change.emit).toHaveBeenCalled();
        expect(oneboxComponent.model.value).toBeNull();
      });
    });

    describe('when init model value is not empty and has authority', () => {
      beforeEach(() => {
        oneboxCompFixture = TestBed.createComponent(DsDynamicOneboxComponent);
        oneboxComponent = oneboxCompFixture.componentInstance; // FormComponent test instance
        oneboxComponent.group = ONEBOX_TEST_GROUP;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        const entry = observableOf(Object.assign(new VocabularyEntry(), {
          authority: 'test001',
          value: 'test001',
          display: 'test'
        }));
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByValue').and.returnValue(entry);
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByID').and.returnValue(entry);
        (oneboxComponent.model as any).value = new FormFieldMetadataValueObject('test', null, 'test001');
        oneboxCompFixture.detectChanges();
      });

      afterEach(() => {
        oneboxCompFixture.destroy();
        oneboxComponent = null;
      });

      it('should init component properly', fakeAsync(() => {
        tick();
        expect(oneboxComponent.currentValue).toEqual(new FormFieldMetadataValueObject('test001', null, 'test001', 'test'));
        expect((oneboxComponent as any).vocabularyService.getVocabularyEntryByID).toHaveBeenCalled();
      }));

      it('should emit change Event onChange and currentValue is empty', () => {
        oneboxComponent.currentValue = null;
        spyOn(oneboxComponent.change, 'emit');
        oneboxComponent.onChange(new Event('change'));
        expect(oneboxComponent.change.emit).toHaveBeenCalled();
        expect(oneboxComponent.model.value).toBeNull();
      });
    });
  });

  describe('Has hierarchical vocabulary', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();
      spyOn(vocabularyServiceStub, 'findVocabularyById').and.returnValue(createSuccessfulRemoteDataObject$(hierarchicalVocabulary));
      oneboxCompFixture = TestBed.createComponent(DsDynamicOneboxComponent);
      oneboxComponent = oneboxCompFixture.componentInstance; // FormComponent test instance
      modalService = TestBed.inject(NgbModal);
      modalService.open.and.returnValue(new MockNgbModalRef());
    });

    describe('when init model value is empty', () => {
      beforeEach(() => {
        oneboxComponent.group = ONEBOX_TEST_GROUP;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        oneboxCompFixture.detectChanges();
      });

      afterEach(() => {
        oneboxCompFixture.destroy();
        oneboxComponent = null;
      });

      it('should init component properly', fakeAsync(() => {
        tick();
        expect(oneboxComponent.currentValue).not.toBeDefined();
      }));

      it('should open tree properly', (done) => {
        scheduler.schedule(() => oneboxComponent.openTree(new Event('click')));
        scheduler.flush();

        expect((oneboxComponent as any).modalService.open).toHaveBeenCalled();
        done();
      });
    });

    describe('when init model value is not empty', () => {
      beforeEach(() => {
        oneboxComponent.group = ONEBOX_TEST_GROUP;
        oneboxComponent.model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);
        const entry = observableOf(Object.assign(new VocabularyEntry(), {
          authority: null,
          value: 'test',
          display: 'testDisplay'
        }));
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByValue').and.returnValue(entry);
        spyOn((oneboxComponent as any).vocabularyService, 'getVocabularyEntryByID').and.returnValue(entry);
        (oneboxComponent.model as any).value = new FormFieldMetadataValueObject('test', null, null, 'testDisplay');
        oneboxCompFixture.detectChanges();
      });

      afterEach(() => {
        oneboxCompFixture.destroy();
        oneboxComponent = null;
      });

      it('should init component properly', fakeAsync(() => {
        tick();
        expect(oneboxComponent.currentValue).toEqual(new FormFieldMetadataValueObject('test', null, null, 'testDisplay'));
        expect((oneboxComponent as any).vocabularyService.getVocabularyEntryByValue).toHaveBeenCalled();
      }));

      it('should open tree properly', (done) => {
        scheduler.schedule(() => oneboxComponent.openTree(new Event('click')));
        scheduler.flush();

        expect((oneboxComponent as any).modalService.open).toHaveBeenCalled();
        done();
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

  group: FormGroup = ONEBOX_TEST_GROUP;

  model = new DynamicOneboxModel(ONEBOX_TEST_MODEL_CONFIG);

}

