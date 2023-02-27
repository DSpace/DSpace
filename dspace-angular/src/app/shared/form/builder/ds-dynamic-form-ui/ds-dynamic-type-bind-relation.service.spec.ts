import {inject, TestBed} from '@angular/core/testing';

import {
  DynamicFormControlRelation,
  DynamicFormRelationService,
  MATCH_VISIBLE,
  OR_OPERATOR,
  HIDDEN_MATCHER,
  HIDDEN_MATCHER_PROVIDER, REQUIRED_MATCHER_PROVIDER, DISABLED_MATCHER_PROVIDER,
} from '@ng-dynamic-forms/core';

import {
  mockInputWithTypeBindModel, MockRelationModel
} from '../../../mocks/form-models.mock';
import {DsDynamicTypeBindRelationService} from './ds-dynamic-type-bind-relation.service';
import {FormFieldMetadataValueObject} from '../models/form-field-metadata-value.model';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {FormBuilderService} from '../form-builder.service';
import {getMockFormBuilderService} from '../../../mocks/form-builder-service.mock';
import {Injector} from '@angular/core';

describe('DSDynamicTypeBindRelationService test suite', () => {
  let service: DsDynamicTypeBindRelationService;
  let dynamicFormRelationService: DynamicFormRelationService;
  let injector: Injector;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      providers: [
        { provide: FormBuilderService, useValue: getMockFormBuilderService() },
        { provide: DsDynamicTypeBindRelationService, useClass: DsDynamicTypeBindRelationService },
        { provide: DynamicFormRelationService },
        DISABLED_MATCHER_PROVIDER, HIDDEN_MATCHER_PROVIDER, REQUIRED_MATCHER_PROVIDER
      ]
    }).compileComponents().then();
  });

  beforeEach(inject([DsDynamicTypeBindRelationService, DynamicFormRelationService],
    (relationService: DsDynamicTypeBindRelationService,
     formRelationService: DynamicFormRelationService,
    ) => {
    service = relationService;
    dynamicFormRelationService = formRelationService;
  }));

  describe('Test getTypeBindValue method', () => {
    it('Should get type bind "boundType" from the given metadata object value', () => {
        const mockMetadataValueObject: FormFieldMetadataValueObject = new FormFieldMetadataValueObject(
          'boundType', null, null, 'Bound Type'
        );
        const bindType = service.getTypeBindValue(mockMetadataValueObject);
        expect(bindType).toBe('boundType');
    });
    it('Should get type authority key "bound-auth-key" from the given metadata object value', () => {
      const mockMetadataValueObject: FormFieldMetadataValueObject = new FormFieldMetadataValueObject(
        'boundType', null, 'bound-auth-key', 'Bound Type'
      );
      const bindType = service.getTypeBindValue(mockMetadataValueObject);
      expect(bindType).toBe('bound-auth-key');
    });
    it('Should get passed string returned directly as string passed instead of metadata', () => {
      const bindType = service.getTypeBindValue('rawString');
      expect(bindType).toBe('rawString');
    });
    it('Should get "undefined" returned directly as no object given', () => {
      const bindType = service.getTypeBindValue(undefined);
      expect(bindType).toBeUndefined();
    });
  });

  describe('Test getRelatedFormModel method', () => {
    it('Should get 0 related form models for simple type bind mock data', () => {
      const testModel = MockRelationModel;
      const relatedModels = service.getRelatedFormModel(testModel);
      expect(relatedModels).toHaveSize(0);
    });
    it('Should get 1 related form models for mock relation model data', () => {
      const testModel = mockInputWithTypeBindModel;
      testModel.typeBindRelations = getTypeBindRelations(['boundType']);
      const relatedModels = service.getRelatedFormModel(testModel);
      expect(relatedModels).toHaveSize(1);
    });
  });

  describe('Test matchesCondition method', () => {
    it('Should receive one subscription to dc.type type binding"', () => {
      const testModel = mockInputWithTypeBindModel;
      testModel.typeBindRelations = getTypeBindRelations(['boundType']);
      const dcTypeControl = new FormControl();
      dcTypeControl.setValue('boundType');
      let subscriptions = service.subscribeRelations(testModel, dcTypeControl);
      expect(subscriptions).toHaveSize(1);
    });

    it('Expect hasMatch to be true (ie. this should be hidden)', () => {
      const testModel = mockInputWithTypeBindModel;
      testModel.typeBindRelations = getTypeBindRelations(['boundType']);
      const dcTypeControl = new FormControl();
      dcTypeControl.setValue('boundType');
      testModel.typeBindRelations[0].when[0].value = 'anotherType';
      const relation = dynamicFormRelationService.findRelationByMatcher((testModel as any).typeBindRelations, HIDDEN_MATCHER);
      const matcher = HIDDEN_MATCHER;
      if (relation !== undefined) {
        const hasMatch = service.matchesCondition(relation, matcher);
        matcher.onChange(hasMatch, testModel, dcTypeControl, injector);
        expect(hasMatch).toBeTruthy();
      }
    });

    it('Expect hasMatch to be false (ie. this should NOT be hidden)', () => {
      const testModel = mockInputWithTypeBindModel;
      testModel.typeBindRelations = getTypeBindRelations(['boundType']);
      const dcTypeControl = new FormControl();
      dcTypeControl.setValue('boundType');
      testModel.typeBindRelations[0].when[0].value = 'boundType';
      const relation = dynamicFormRelationService.findRelationByMatcher((testModel as any).typeBindRelations, HIDDEN_MATCHER);
      const matcher = HIDDEN_MATCHER;
      if (relation !== undefined) {
        const hasMatch = service.matchesCondition(relation, matcher);
        matcher.onChange(hasMatch, testModel, dcTypeControl, injector);
        expect(hasMatch).toBeFalsy();
      }
    });

  });

});

function getTypeBindRelations(configuredTypeBindValues: string[]): DynamicFormControlRelation[] {
  const bindValues = [];
  configuredTypeBindValues.forEach((value) => {
    bindValues.push({
      id: 'dc.type',
      value: value
    });
  });
  return [{
    match: MATCH_VISIBLE,
    operator: OR_OPERATOR,
    when: bindValues
  }];
}
