import { FormFieldModel } from '../models/form-field.model';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';
import { ListFieldParser } from './list-field-parser';
import { DynamicListCheckboxGroupModel } from '../ds-dynamic-form-ui/models/list/dynamic-list-checkbox-group.model';
import { DynamicListRadioGroupModel } from '../ds-dynamic-form-ui/models/list/dynamic-list-radio-group.model';
import { ParserOptions } from './parser-options';

describe('ListFieldParser test suite', () => {
  let field: FormFieldModel;
  let initFormValues = {};

  const submissionId = '1234';
  const parserOptions: ParserOptions = {
    readOnly: false,
    submissionScope: 'testScopeUUID',
    collectionUUID: null,
    typeField: 'dc_type'
  };

  beforeEach(() => {
    field = {
      input: {
        type: 'list'
      },
      label: 'Type',
      mandatory: 'false',
      repeatable: true,
      hints: 'Select the type.',
      selectableMetadata: [
        {
          metadata: 'type',
          controlledVocabulary: 'type_programme',
          closed: false
        }
      ],
      languageCodes: []
    } as FormFieldModel;

  });

  it('should init parser properly', () => {
    const parser = new ListFieldParser(submissionId, field, initFormValues, parserOptions);

    expect(parser instanceof ListFieldParser).toBe(true);
  });

  it('should return a DynamicListCheckboxGroupModel object when repeatable option is true', () => {
    const parser = new ListFieldParser(submissionId, field, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DynamicListCheckboxGroupModel).toBe(true);
  });

  it('should return a DynamicListRadioGroupModel object when repeatable option is false', () => {
    field.repeatable = false;
    const parser = new ListFieldParser(submissionId, field, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DynamicListRadioGroupModel).toBe(true);
  });

  it('should set init value properly', () => {
    initFormValues = {
      type: [new FormFieldMetadataValueObject('test type')],
    };
    const expectedValue = [new FormFieldMetadataValueObject('test type')];

    const parser = new ListFieldParser(submissionId, field, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel.value).toEqual(expectedValue);
  });

});
