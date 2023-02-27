import { DynamicSelectModel } from '@ng-dynamic-forms/core';

import { DsDynamicInputModel } from '../form/builder/ds-dynamic-form-ui/models/ds-dynamic-input.model';
import { DynamicQualdropModel } from '../form/builder/ds-dynamic-form-ui/models/ds-dynamic-qualdrop.model';
import {
  DynamicRowArrayModel,
  DynamicRowArrayModelConfig
} from '../form/builder/ds-dynamic-form-ui/models/ds-dynamic-row-array-model';
import { SubmissionScopeType } from '../../core/submission/submission-scope-type';
import { DynamicRelationGroupModel } from '../form/builder/ds-dynamic-form-ui/models/relation-group/dynamic-relation-group.model';
import { FormFieldModel } from '../form/builder/models/form-field.model';
import { VocabularyOptions } from '../../core/submission/vocabularies/models/vocabulary-options.model';
import { VocabularyEntry } from '../../core/submission/vocabularies/models/vocabulary-entry.model';
import { FormFieldMetadataValueObject } from '../form/builder/models/form-field-metadata-value.model';
import { DynamicRowGroupModel } from '../form/builder/ds-dynamic-form-ui/models/ds-dynamic-row-group-model';
import { FormRowModel } from '../../core/config/models/config-submission-form.model';

export const qualdropSelectConfig = {
  name: 'dc.identifier_QUALDROP_METADATA',
  id: 'dc_identifier_QUALDROP_METADATA',
  readOnly: false,
  disabled: false,
  label: 'Identifiers',
  placeholder: 'Identifiers',
  options: [
    {
      label: 'ISSN',
      value: 'dc.identifier.issn'
    },
    {
      label: 'Other',
      value: 'dc.identifier.other'
    },
    {
      label: 'ISMN',
      value: 'dc.identifier.ismn'
    },
    {
      label: 'Gov\'t Doc #',
      value: 'dc.identifier.govdoc'
    },
    {
      label: 'URI',
      value: 'dc.identifier.uri'
    },
    {
      label: 'ISBN',
      value: 'dc.identifier.isbn'
    }
  ],
  value: 'dc.identifier.issn'
};

export const qualdropInputConfig = {
  name: 'dc.identifier_QUALDROP_VALUE',
  id: 'dc_identifier_QUALDROP_VALUE',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: 'test',
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockQualdropSelectModel = new DynamicSelectModel(qualdropSelectConfig);
export const mockQualdropInputModel = new DsDynamicInputModel(qualdropInputConfig);

export const qualdropConfig = {
  id: 'dc_identifier_QUALDROP_GROUP',
  legend: 'Identifiers',
  readOnly: false,
  group: [mockQualdropSelectModel, mockQualdropInputModel],
  required: false
};

export const MockQualdropModel = new DynamicQualdropModel(qualdropConfig);

const rowArrayQualdropConfig = {
  id: 'row_QUALDROP_GROUP',
  initialCount: 1,
  notRepeatable: true,
  isDraggable: false,
  relationshipConfig: undefined,
  groupFactory: () => {
    return [MockQualdropModel];
  },
  required: false,
  submissionId: '1234',
  metadataKey: 'dc.some.key',
  metadataFields: ['dc.some.key'],
  hasSelectableMetadata: false,
  showButtons: true
} as DynamicRowArrayModelConfig;

export const MockRowArrayQualdropModel: DynamicRowArrayModel = new DynamicRowArrayModel(rowArrayQualdropConfig);

const mockFormRowModel = {
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
    } as FormFieldModel
  ]
} as FormRowModel;

const relationGroupConfig = {
  submissionId: '1234',
  id: 'relationGroup',
  formConfiguration: [mockFormRowModel],
  mandatoryField: 'false',
  relationFields: ['journal', 'issue'],
  scopeUUID: 'scope',
  repeatable: false,
  submissionScope: SubmissionScopeType.WorkspaceItem,
  value: {
    journal: [
      'journal test 1',
      'journal test 2'
    ],
    issue: [
      'issue test 1',
      'issue test 2'
    ],
  },
  metadataFields: [],
  hasSelectableMetadata: false
};

export const MockRelationModel: DynamicRelationGroupModel = new DynamicRelationGroupModel(relationGroupConfig);

export const inputWithLanguageAndAuthorityConfig = {
  vocabularyOptions: new VocabularyOptions('testAuthority', false),
  languageCodes: [
    {
      display: 'English',
      code: 'en_US'
    },
    {
      display: 'Italian',
      code: 'it_IT'
    }
  ],
  language: 'en_US',
  name: 'testWithAuthority',
  id: 'testWithAuthority',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: {
    value: 'testWithLanguageAndAuthority',
    display: 'testWithLanguageAndAuthority',
    authority: 'testWithLanguageAndAuthority',
  },
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithLanguageAndAuthorityModel = new DsDynamicInputModel(inputWithLanguageAndAuthorityConfig);

export const inputWithLanguageConfig = {
  languageCodes: [
    {
      display: 'English',
      code: 'en_US'
    },
    {
      display: 'Italian',
      code: 'it_IT'
    }
  ],
  language: 'en_US',
  name: 'testWithLanguage',
  id: 'testWithLanguage',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: 'testWithLanguage',
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithLanguageModel = new DsDynamicInputModel(inputWithLanguageConfig);

export const inputWithLanguageAndAuthorityArrayConfig = {
  vocabularyOptions: new VocabularyOptions('testAuthority', false),
  languageCodes: [
    {
      display: 'English',
      code: 'en_US'
    },
    {
      display: 'Italian',
      code: 'it_IT'
    }
  ],
  language: 'en_US',
  name: 'testWithLanguageAndAuthorityArray',
  id: 'testWithLanguageAndAuthorityArray',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: [{
    value: 'testLanguageAndAuthorityArray',
    display: 'testLanguageAndAuthorityArray',
    authority: 'testLanguageAndAuthorityArray',
  }],
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithLanguageAndAuthorityArrayModel = new DsDynamicInputModel(inputWithLanguageAndAuthorityArrayConfig);

export const inputWithFormFieldValueConfig = {
  name: 'testWithFormField',
  id: 'testWithFormField',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: new FormFieldMetadataValueObject('testWithFormFieldValue'),
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithFormFieldValueModel = new DsDynamicInputModel(inputWithFormFieldValueConfig);

export const inputWithAuthorityValueConfig = {
  name: 'testWithAuthorityField',
  id: 'testWithAuthorityField',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: Object.assign({}, new VocabularyEntry(), {
    value: 'testWithAuthorityValue',
    authority: 'testWithAuthorityValue',
    display: 'testWithAuthorityValue'
  }),
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithAuthorityValueModel = new DsDynamicInputModel(inputWithAuthorityValueConfig);

export const inputWithObjectValueConfig = {
  name: 'testWithObjectValue',
  id: 'testWithObjectValue',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: { value: 'testWithObjectValue', authority: 'testWithObjectValue', display: 'testWithObjectValue' },
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockInputWithObjectValueModel = new DsDynamicInputModel(inputWithObjectValueConfig);

export const mockRowGroupModel = new DynamicRowGroupModel({
  id: 'mockRowGroupModel',
  group: [mockInputWithFormFieldValueModel],
});

export const fileFormEditInputConfig = {
  name: 'dc.title',
  id: 'dc_title',
  readOnly: false,
  disabled: false,
  repeatable: false,
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false
};

export const mockFileFormEditInputModel = new DsDynamicInputModel(fileFormEditInputConfig);

export const mockFileFormEditRowGroupModel = new DynamicRowGroupModel({
  id: 'mockRowGroupModel',
  group: [mockFileFormEditInputModel]
});

// Mock configuration and model for an input with type binding
export const inputWithTypeBindConfig = {
  name: 'testWithTypeBind',
  id: 'testWithTypeBind',
  readOnly: false,
  disabled: false,
  repeatable: false,
  value: {
    value: 'testWithTypeBind',
    display: 'testWithTypeBind',
    authority: 'bound-auth-key'
  },
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false,
  getTypeBindModel: new DsDynamicInputModel({
      name: 'testWithTypeBind',
      id: 'testWithTypeBind',
      readOnly: false,
      disabled: false,
      repeatable: false,
      value: {
        value: 'testWithTypeBind',
        display: 'testWithTypeBind',
        authority: 'bound-auth-key'
      },
      submissionId: '1234',
      metadataFields: [],
      hasSelectableMetadata: false,
      typeBindRelations: [
        {match: 'VISIBLE', operator: 'OR', when: [{'id': 'dc.type', 'value': 'boundType'}]}
      ]
    }
  )
};

export const mockInputWithTypeBindModel = new DsDynamicInputModel(inputWithAuthorityValueConfig);

export const dcTypeInputConfig = {
  name: 'dc.type',
  id: 'dc_type',
  readOnly: false,
  disabled: false,
  repeatable: false,
  submissionId: '1234',
  metadataFields: [],
  hasSelectableMetadata: false,
  value: {
    value: 'boundType'
  }
};

export const mockDcTypeInputModel = new DsDynamicInputModel(dcTypeInputConfig);
