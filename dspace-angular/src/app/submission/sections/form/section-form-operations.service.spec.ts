import { TestBed, waitForAsync } from '@angular/core/testing';

import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import {
  DYNAMIC_FORM_CONTROL_TYPE_ARRAY,
  DYNAMIC_FORM_CONTROL_TYPE_GROUP,
  DynamicFormControlEvent,
  DynamicInputModel
} from '@ng-dynamic-forms/core';

import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { getMockFormBuilderService } from '../../../shared/mocks/form-builder-service.mock';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { SectionFormOperationsService } from './section-form-operations.service';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { FormFieldPreviousValueObject } from '../../../shared/form/builder/models/form-field-previous-value-object';
import {
  mockInputWithAuthorityValueModel,
  mockInputWithFormFieldValueModel,
  mockInputWithLanguageAndAuthorityArrayModel,
  mockInputWithLanguageAndAuthorityModel,
  mockInputWithLanguageModel,
  mockInputWithObjectValueModel,
  mockQualdropInputModel,
  MockQualdropModel,
  MockRelationModel,
  mockRowGroupModel
} from '../../../shared/mocks/form-models.mock';
import { FormFieldMetadataValueObject } from '../../../shared/form/builder/models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { DynamicRowArrayModel } from '../../../shared/form/builder/ds-dynamic-form-ui/models/ds-dynamic-row-array-model';

describe('SectionFormOperationsService test suite', () => {
  let formBuilderService: any;
  let service: SectionFormOperationsService;
  let serviceAsAny: any;

  const jsonPatchOpBuilder: any = jasmine.createSpyObj('jsonPatchOpBuilder', {
      add: jasmine.createSpy('add'),
      replace: jasmine.createSpy('replace'),
      remove: jasmine.createSpy('remove'),
    });
  const pathCombiner = new JsonPatchOperationPathCombiner('sections', 'test');

  const dynamicFormControlChangeEvent: DynamicFormControlEvent = {
    $event: new Event('change'),
    context: null,
    control: null,
    group: null,
    model: null,
    type: 'change'
  };

  const dynamicFormControlRemoveEvent: DynamicFormControlEvent = {
    $event: new Event('change'),
    context: null,
    control: null,
    group: null,
    model: null,
    type: 'remove'
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      providers: [
        { provide: FormBuilderService, useValue: getMockFormBuilderService() },
        { provide: JsonPatchOperationsBuilder, useValue: jsonPatchOpBuilder },
        SectionFormOperationsService
      ]
    }).compileComponents().then();
  }));

  beforeEach(() => {
    service = TestBed.inject(SectionFormOperationsService);
    serviceAsAny = service;
    formBuilderService = TestBed.inject(FormBuilderService);
  });

  afterEach(() => {
    jsonPatchOpBuilder.add.calls.reset();
    jsonPatchOpBuilder.remove.calls.reset();
  });

  describe('dispatchOperationsFromEvent', () => {
    it('should call dispatchOperationsFromRemoveEvent on remove event', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      spyOn(serviceAsAny, 'dispatchOperationsFromRemoveEvent');

      service.dispatchOperationsFromEvent(pathCombiner, dynamicFormControlRemoveEvent, previousValue, true);

      expect(serviceAsAny.dispatchOperationsFromRemoveEvent).toHaveBeenCalled();
    });

    it('should call dispatchOperationsFromChangeEvent on change event', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      spyOn(serviceAsAny, 'dispatchOperationsFromChangeEvent');

      service.dispatchOperationsFromEvent(pathCombiner, dynamicFormControlChangeEvent, previousValue, true);

      expect(serviceAsAny.dispatchOperationsFromChangeEvent).toHaveBeenCalled();
    });
  });

  describe('getArrayIndexFromEvent', () => {
    it('should return the index of the array to which the element belongs', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        context: {
          index: 1
        }
      });

      expect(service.getArrayIndexFromEvent(event)).toBe(1);
    });

    it('should return the index of the array to which the parent element belongs', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: {
            parent: {
              index: 2
            }
          }
        }
      });
      spyOn(serviceAsAny, 'isPartOfArrayOfGroup').and.returnValue(true);

      expect(service.getArrayIndexFromEvent(event)).toBe(2);
    });

    it('should return zero when element doesn\'t belong to an array', () => {

      spyOn(serviceAsAny, 'isPartOfArrayOfGroup').and.returnValue(false);

      expect(service.getArrayIndexFromEvent(dynamicFormControlChangeEvent)).toBe(0);
    });

    it('should return zero when event is empty', () => {

      expect(service.getArrayIndexFromEvent(null)).toBe(0);
    });

  });

  describe('isPartOfArrayOfGroup', () => {
    it('should return true when parent element belongs to an array group element', () => {
      const model = {
        parent: {
          type: DYNAMIC_FORM_CONTROL_TYPE_GROUP,
          parent: {
            context: {
              type: DYNAMIC_FORM_CONTROL_TYPE_ARRAY
            }
          }
        }
      };

      expect(service.isPartOfArrayOfGroup(model as any)).toBeTruthy();
    });

    it('should return false when parent element doesn\'t belong to an array group element', () => {
      const model = {
        parent: null
      };

      expect(service.isPartOfArrayOfGroup(model as any)).toBeFalsy();
    });

  });

  describe('getQualdropValueMap', () => {
    it('should return map properly', () => {
      const context = {
        groups: [
          {
            group: [MockQualdropModel]
          }
        ]
      };
      const model = {
        parent: {
          parent: {
            context: context
          }
        }
      };
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: model
      });
      const expectMap = new Map();
      expectMap.set(MockQualdropModel.qualdropId, [MockQualdropModel.value]);

      formBuilderService.isQualdropGroup.and.returnValue(false);

      expect(service.getQualdropValueMap(event)).toEqual(expectMap);
    });

    it('should return map properly when model is DynamicQualdropModel', () => {
      const context = {
        groups: [
          {
            group: [MockQualdropModel]
          }
        ]
      };
      const model = {
        parent: {
          context: context
        }
      };
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: model
      });
      const expectMap = new Map();
      expectMap.set(MockQualdropModel.qualdropId, [MockQualdropModel.value]);

      formBuilderService.isQualdropGroup.and.returnValue(true);

      expect(service.getQualdropValueMap(event)).toEqual(expectMap);
    });
  });

  describe('getFieldPathFromEvent', () => {
    it('should field path properly', () => {
      const spy = spyOn(serviceAsAny, 'getArrayIndexFromEvent');
      spy.and.returnValue(1);
      spyOn(serviceAsAny, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');

      expect(service.getFieldPathFromEvent(dynamicFormControlChangeEvent)).toBe('path/1');

      spy.and.returnValue(undefined);

      expect(service.getFieldPathFromEvent(dynamicFormControlChangeEvent)).toBe('path');
    });
  });

  describe('getQualdropItemPathFromEvent', () => {
    it('should return path properly', () => {
      const context = {
        groups: [
          {
            group: [MockQualdropModel]
          }
        ]
      };
      const model = {
        parent: {
          parent: {
            context: context
          }
        }
      };
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: model
      });
      const expectPath = 'dc.identifier.issn/0';
      spyOn(serviceAsAny, 'getArrayIndexFromEvent').and.returnValue(0);

      formBuilderService.isQualdropGroup.and.returnValue(false);

      expect(service.getQualdropItemPathFromEvent(event)).toEqual(expectPath);
    });

    it('should return path properly when model is DynamicQualdropModel', () => {
      const context = {
        groups: [
          {
            group: [MockQualdropModel]
          }
        ]
      };
      const model = {
        parent: {
          context: context
        }
      };
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: model
      });
      const expectPath = 'dc.identifier.issn/0';
      spyOn(serviceAsAny, 'getArrayIndexFromEvent').and.returnValue(0);

      formBuilderService.isQualdropGroup.and.returnValue(true);

      expect(service.getQualdropItemPathFromEvent(event)).toEqual(expectPath);
    });
  });

  describe('getFieldPathSegmentedFromChangeEvent', () => {
    it('should return field segmented path properly', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockQualdropInputModel
      });
      formBuilderService.isQualdropGroup.and.returnValues(false, false);

      expect(service.getFieldPathSegmentedFromChangeEvent(event)).toEqual('path');
    });

    it('should return field segmented path properly when model is DynamicQualdropModel', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: MockQualdropModel
      });
      formBuilderService.isQualdropGroup.and.returnValue(true);

      expect(service.getFieldPathSegmentedFromChangeEvent(event)).toEqual('dc.identifier.issn');
    });

    it('should return field segmented path properly when model belongs to a DynamicQualdropModel', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: MockQualdropModel
        }
      });
      formBuilderService.isQualdropGroup.and.returnValues(false, true);

      expect(service.getFieldPathSegmentedFromChangeEvent(event)).toEqual('dc.identifier.issn');
    });

  });

  describe('getFieldValueFromChangeEvent', () => {
    it('should return field value properly when model belongs to a DynamicQualdropModel', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: MockQualdropModel
        }
      });
      formBuilderService.isModelInCustomGroup.and.returnValue(true);
      const expectedValue = 'test';

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);
    });

    it('should return field value properly when model is DynamicRelationGroupModel', () => {
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: MockRelationModel
      });
      formBuilderService.isModelInCustomGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(true);
      const expectedValue = {
        journal: [
          'journal test 1',
          'journal test 2'
        ],
        issue: [
          'issue test 1',
          'issue test 2'
        ],
      };

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);
    });

    it('should return field value properly when model has language', () => {
      let event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithLanguageModel
      });
      formBuilderService.isModelInCustomGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      let expectedValue: any = new FormFieldMetadataValueObject(mockInputWithLanguageModel.value, 'en_US');

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);

      event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithLanguageAndAuthorityModel
      });
      expectedValue = Object.assign(new VocabularyEntry(), mockInputWithLanguageAndAuthorityModel.value, {language: mockInputWithLanguageAndAuthorityModel.language});

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);

      event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithLanguageAndAuthorityArrayModel
      });
      expectedValue = [
        Object.assign(new VocabularyEntry(), mockInputWithLanguageAndAuthorityArrayModel.value[0],
        { language: mockInputWithLanguageAndAuthorityArrayModel.language }
        )
      ];

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);
    });

    it('should return field value properly when model has an object as value', () => {
      let event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithFormFieldValueModel
      });
      formBuilderService.isModelInCustomGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      let expectedValue: any = mockInputWithFormFieldValueModel.value;

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);

      event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithAuthorityValueModel
      });
      expectedValue = mockInputWithAuthorityValueModel.value;

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);

      event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: mockInputWithObjectValueModel
      });
      expectedValue = mockInputWithObjectValueModel.value;

      expect(service.getFieldValueFromChangeEvent(event)).toEqual(expectedValue);
    });
  });

  describe('getValueMap', () => {
    it('should return field segmented path properly when model belongs to a DynamicQualdropModel', () => {
      const items = [
        { path: 'test1' },
        { path: 'test2' },
        { path1: 'test3' },
      ];
      const expectedMap = new Map();
      expectedMap.set('path', ['test1', 'test2']);
      expectedMap.set('path1', ['test3']);

      expect(service.getValueMap(items)).toEqual(expectedMap);
    });
  });

  describe('dispatchOperationsFromRemoveEvent', () => {
    it('should call dispatchOperationsFromMap when is Qualdrop Group model', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue('test');
      spyOn(serviceAsAny, 'getQualdropValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(true);

      serviceAsAny.dispatchOperationsFromRemoveEvent(pathCombiner, dynamicFormControlRemoveEvent, previousValue);

      expect(serviceAsAny.dispatchOperationsFromMap).toHaveBeenCalled();
    });

    it('should dispatch a json-path remove operation', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue('test');
      formBuilderService.isQualdropGroup.and.returnValue(false);

      serviceAsAny.dispatchOperationsFromRemoveEvent(pathCombiner, dynamicFormControlRemoveEvent, previousValue);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalled();
    });

  });

  describe('dispatchOperationsFromChangeEvent', () => {
    it('should call dispatchOperationsFromMap when is Qualdrop Group model', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: MockQualdropModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue('test');
      spyOn(serviceAsAny, 'getQualdropValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(true);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(serviceAsAny.getQualdropValueMap).toHaveBeenCalled();
      expect(serviceAsAny.dispatchOperationsFromMap).toHaveBeenCalled();
    });

    it('should call dispatchOperationsFromMap when is Qualdrop Group model', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: MockRelationModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue('test');
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(true);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(serviceAsAny.getValueMap).toHaveBeenCalled();
      expect(serviceAsAny.dispatchOperationsFromMap).toHaveBeenCalled();
    });

    it('should dispatch a json-path add operation', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue('test');
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(true);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(pathCombiner.getPath('path'), 'test', true);
    });

    it('should dispatch a json-path remove operation when previous path is equal and there is no value', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      const spyPath = spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject());
      const spyIndex = spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path'));

      spyIndex.and.returnValue(1);
      spyPath.and.returnValue('path/1');
      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path/1'));
    });

    it('should dispatch a json-path replace operation when previous path is equal and there is no value', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject('test'));
      spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.replace).toHaveBeenCalledWith(
        pathCombiner.getPath('path/0'),
        new FormFieldMetadataValueObject('test'));
    });

    it('should dispatch a json-path remove operation when has a stored value', () => {
      let previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      const spyPath = spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject());
      const spyIndex = spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, true);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path'));

      spyIndex.and.returnValue(1);
      spyPath.and.returnValue('path/1');
      previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, true);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path/1'));
    });

    it('should dispatch a json-path replace operation when has a stored value', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject('test'));
      spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, true);

      expect(jsonPatchOpBuilder.replace).toHaveBeenCalledWith(
        pathCombiner.getPath('path/0'),
        new FormFieldMetadataValueObject('test'));
    });

    it('should dispatch a json-path add operation when has a stored value but previous value is empty', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], null);
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject('test'));
      spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, true);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(
        pathCombiner.getPath('path'),
        new FormFieldMetadataValueObject('test'),
        true);
    });

    it('should dispatch a json-path add operation when has a value and field index is zero or undefined', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      const spyPath = spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/0');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject('test'));
      const spyIndex = spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(
        pathCombiner.getPath('path'),
        new FormFieldMetadataValueObject('test'),
        true);

      spyIndex.and.returnValue(undefined);
      spyPath.and.returnValue('path');
      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, true);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(
        pathCombiner.getPath('path'),
        new FormFieldMetadataValueObject('test'),
        true);
    });

    it('should dispatch a json-path add operation when has a value', () => {
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], 'value');
      const event = Object.assign({}, dynamicFormControlChangeEvent, {
        model: {
          parent: mockRowGroupModel
        }
      });
      spyOn(service, 'getFieldPathFromEvent').and.returnValue('path/1');
      spyOn(service, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      spyOn(service, 'getFieldValueFromChangeEvent').and.returnValue(new FormFieldMetadataValueObject('test'));
      spyOn(service, 'getArrayIndexFromEvent').and.returnValue(0);
      spyOn(serviceAsAny, 'getValueMap');
      spyOn(serviceAsAny, 'dispatchOperationsFromMap');
      formBuilderService.isQualdropGroup.and.returnValue(false);
      formBuilderService.isRelationGroup.and.returnValue(false);
      formBuilderService.hasArrayGroupValue.and.returnValue(false);
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.dispatchOperationsFromChangeEvent(pathCombiner, event, previousValue, false);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(
        pathCombiner.getPath('path'),
        new FormFieldMetadataValueObject('test'),
        true
      );
    });
  });

  describe('dispatchOperationsFromMap', () => {
    it('should dispatch a json-path remove operation when event type is remove', () => {
      const valueMap = new Map();
      valueMap.set('path', ['testMap']);
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], valueMap);
      spyOn(serviceAsAny, 'getQualdropItemPathFromEvent').and.returnValue('path/test');

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlRemoveEvent, previousValue);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path/test'));
    });

    it('should dispatch a json-path add operation when a map has a new entry', () => {
      const valueMap = new Map();
      valueMap.set('path', ['testMapNew']);
      const previousValueMap = new Map();
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], previousValueMap);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlChangeEvent, previousValue);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(pathCombiner.getPath('path'), ['testMapNew'], true);
    });

    it('should dispatch a json-path add operation when a map entry has changed', () => {
      const valueMap = new Map();
      valueMap.set('path', ['testMapNew']);
      const previousValueMap = new Map();
      previousValueMap.set('path', ['testMap']);
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], previousValueMap);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlChangeEvent, previousValue);

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(pathCombiner.getPath('path'), ['testMapNew'], true);
    });

    it('should dispatch a json-path remove operation when a map entry has changed', () => {
      const valueMap = new Map();
      valueMap.set('path', ['testMap']);
      valueMap.set('path2', false);
      const previousValueMap = new Map();
      previousValueMap.set('path', ['testMap']);
      previousValueMap.set('path2', ['testMap2']);
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], previousValueMap);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlChangeEvent, previousValue);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path2'));
    });

    it('should dispatch a json-path remove operation when current value is an empty map', () => {
      const valueMap = new Map();
      const previousValueMap = new Map();
      previousValueMap.set('path', ['testMap']);
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], previousValueMap);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlChangeEvent, previousValue);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path'));
    });

    it('should dispatch a json-path remove operation when current value has a null entry and previous value is an empty map', () => {
      const valueMap = new Map();
      valueMap.set('path', [null]);
      const previousValueMap = new Map();
      const previousValue = new FormFieldPreviousValueObject(['path', 'test'], previousValueMap);
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.dispatchOperationsFromMap(valueMap, pathCombiner, dynamicFormControlChangeEvent, previousValue);

      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path'));
    });
  });

  describe('handleArrayGroupPatch', () => {
    let arrayModel;
    let previousValue;
    beforeEach(() => {
      arrayModel = new DynamicRowArrayModel(
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
          typeBindRelations: []
        }
      );
      spyOn(serviceAsAny, 'getFieldPathSegmentedFromChangeEvent').and.returnValue('path');
      previousValue = new FormFieldPreviousValueObject(['path'], null);
    });

    it('should not dispatch a json-path operation when a array value is empty', () => {
      formBuilderService.getValueFromModel.and.returnValue({});
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.handleArrayGroupPatch(
        pathCombiner,
        dynamicFormControlChangeEvent,
        arrayModel,
        previousValue
      );

      expect(jsonPatchOpBuilder.add).not.toHaveBeenCalled();
      expect(jsonPatchOpBuilder.remove).not.toHaveBeenCalled();
    });

    it('should dispatch a json-path add operation when a array value is not empty', () => {
      const pathValue = [
        new FormFieldMetadataValueObject('test'),
        new FormFieldMetadataValueObject('test two')
      ];
      formBuilderService.getValueFromModel.and.returnValue({
        path:pathValue
      });
      spyOn(previousValue, 'isPathEqual').and.returnValue(false);

      serviceAsAny.handleArrayGroupPatch(
        pathCombiner,
        dynamicFormControlChangeEvent,
        arrayModel,
        previousValue
      );

      expect(jsonPatchOpBuilder.add).toHaveBeenCalledWith(
        pathCombiner.getPath('path'),
        pathValue,
        false
      );
      expect(jsonPatchOpBuilder.remove).not.toHaveBeenCalled();
    });

    it('should dispatch a json-path remove operation when a array value is empty and has previous value', () => {
      formBuilderService.getValueFromModel.and.returnValue({});
      spyOn(previousValue, 'isPathEqual').and.returnValue(true);

      serviceAsAny.handleArrayGroupPatch(
        pathCombiner,
        dynamicFormControlChangeEvent,
        arrayModel,
        previousValue
      );

      expect(jsonPatchOpBuilder.add).not.toHaveBeenCalled();
      expect(jsonPatchOpBuilder.remove).toHaveBeenCalledWith(pathCombiner.getPath('path'));
    });
  });
});
