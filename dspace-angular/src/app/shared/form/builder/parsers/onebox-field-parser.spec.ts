import { FormFieldModel } from '../models/form-field.model';
import { OneboxFieldParser } from './onebox-field-parser';
import { DynamicQualdropModel } from '../ds-dynamic-form-ui/models/ds-dynamic-qualdrop.model';
import { DynamicOneboxModel } from '../ds-dynamic-form-ui/models/onebox/dynamic-onebox.model';
import { DsDynamicInputModel } from '../ds-dynamic-form-ui/models/ds-dynamic-input.model';
import { ParserOptions } from './parser-options';
import { FieldParser } from './field-parser';

describe('OneboxFieldParser test suite', () => {
  let field1: FormFieldModel;
  let field2: FormFieldModel;
  let field3: FormFieldModel;

  const submissionId = '1234';
  const initFormValues = {};
  const parserOptions: ParserOptions = {
    readOnly: false,
    submissionScope: 'testScopeUUID',
    collectionUUID: null,
    typeField: 'dc_type'
  };

  beforeEach(() => {
    field1 = {
      input: { type: 'onebox' },
      label: 'Title',
      mandatory: 'false',
      repeatable: false,
      hints: 'Enter the name of the events, if any.',
      selectableMetadata: [
        {
          metadata: 'title',
          controlledVocabulary: 'EVENTAuthority',
          closed: false
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
    const parser = new OneboxFieldParser(submissionId, field1, initFormValues, parserOptions);

    expect(parser instanceof OneboxFieldParser).toBe(true);
  });

  it('should return a DynamicQualdropModel object when selectableMetadata is multiple', () => {
    const parser = new OneboxFieldParser(submissionId, field2, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DynamicQualdropModel).toBe(true);
  });

  it('should return a DsDynamicInputModel object when selectableMetadata is not multiple', () => {
    const parser = new OneboxFieldParser(submissionId, field3, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DsDynamicInputModel).toBe(true);
  });

  it('should return a DynamicOneboxModel object when selectableMetadata has authority', () => {
    const parser = new OneboxFieldParser(submissionId, field1, initFormValues, parserOptions);

    const fieldModel = parser.parse();

    expect(fieldModel instanceof DynamicOneboxModel).toBe(true);
  });

  describe('should handle a DynamicOneboxModel with regex', () => {
    let regexField: FormFieldModel;
    let parser: FieldParser;
    let fieldModel: any;

    beforeEach(() => {
      regexField = {
        input: { type: 'onebox', regex: '/[a-z]+/mi' },
        label: 'Title',
        mandatory: 'false',
        repeatable: false,
        hints: 'Enter the name of the events, if any.',
        selectableMetadata: [
          {
            metadata: 'title',
            controlledVocabulary: 'EVENTAuthority',
            closed: false
          }
        ],
        languageCodes: []
      } as FormFieldModel;

      parser = new OneboxFieldParser(submissionId, regexField, initFormValues, parserOptions);
      fieldModel = parser.parse();
    });

    it('should have initialized pattern validator', () => {
      expect(fieldModel instanceof DynamicOneboxModel).toBe(true);
      expect(fieldModel.validators).not.toBeNull();
      expect(fieldModel.validators.pattern).not.toBeNull();
    });

    it('should mark valid not case sensitive basic characters regex in multiline', () => {
      let pattern = fieldModel.validators.pattern as RegExp;
      expect(pattern.test('HELLO')).toBe(true);
      expect(pattern.test('hello')).toBe(true);
      expect(pattern.test('hello\nhello\nhello')).toBe(true);
      expect(pattern.test('HeLlO')).toBe(true);
    });

    it('should be invalid for non-basic alphabet characters', () => {
      let pattern = fieldModel.validators.pattern as RegExp;
      expect(pattern.test('12345')).toBe(false);
      expect(pattern.test('àèìòùáéíóú')).toBe(false);
    });
  });

});
