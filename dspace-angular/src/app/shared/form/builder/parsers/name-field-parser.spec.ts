import { FormFieldModel } from '../models/form-field.model';
import { NameFieldParser } from './name-field-parser';
import { DynamicConcatModel } from '../ds-dynamic-form-ui/models/ds-dynamic-concat.model';
import { FormFieldMetadataValueObject } from '../models/form-field-metadata-value.model';
import { ParserOptions } from './parser-options';

describe('NameFieldParser test suite', () => {
  let field1: FormFieldModel;
  let field2: FormFieldModel;
  let field3: FormFieldModel;
  let initFormValues: any = {};

  const submissionId = '1234';
  const parserOptions: ParserOptions = {
    readOnly: false,
    submissionScope: 'testScopeUUID',
    collectionUUID: null,
    typeField: 'dc_type'
  };

  beforeEach(() => {
    field1 = {
      input: {
        type: 'name'
      },
      label: 'Name',
      mandatory: 'false',
      repeatable: false,
      hints: 'Enter the name.',
      selectableMetadata: [
        {
          metadata: 'name',
        }
      ],
      languageCodes: []
    } as FormFieldModel;

    field2 = {
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
    } as FormFieldModel;

    field3 = {
      input: { type: 'onebox' },
      label: 'Title',
      mandatory: 'false',
      repeatable: false,
      hints: 'Enter the name of the events, if any.',
      selectableMetadata: [
        {
          metadata: 'title',
        }
      ],
      languageCodes: []
    } as FormFieldModel;
  });

  it('should init parser properly', () => {
    const parser = new NameFieldParser(submissionId, field1, initFormValues, parserOptions);

    expect(parser instanceof NameFieldParser).toBe(true);
  });

  it('should return a DynamicConcatModel object when repeatable option is false', () => {
    const parser = new NameFieldParser(submissionId, field2, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DynamicConcatModel).toBe(true);
  });

  it('should return a DynamicConcatModel object with the correct separator', () => {
    const parser = new NameFieldParser(submissionId, field2, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect((fieldModel as DynamicConcatModel).separator).toBe(', ');
  });

  it('should set init value properly', () => {
    initFormValues = {
      name: [new FormFieldMetadataValueObject('test, name')],
    };
    const expectedValue = new FormFieldMetadataValueObject('test, name', undefined, undefined, 'test');

    const parser = new NameFieldParser(submissionId, field1, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel.value).toEqual(expectedValue);
  });

});
