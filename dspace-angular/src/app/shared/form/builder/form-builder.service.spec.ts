import { inject, TestBed } from '@angular/core/testing';
import {
  FormArray,
  FormControl,
  FormGroup,
  NG_ASYNC_VALIDATORS,
  NG_VALIDATORS,
  ReactiveFormsModule
} from '@angular/forms';
import {
  DynamicCheckboxGroupModel,
  DynamicCheckboxModel,
  DynamicColorPickerModel,
  DynamicDatePickerModel,
  DynamicEditorModel,
  DynamicFileUploadModel,
  DynamicFormArrayModel,
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicFormValidationService,
  DynamicFormValueControlModel,
  DynamicInputModel,
  DynamicRadioGroupModel,
  DynamicRatingModel,
  DynamicSelectModel,
  DynamicSliderModel,
  DynamicSwitchModel,
  DynamicTextAreaModel,
  DynamicTimePickerModel,
} from '@ng-dynamic-forms/core';
import { DynamicTagModel } from './ds-dynamic-form-ui/models/tag/dynamic-tag.model';
import { DynamicListCheckboxGroupModel } from './ds-dynamic-form-ui/models/list/dynamic-list-checkbox-group.model';
import { DynamicQualdropModel } from './ds-dynamic-form-ui/models/ds-dynamic-qualdrop.model';
import { DynamicScrollableDropdownModel } from './ds-dynamic-form-ui/models/scrollable-dropdown/dynamic-scrollable-dropdown.model';
import { DynamicRelationGroupModel } from './ds-dynamic-form-ui/models/relation-group/dynamic-relation-group.model';
import { DynamicLookupModel } from './ds-dynamic-form-ui/models/lookup/dynamic-lookup.model';
import { DynamicDsDatePickerModel } from './ds-dynamic-form-ui/models/date-picker/date-picker.model';
import { DynamicOneboxModel } from './ds-dynamic-form-ui/models/onebox/dynamic-onebox.model';
import { DynamicListRadioGroupModel } from './ds-dynamic-form-ui/models/list/dynamic-list-radio-group.model';
import { VocabularyOptions } from '../../../core/submission/vocabularies/models/vocabulary-options.model';
import { FormFieldModel } from './models/form-field.model';
import { SubmissionFormsModel } from '../../../core/config/models/config-submission-forms.model';
import { FormBuilderService } from './form-builder.service';
import { DynamicRowGroupModel } from './ds-dynamic-form-ui/models/ds-dynamic-row-group-model';
import { DsDynamicInputModel } from './ds-dynamic-form-ui/models/ds-dynamic-input.model';
import { FormFieldMetadataValueObject } from './models/form-field-metadata-value.model';
import { DynamicConcatModel } from './ds-dynamic-form-ui/models/ds-dynamic-concat.model';
import { DynamicLookupNameModel } from './ds-dynamic-form-ui/models/lookup/dynamic-lookup-name.model';
import { DynamicRowArrayModel } from './ds-dynamic-form-ui/models/ds-dynamic-row-array-model';
import { FormRowModel } from '../../../core/config/models/config-submission-form.model';
import {ConfigurationDataService} from '../../../core/data/configuration-data.service';
import {createSuccessfulRemoteDataObject$} from '../../remote-data.utils';
import {ConfigurationProperty} from '../../../core/shared/configuration-property.model';

describe('FormBuilderService test suite', () => {

  let testModel: DynamicFormControlModel[];
  let testFormConfiguration: SubmissionFormsModel;
  let service: FormBuilderService;
  let configSpy: ConfigurationDataService;
  const typeFieldProp = 'submit.type-bind.field';
  const typeFieldTestValue = 'dc.type';

  const submissionId = '1234';

  function testValidator() {
    return { testValidator: { valid: true } };
  }

  function testAsyncValidator() {
    return new Promise<boolean>((resolve) => setTimeout(() => resolve(true), 0));
  }

  const createConfigSuccessSpy = (...values: string[]) => jasmine.createSpyObj('configurationDataService', {
    findByPropertyName: createSuccessfulRemoteDataObject$({
      ... new ConfigurationProperty(),
      name: typeFieldProp,
      values: values,
    }),
  });

  beforeEach(() => {
    configSpy = createConfigSuccessSpy(typeFieldTestValue);
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      providers: [
        { provide: FormBuilderService, useClass: FormBuilderService },
        { provide: DynamicFormValidationService, useValue: {} },
        { provide: NG_VALIDATORS, useValue: testValidator, multi: true },
        { provide: NG_ASYNC_VALIDATORS, useValue: testAsyncValidator, multi: true },
        { provide: ConfigurationDataService, useValue: configSpy }
      ]
    });

    const vocabularyOptions: VocabularyOptions = {
      name: 'type_programme',
      closed: false
    };

    testModel = [

      new DynamicSelectModel<string>(
        {
          id: 'testSelect',
          options: [
            {
              label: 'Option 1',
              value: 'option-1'
            },
            {
              label: 'Option 2',
              value: 'option-2'
            }
          ],
          value: 'option-3'
        }
      ),

      new DynamicInputModel(
        {
          id: 'testInput',
          mask: '(000) 000-0000',
        }
      ),

      new DynamicCheckboxGroupModel(
        {
          id: 'testCheckboxGroup',
          group: [
            new DynamicCheckboxModel(
              {
                id: 'testCheckboxGroup1',
                value: true
              }
            ),
            new DynamicCheckboxModel(
              {
                id: 'testCheckboxGroup2',
                value: true
              }
            )
          ]
        }
      ),

      new DynamicRadioGroupModel<string>(
        {
          id: 'testRadioGroup',
          options: [
            {
              label: 'Option 1',
              value: 'option-1'
            },
            {
              label: 'Option 2',
              value: 'option-2'
            }
          ],
          value: 'option-3'
        }
      ),

      new DynamicTextAreaModel({ id: 'testTextArea' }),

      new DynamicCheckboxModel({ id: 'testCheckbox' }),

      new DynamicFormArrayModel(
        {
          id: 'testFormArray',
          initialCount: 5,
          groupFactory: () => {
            return [
              new DynamicInputModel({ id: 'testFormArrayGroupInput' }),
              new DynamicFormArrayModel({
                id: 'testNestedFormArray', groupFactory: () => [
                  new DynamicInputModel({ id: 'testNestedFormArrayGroupInput' })
                ]
              })
            ];
          }
        }
      ),

      new DynamicFormGroupModel(
        {
          id: 'testFormGroup',
          group: [
            new DynamicInputModel({ id: 'nestedTestInput' }),
            new DynamicTextAreaModel({ id: 'nestedTestTextArea' })
          ]
        }
      ),

      new DynamicSliderModel({ id: 'testSlider' }),

      new DynamicSwitchModel({ id: 'testSwitch' }),

      new DynamicDatePickerModel({ id: 'testDatepicker', value: new Date() }),

      new DynamicFileUploadModel({ id: 'testFileUpload' }),

      new DynamicEditorModel({ id: 'testEditor' }),

      new DynamicTimePickerModel({ id: 'testTimePicker' }),

      new DynamicRatingModel({ id: 'testRating' }),

      new DynamicColorPickerModel({ id: 'testColorPicker' }),

      new DynamicOneboxModel({
        id: 'testOnebox',
        repeatable: false,
        metadataFields: [],
        submissionId: '1234',
        hasSelectableMetadata: false,
      }),

      new DynamicScrollableDropdownModel({
        id: 'testScrollableDropdown',
        vocabularyOptions: vocabularyOptions,
        repeatable: false,
        metadataFields: [],
        submissionId: '1234',
        hasSelectableMetadata: false
      }),

      new DynamicTagModel({
        id: 'testTag',
        repeatable: false,
        metadataFields: [],
        submissionId: '1234',
        hasSelectableMetadata: false
      }),

      new DynamicListCheckboxGroupModel({
        id: 'testCheckboxList',
        vocabularyOptions: vocabularyOptions,
        repeatable: true
      }),

      new DynamicListRadioGroupModel({ id: 'testRadioList', vocabularyOptions: vocabularyOptions, repeatable: false }),

      new DynamicRelationGroupModel({
        submissionId,
        id: 'testRelationGroup',
        formConfiguration: [{
          fields: [{
            hints: 'Enter the name of the author.',
            input: { type: 'onebox' },
            label: 'Authors',
            typeBind: [],
            languageCodes: [],
            mandatory: 'true',
            mandatoryMessage: 'Required field!',
            repeatable: false,
            selectableMetadata: [{
              controlledVocabulary: 'RPAuthority',
              closed: false,
              metadata: 'dc.contributor.author'
            }]
          } as FormFieldModel]
        } as FormRowModel, {
          fields: [{
            hints: 'Enter the affiliation of the author.',
            input: { type: 'onebox' },
            label: 'Affiliation',
            languageCodes: [],
            mandatory: 'false',
            repeatable: false,
            selectableMetadata: [{
              controlledVocabulary: 'OUAuthority',
              closed: false,
              metadata: 'local.contributor.affiliation'
            }]
          } as FormFieldModel]
        } as FormRowModel],
        mandatoryField: '',
        name: 'testRelationGroup',
        relationFields: [],
        scopeUUID: '',
        submissionScope: '',
        repeatable: false,
        metadataFields: [],
        hasSelectableMetadata: true
      }),

      new DynamicDsDatePickerModel({ id: 'testDate' }),

      new DynamicLookupModel({
        id: 'testLookup',
        repeatable: false,
        metadataFields: [],
        submissionId: '1234',
        hasSelectableMetadata: true
      }),

      new DynamicLookupNameModel({
        id: 'testLookupName',
        repeatable: false,
        metadataFields: [],
        submissionId: '1234',
        hasSelectableMetadata: true
      }),

      new DynamicQualdropModel({ id: 'testCombobox', readOnly: false, required: false }),

      new DynamicRowArrayModel(
        {
          id: 'testFormRowArray',
          initialCount: 5,
          notRepeatable: false,
          relationshipConfig: undefined,
          submissionId: '1234',
          isDraggable: true,
          groupFactory: () => {
            return [
              new DynamicInputModel({ id: 'testFormRowArrayGroupInput' })
            ];
          },
          required: false,
          metadataKey: 'dc.contributor.author',
          metadataFields: ['dc.contributor.author'],
          hasSelectableMetadata: true,
          showButtons: true,
          typeBindRelations: [{ match: 'VISIBLE', operator: 'OR', when: [{id: 'dc.type', value: 'Book' }]}]
        },
      ),

      new DynamicConcatModel({
        id: 'testConcatGroup_CONCAT_GROUP',
        group: [
          new DynamicInputModel({ id: 'testConcatGroup_CONCAT_FIRST_INPUT' }),
          new DynamicInputModel({ id: 'testConcatGroup_CONCAT_SECOND_INPUT' }),
        ]
      } as any)
    ];

    testFormConfiguration = {
      name: 'testFormConfiguration',
      rows: [
        {
          fields: [
            {
              input: { type: 'lookup' },
              label: 'Journal',
              mandatory: 'false',
              repeatable: false,
              hints: 'Enter the name of the journal where the item has been\n\t\t\t\t\tpublished, if any.',
              selectableMetadata: [
                {
                  metadata: 'journal',
                  controlledVocabulary: 'JOURNALAuthority',
                  closed: false
                }
              ],
              languageCodes: []
            } as FormFieldModel,
            {
              input: { type: 'onebox' },
              label: 'Issue',
              mandatory: 'false',
              repeatable: false,
              hints: ' Enter issue number.',
              selectableMetadata: [
                {
                  metadata: 'issue'
                }
              ],
              languageCodes: []
            } as FormFieldModel,
            {
              input: { type: 'name' },
              label: 'Name',
              mandatory: 'false',
              repeatable: false,
              hints: 'Enter full name.',
              selectableMetadata: [
                {
                  metadata: 'name'
                }
              ],
              languageCodes: []
            } as FormFieldModel
          ]
        } as FormRowModel,
        {
          fields: [
            {
              hints: 'If the item has any identification numbers or codes associated with↵	it, please enter the types and the actual numbers or codes.',
              input: { type: 'onebox' },
              label: 'Identifiers',
              languageCodes: [],
              mandatory: 'false',
              repeatable: false,
              selectableMetadata: [
                { metadata: 'dc.identifier.issn', label: 'ISSN' },
                { metadata: 'dc.identifier.other', label: 'Other' },
                { metadata: 'dc.identifier.ismn', label: 'ISMN' },
                { metadata: 'dc.identifier.govdoc', label: 'Gov\'t Doc #' },
                { metadata: 'dc.identifier.uri', label: 'URI' },
                { metadata: 'dc.identifier.isbn', label: 'ISBN' },
                { metadata: 'dc.identifier.doi', label: 'DOI' },
                { metadata: 'dc.identifier.pmid', label: 'PubMed ID' },
                { metadata: 'dc.identifier.arxiv', label: 'arXiv' }
              ]
            }, {
              input: { type: 'onebox' },
              label: 'Publisher',
              mandatory: 'false',
              repeatable: false,
              hints: 'Enter the name of the publisher of the previously issued instance of this item.',
              selectableMetadata: [
                {
                  metadata: 'publisher'
                }
              ],
              languageCodes: []
            }
          ]
        } as FormRowModel,
        {
          fields: [
            {
              input: { type: 'onebox' },
              label: 'Conference',
              mandatory: 'false',
              repeatable: false,
              hints: 'Enter the name of the events, if any.',
              selectableMetadata: [
                {
                  metadata: 'conference',
                  controlledVocabulary: 'EVENTAuthority',
                  closed: false
                }
              ],
              languageCodes: []
            }
          ]
        } as FormRowModel
      ],
      self: {
        href: 'testFormConfiguration.url'
      },
      type: 'submissionform',
      _links: {
        self: {
          href: 'testFormConfiguration.url'
        }
      }
    } as any;
  });

  beforeEach(inject([FormBuilderService], (formService: FormBuilderService) => {
    service = formService;
  }));

  it('should find a dynamic form control model by id', () => {

    expect(service.findById('testCheckbox', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testCheckboxGroup', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testDatepicker', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testFormArray', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testInput', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testRadioGroup', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testSelect', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testSlider', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testSwitch', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testTextArea', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testFileUpload', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testEditor', testModel) instanceof DynamicEditorModel).toBe(true);
    expect(service.findById('testTimePicker', testModel) instanceof DynamicTimePickerModel).toBe(true);
    expect(service.findById('testRating', testModel) instanceof DynamicRatingModel).toBe(true);
    expect(service.findById('testColorPicker', testModel) instanceof DynamicColorPickerModel).toBe(true);
    expect(service.findById('testConcatGroup', testModel) instanceof DynamicConcatModel).toBe(true);
  });

  it('should find a nested dynamic form control model by id', () => {

    expect(service.findById('testCheckboxGroup1', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testCheckboxGroup2', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('nestedTestInput', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testFormRowArrayGroupInput', testModel) instanceof DynamicFormControlModel).toBe(true);
    expect(service.findById('testFormRowArrayGroupInput', testModel, 2) instanceof DynamicFormControlModel).toBe(true);
  });

  it('should create an array of form models', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');

    expect(formModel[0] instanceof DynamicRowGroupModel).toBe(true);
    expect((formModel[0] as DynamicRowGroupModel).group.length).toBe(3);
    expect((formModel[0] as DynamicRowGroupModel).get(0) instanceof DynamicLookupModel).toBe(true);
    expect((formModel[0] as DynamicRowGroupModel).get(1) instanceof DsDynamicInputModel).toBe(true);
    expect((formModel[0] as DynamicRowGroupModel).get(2) instanceof DynamicConcatModel).toBe(true);

    expect(formModel[1] instanceof DynamicRowGroupModel).toBe(true);
    expect((formModel[1] as DynamicRowGroupModel).group.length).toBe(2);
    expect((formModel[1] as DynamicRowGroupModel).get(0) instanceof DynamicQualdropModel).toBe(true);
    expect(((formModel[1] as DynamicRowGroupModel).get(0) as DynamicQualdropModel).get(0) instanceof DynamicSelectModel).toBe(true);
    expect(((formModel[1] as DynamicRowGroupModel).get(0) as DynamicQualdropModel).get(1) instanceof DsDynamicInputModel).toBe(true);
    expect((formModel[1] as DynamicRowGroupModel).get(1) instanceof DsDynamicInputModel).toBe(true);

    expect(formModel[2] instanceof DynamicRowGroupModel).toBe(true);
    expect((formModel[2] as DynamicRowGroupModel).group.length).toBe(1);
    expect((formModel[2] as DynamicRowGroupModel).get(0) instanceof DynamicOneboxModel).toBe(true);
  });

  it('should return form\'s fields value from form model', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    let value = {} as any;

    expect(service.getValueFromModel(formModel)).toEqual(value);

    ((formModel[0] as DynamicRowGroupModel).get(1) as DsDynamicInputModel).value = 'test';

    value = {
      issue: [new FormFieldMetadataValueObject('test')]
    };
    expect(service.getValueFromModel(formModel)).toEqual(value);

    ((formModel[2] as DynamicRowGroupModel).get(0) as DynamicOneboxModel).value = 'test one';
    value = {
      issue: [new FormFieldMetadataValueObject('test')],
      conference: [new FormFieldMetadataValueObject('test one')]
    };
    expect(service.getValueFromModel(formModel)).toEqual(value);
  });

  it('should clear all form\'s fields value', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    const value = {} as any;

    ((formModel[0] as DynamicRowGroupModel).get(1) as DsDynamicInputModel).value = 'test';
    ((formModel[2] as DynamicRowGroupModel).get(0) as DynamicOneboxModel).value = 'test one';

    service.clearAllModelsValue(formModel);
    expect(((formModel[0] as DynamicRowGroupModel).get(1) as DynamicOneboxModel).value).toEqual(undefined);
    expect(((formModel[2] as DynamicRowGroupModel).get(0) as DynamicOneboxModel).value).toEqual(undefined);
  });

  it('should return true when model has a custom group model as parent', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    let model = service.findById('dc_identifier_QUALDROP_VALUE', formModel);
    let modelParent = service.findById('dc_identifier_QUALDROP_GROUP', formModel);
    model.parent = modelParent;

    expect(service.isModelInCustomGroup(model)).toBe(true);

    model = service.findById('name_CONCAT_FIRST_INPUT', formModel);
    modelParent = service.findById('name_CONCAT_GROUP', formModel);
    model.parent = modelParent;

    expect(service.isModelInCustomGroup(model)).toBe(true);
  });

  it('should return true when model value is an array', () => {
    let model = service.findById('testCheckboxList', testModel) as DynamicFormArrayModel;

    expect(service.hasArrayGroupValue(model)).toBe(true);

    model = service.findById('testRadioList', testModel) as DynamicFormArrayModel;

    expect(service.hasArrayGroupValue(model)).toBe(true);

    model = service.findById('testTag', testModel) as DynamicFormArrayModel;

    expect(service.hasArrayGroupValue(model)).toBe(true);
  });

  it('should return true when model value is a map', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    const model = service.findById('dc_identifier_QUALDROP_VALUE', formModel);
    const modelParent = service.findById('dc_identifier_QUALDROP_GROUP', formModel);
    model.parent = modelParent;

    expect(service.hasMappedGroupValue(model)).toBe(true);
  });

  it('should return true when model is a Qualdrop Group', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    let model = service.findById('dc_identifier_QUALDROP_GROUP', formModel);

    expect(service.isQualdropGroup(model)).toBe(true);

    model = service.findById('name_CONCAT_GROUP', formModel);

    expect(service.isQualdropGroup(model)).toBe(false);
  });

  it('should return true when model is a Custom or List Group', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    let model = service.findById('dc_identifier_QUALDROP_GROUP', formModel);

    expect(service.isCustomOrListGroup(model)).toBe(true);

    model = service.findById('name_CONCAT_GROUP', formModel);

    expect(service.isCustomOrListGroup(model)).toBe(true);

    model = service.findById('testCheckboxList', testModel);

    expect(service.isCustomOrListGroup(model)).toBe(true);

    model = service.findById('testRadioList', testModel);

    expect(service.isCustomOrListGroup(model)).toBe(true);
  });

  it('should return true when model is a Custom Group', () => {
    const formModel = service.modelFromConfiguration(submissionId, testFormConfiguration, 'testScopeUUID');
    let model = service.findById('dc_identifier_QUALDROP_GROUP', formModel);

    expect(service.isCustomGroup(model)).toBe(true);

    model = service.findById('name_CONCAT_GROUP', formModel);

    expect(service.isCustomGroup(model)).toBe(true);

    model = service.findById('testCheckboxList', testModel);

    expect(service.isCustomGroup(model)).toBe(false);

    model = service.findById('testRadioList', testModel);

    expect(service.isCustomGroup(model)).toBe(false);
  });

  it('should return true when model is a List Group', () => {
    let model = service.findById('testCheckboxList', testModel);

    expect(service.isListGroup(model)).toBe(true);

    model = service.findById('testRadioList', testModel);

    expect(service.isListGroup(model)).toBe(true);
  });

  it('should return true when model is a Relation Group', () => {
    const model = service.findById('testRelationGroup', testModel);

    expect(service.isRelationGroup(model)).toBe(true);
  });

  it('should return true when model is a Array Row Group', () => {
    let model = service.findById('testFormRowArray', testModel, null);

    expect(service.isRowArrayGroup(model)).toBe(true);

    model = service.findById('testFormArray', testModel);

    expect(service.isRowArrayGroup(model)).toBe(false);
  });

  it('should return true when model is a Array Group', () => {
    let model = service.findById('testFormRowArray', testModel) as DynamicFormArrayModel;

    expect(service.isArrayGroup(model)).toBe(true);

    model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;

    expect(service.isArrayGroup(model)).toBe(true);
  });

  it('should return properly form control by field id', () => {
    const group = service.createFormGroup(testModel);
    const control = group.controls.testLookup;

    expect(service.getFormControlById('testLookup', group, testModel)).toEqual(control);
  });

  it('should return field id from model', () => {
    const model = service.findById('testRadioList', testModel);

    expect(service.getId(model)).toEqual('testRadioList');
  });

  it('should create a form group', () => {

    const formGroup = service.createFormGroup(testModel);

    expect(formGroup instanceof FormGroup).toBe(true);

    expect(formGroup.get('testCheckbox') instanceof FormControl).toBe(true);
    expect(formGroup.get('testCheckboxGroup') instanceof FormGroup).toBe(true);
    expect(formGroup.get('testDatepicker') instanceof FormControl).toBe(true);
    expect(formGroup.get('testFormArray') instanceof FormArray).toBe(true);
    expect(formGroup.get('testInput') instanceof FormControl).toBe(true);
    expect(formGroup.get('testRadioGroup') instanceof FormControl).toBe(true);
    expect(formGroup.get('testSelect') instanceof FormControl).toBe(true);
    expect(formGroup.get('testTextArea') instanceof FormControl).toBe(true);
    expect(formGroup.get('testFileUpload') instanceof FormControl).toBe(true);
    expect(formGroup.get('testEditor') instanceof FormControl).toBe(true);
    expect(formGroup.get('testTimePicker') instanceof FormControl).toBe(true);
    expect(formGroup.get('testRating') instanceof FormControl).toBe(true);
    expect(formGroup.get('testColorPicker') instanceof FormControl).toBe(true);
  });

  it('should throw when unknown DynamicFormControlModel id is specified in JSON', () => {

    expect(() => service.fromJSON([{ id: 'test' }]))
      .toThrow(new Error(`unknown form control model type defined on JSON object with id "test"`));
  });

  it('should resolve array group path', () => {

    service.createFormGroup(testModel);

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const nestedModel = (model.get(0).get(1) as DynamicFormArrayModel).get(0);

    expect(service.getPath(model)).toEqual(['testFormArray']);
    expect(service.getPath(nestedModel)).toEqual(['testFormArray', '0', 'testNestedFormArray', '0']);
  });

  it('should add a form control to an existing form group', () => {

    const formGroup = service.createFormGroup(testModel);
    const nestedFormGroup = formGroup.controls.testFormGroup as FormGroup;
    const nestedFormGroupModel = testModel[7] as DynamicFormGroupModel;
    const newModel1 = new DynamicInputModel({ id: 'newInput1' });
    const newModel2 = new DynamicInputModel({ id: 'newInput2' });

    service.addFormGroupControl(formGroup, testModel, newModel1);
    service.addFormGroupControl(nestedFormGroup, nestedFormGroupModel, newModel2);

    expect(formGroup.controls[newModel1.id]).toBeTruthy();
    expect(testModel[testModel.length - 1] === newModel1).toBe(true);

    expect((formGroup.controls.testFormGroup as FormGroup).controls[newModel2.id]).toBeTruthy();
    expect(nestedFormGroupModel.get(nestedFormGroupModel.group.length - 1) === newModel2).toBe(true);
  });

  it('should insert a form control to an existing form group', () => {

    const formGroup = service.createFormGroup(testModel);
    const nestedFormGroup = formGroup.controls.testFormGroup as FormGroup;
    const nestedFormGroupModel = testModel[7] as DynamicFormGroupModel;
    const newModel1 = new DynamicInputModel({ id: 'newInput1' });
    const newModel2 = new DynamicInputModel({ id: 'newInput2' });

    service.insertFormGroupControl(4, formGroup, testModel, newModel1);
    service.insertFormGroupControl(0, nestedFormGroup, nestedFormGroupModel, newModel2);

    expect(formGroup.controls[newModel1.id]).toBeTruthy();
    expect(testModel[4] === newModel1).toBe(true);
    expect(service.getPath(testModel[4])).toEqual(['newInput1']);

    expect((formGroup.controls.testFormGroup as FormGroup).controls[newModel2.id]).toBeTruthy();
    expect(nestedFormGroupModel.get(0) === newModel2).toBe(true);
    expect(service.getPath(nestedFormGroupModel.get(0))).toEqual(['testFormGroup', 'newInput2']);
  });

  it('should move an existing form control to a different group position', () => {

    const formGroup = service.createFormGroup(testModel);
    const nestedFormGroupModel = testModel[7] as DynamicFormGroupModel;
    const model1 = testModel[0];
    const model2 = nestedFormGroupModel.get(0);

    service.moveFormGroupControl(0, 2, testModel);

    expect(formGroup.controls[model1.id]).toBeTruthy();
    expect(testModel[2] === model1).toBe(true);

    service.moveFormGroupControl(0, 1, nestedFormGroupModel);

    expect((formGroup.controls.testFormGroup as FormGroup).controls[model2.id]).toBeTruthy();
    expect(nestedFormGroupModel.get(1) === model2).toBe(true);
  });

  it('should remove a form control from an existing form group', () => {

    const formGroup = service.createFormGroup(testModel);
    const nestedFormGroup = formGroup.controls.testFormGroup as FormGroup;
    const nestedFormGroupModel = testModel[7] as DynamicFormGroupModel;
    const length = testModel.length;
    const size = nestedFormGroupModel.size();
    const index = 1;
    const id1 = testModel[index].id;
    const id2 = nestedFormGroupModel.get(index).id;

    service.removeFormGroupControl(index, formGroup, testModel);

    expect(Object.keys(formGroup.controls).length).toBe(length - 1);
    expect(formGroup.controls[id1]).toBeUndefined();

    expect(testModel.length).toBe(length - 1);
    expect(service.findById(id1, testModel)).toBeNull();

    service.removeFormGroupControl(index, nestedFormGroup, nestedFormGroupModel);

    expect(Object.keys(nestedFormGroup.controls).length).toBe(size - 1);
    expect(nestedFormGroup.controls[id2]).toBeUndefined();

    expect(nestedFormGroupModel.size()).toBe(size - 1);
    expect(service.findById(id2, testModel)).toBeNull();
  });

  it('should create a form array', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    let formArray;

    expect(service.createFormArray).toBeTruthy();

    formArray = service.createFormArray(model);

    expect(formArray instanceof FormArray).toBe(true);
    expect(formArray.length).toBe(model.initialCount);
  });

  it('should add a form array group', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);

    service.addFormArrayGroup(formArray, model);

    expect(formArray.length).toBe(model.initialCount + 1);
  });

  it('should insert a form array group', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);

    service.insertFormArrayGroup(0, formArray, model);

    expect(formArray.length).toBe(model.initialCount + 1);
  });

  it('should move up a form array group', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);
    const index = 3;
    const step = 1;

    (formArray.at(index) as FormGroup).controls.testFormArrayGroupInput.setValue('next test value 1');
    (formArray.at(index + step) as FormGroup).controls.testFormArrayGroupInput.setValue('next test value 2');

    (model.get(index).get(0) as DynamicFormValueControlModel<any>).value = 'next test value 1';
    (model.get(index + step).get(0) as DynamicFormValueControlModel<any>).value = 'next test value 2';

    service.moveFormArrayGroup(index, step, formArray, model);

    expect(formArray.length).toBe(model.initialCount);

    expect((formArray.at(index) as FormGroup).controls.testFormArrayGroupInput.value).toEqual('next test value 2');
    expect((formArray.at(index + step) as FormGroup).controls.testFormArrayGroupInput.value).toEqual('next test value 1');

    expect((model.get(index).get(0) as DynamicFormValueControlModel<any>).value).toEqual('next test value 2');
    expect((model.get(index + step).get(0) as DynamicFormValueControlModel<any>).value).toEqual('next test value 1');
  });

  it('should move down a form array group', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);
    const index = 3;
    const step = -1;

    (formArray.at(index) as FormGroup).controls.testFormArrayGroupInput.setValue('next test value 1');
    (formArray.at(index + step) as FormGroup).controls.testFormArrayGroupInput.setValue('next test value 2');

    (model.get(index).get(0) as DynamicFormValueControlModel<any>).value = 'next test value 1';
    (model.get(index + step).get(0) as DynamicFormValueControlModel<any>).value = 'next test value 2';

    service.moveFormArrayGroup(index, step, formArray, model);

    expect(formArray.length).toBe(model.initialCount);

    expect((formArray.at(index) as FormGroup).controls.testFormArrayGroupInput.value).toEqual('next test value 2');
    expect((formArray.at(index + step) as FormGroup).controls.testFormArrayGroupInput.value).toEqual('next test value 1');

    expect((model.get(index).get(0) as DynamicFormValueControlModel<any>).value).toEqual('next test value 2');
    expect((model.get(index + step).get(0) as DynamicFormValueControlModel<any>).value).toEqual('next test value 1');
  });

  it('should throw when form array group is to be moved out of bounds', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);

    expect(() => service.moveFormArrayGroup(2, -5, formArray, model))
      .toThrow(new Error(`form array group cannot be moved due to index or new index being out of bounds`));
  });

  it('should remove a form array group', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);

    service.removeFormArrayGroup(0, formArray, model);

    expect(formArray.length).toBe(model.initialCount - 1);
  });

  it('should clear a form array', () => {

    const model = service.findById('testFormArray', testModel) as DynamicFormArrayModel;
    const formArray = service.createFormArray(model);

    service.clearFormArray(formArray, model);

    expect(formArray.length === 0).toBe(true);
  });

  it(`should request the ${typeFieldProp} property and set value "dc_type"`, () => {
    const typeValue = service.getTypeField();
    expect(configSpy.findByPropertyName).toHaveBeenCalledTimes(1);
    expect(configSpy.findByPropertyName).toHaveBeenCalledWith(typeFieldProp);
    expect(typeValue).toEqual('dc_type');
  });

});
