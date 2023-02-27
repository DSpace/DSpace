// Load the implementations that should be tested
import { ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ComponentFixture, fakeAsync, flush, inject, TestBed, waitForAsync, } from '@angular/core/testing';
import { of as observableOf } from 'rxjs';

import { DynamicFormLayoutService, DynamicFormsCoreModule, DynamicFormValidationService } from '@ng-dynamic-forms/core';
import { DynamicFormsNGBootstrapUIModule } from '@ng-dynamic-forms/ui-ng-bootstrap';
import { NgbModule, NgbTypeaheadSelectItemEvent } from '@ng-bootstrap/ng-bootstrap';

import { VocabularyOptions } from '../../../../../../core/submission/vocabularies/models/vocabulary-options.model';
import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { VocabularyServiceStub } from '../../../../../testing/vocabulary-service.stub';
import { DsDynamicTagComponent } from './dynamic-tag.component';
import { DynamicTagModel } from './dynamic-tag.model';
import { Chips } from '../../../../chips/models/chips.model';
import { FormFieldMetadataValueObject } from '../../../models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { createTestComponent } from '../../../../../testing/utils.test';
import {
  mockDynamicFormLayoutService,
  mockDynamicFormValidationService
} from '../../../../../testing/dynamic-form-mock-services';

function createKeyUpEvent(key: number) {
  /* eslint-disable no-empty,@typescript-eslint/no-empty-function */
  const event = {
    keyCode: key, preventDefault: () => {
    }, stopPropagation: () => {
    }
  };
  /* eslint-enable no-empty, @typescript-eslint/no-empty-function */
  spyOn(event, 'preventDefault');
  spyOn(event, 'stopPropagation');
  return event;
}

let TAG_TEST_GROUP;
let TAG_TEST_MODEL_CONFIG;

function init() {
  TAG_TEST_GROUP = new FormGroup({
    tag: new FormControl(),
  });

  TAG_TEST_MODEL_CONFIG = {
    vocabularyOptions: {
      closed: false,
      name: 'common_iso_languages'
    } as VocabularyOptions,
    disabled: false,
    id: 'tag',
    label: 'Keywords',
    minChars: 3,
    name: 'tag',
    placeholder: 'Keywords',
    readOnly: false,
    required: false,
    repeatable: false
  };
}

describe('DsDynamicTagComponent test suite', () => {

  let testComp: TestComponent;
  let tagComp: DsDynamicTagComponent;
  let testFixture: ComponentFixture<TestComponent>;
  let tagFixture: ComponentFixture<DsDynamicTagComponent>;
  let html;
  let chips: Chips;
  let modelValue: any;

  // waitForAsync beforeEach
  beforeEach(waitForAsync(() => {
    const vocabularyServiceStub = new VocabularyServiceStub();
    init();
    TestBed.configureTestingModule({
      imports: [
        DynamicFormsCoreModule,
        DynamicFormsNGBootstrapUIModule,
        FormsModule,
        NgbModule,
        ReactiveFormsModule,
      ],
      declarations: [
        DsDynamicTagComponent,
        TestComponent,
      ], // declare the test component
      providers: [
        ChangeDetectorRef,
        DsDynamicTagComponent,
        { provide: VocabularyService, useValue: vocabularyServiceStub },
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
      <ds-dynamic-tag [bindId]="bindId"
                      [group]="group"
                      [model]="model"
                      (blur)="onBlur($event)"
                      (change)="onValueChange($event)"
                      (focus)="onFocus($event)"></ds-dynamic-tag>`;

      testFixture = createTestComponent(html, TestComponent) as ComponentFixture<TestComponent>;
      testComp = testFixture.componentInstance;
    });
    afterEach(() => {
      testFixture.destroy();
    });
    it('should create DsDynamicTagComponent', inject([DsDynamicTagComponent], (app: DsDynamicTagComponent) => {

      expect(app).toBeDefined();
    }));
  });

  describe('when vocabularyOptions are set', () => {
    describe('and init model value is empty', () => {
      beforeEach(() => {

        tagFixture = TestBed.createComponent(DsDynamicTagComponent);
        tagComp = tagFixture.componentInstance; // FormComponent test instance
        tagComp.group = TAG_TEST_GROUP;
        tagComp.model = new DynamicTagModel(TAG_TEST_MODEL_CONFIG);
        tagFixture.detectChanges();
      });

      afterEach(() => {
        tagFixture.destroy();
        tagComp = null;
      });

      it('should init component properly', () => {
        chips = new Chips([], 'display');
        expect(tagComp.chips.getChipsItems()).toEqual(chips.getChipsItems());
      });

      it('should search when 3+ characters typed', fakeAsync(() => {
        spyOn((tagComp as any).vocabularyService, 'getVocabularyEntriesByValue').and.callThrough();

        tagComp.search(observableOf('test')).subscribe(() => {
          expect((tagComp as any).vocabularyService.getVocabularyEntriesByValue).toHaveBeenCalled();
        });
      }));

      it('should select a results entry properly', fakeAsync(() => {
        modelValue = [
          Object.assign(new VocabularyEntry(), { authority: 1, display: 'Name, Lastname', value: 1 })
        ];
        const event: NgbTypeaheadSelectItemEvent = {
          item: Object.assign(new VocabularyEntry(), {
            authority: 1,
            display: 'Name, Lastname',
            value: 1
          }),
          preventDefault: () => {
            return;
          }
        };
        spyOn(tagComp.change, 'emit');

        tagComp.onSelectItem(event);

        tagFixture.detectChanges();
        flush();

        expect(tagComp.chips.getChipsItems()).toEqual(modelValue);
        expect(tagComp.model.value).toEqual(modelValue);
        expect(tagComp.currentValue).toBeNull();
        expect(tagComp.change.emit).toHaveBeenCalled();
      }));

      it('should emit blur Event onBlur', () => {
        spyOn(tagComp.blur, 'emit');
        tagComp.onBlur(new Event('blur'));
        expect(tagComp.blur.emit).toHaveBeenCalled();
      });

      it('should emit focus Event onFocus', () => {
        spyOn(tagComp.focus, 'emit');
        tagComp.onFocus(new Event('focus'));
        expect(tagComp.focus.emit).toHaveBeenCalled();
      });

      it('should emit change Event onBlur when currentValue is not empty', fakeAsync(() => {
        tagComp.currentValue = 'test value';
        tagFixture.detectChanges();
        spyOn(tagComp.blur, 'emit');
        spyOn(tagComp.change, 'emit');
        tagComp.onBlur(new Event('blur'));

        tagFixture.detectChanges();
        flush();

        expect(tagComp.change.emit).toHaveBeenCalled();
        expect(tagComp.blur.emit).toHaveBeenCalled();
      }));
    });

    describe('and init model value is not empty', () => {
      beforeEach(() => {

        tagFixture = TestBed.createComponent(DsDynamicTagComponent);
        tagComp = tagFixture.componentInstance; // FormComponent test instance
        tagComp.group = TAG_TEST_GROUP;
        tagComp.model = new DynamicTagModel(TAG_TEST_MODEL_CONFIG);
        modelValue = [
          new FormFieldMetadataValueObject('a', null, 'test001'),
          new FormFieldMetadataValueObject('b', null, 'test002'),
          new FormFieldMetadataValueObject('c', null, 'test003'),
        ];
        tagComp.model.value = modelValue;
        tagFixture.detectChanges();
      });

      afterEach(() => {
        tagFixture.destroy();
        tagComp = null;
      });

      it('should init component properly', () => {
        chips = new Chips(modelValue, 'display');
        expect(tagComp.chips.getChipsItems()).toEqual(chips.getChipsItems());
      });
    });

  });

  describe('when vocabularyOptions are not set', () => {
    describe('and init model value is empty', () => {
      beforeEach(() => {

        tagFixture = TestBed.createComponent(DsDynamicTagComponent);
        tagComp = tagFixture.componentInstance; // FormComponent test instance
        tagComp.group = TAG_TEST_GROUP;
        const config = TAG_TEST_MODEL_CONFIG;
        config.vocabularyOptions = null;
        tagComp.model = new DynamicTagModel(config);
        tagFixture.detectChanges();
      });

      afterEach(() => {
        tagFixture.destroy();
        tagComp = null;
      });

      it('should init component properly', () => {
        chips = new Chips([], 'display');
        expect(tagComp.chips.getChipsItems()).toEqual(chips.getChipsItems());
      });

      it('should add an item on ENTER or key press is \',\' or \';\'', fakeAsync(() => {
        let event = createKeyUpEvent(13);
        tagComp.currentValue = 'test value';

        tagFixture.detectChanges();
        tagComp.onKeyUp(event);

        flush();

        expect(tagComp.model.value).toEqual(['test value']);
        expect(tagComp.currentValue).toBeNull();

        event = createKeyUpEvent(188);
        tagComp.currentValue = 'test value';

        tagFixture.detectChanges();
        tagComp.onKeyUp(event);

        flush();

        expect(tagComp.model.value).toEqual(['test value']);
        expect(tagComp.currentValue).toBeNull();
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

  group: FormGroup = TAG_TEST_GROUP;

  model = new DynamicTagModel(TAG_TEST_MODEL_CONFIG);

  showErrorMessages = false;

}
