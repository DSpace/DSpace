import {
  DynamicFormControlLayout,
  DynamicFormControlRelation,
  DynamicInputModel,
  DynamicInputModelConfig,
  serializable
} from '@ng-dynamic-forms/core';
import {Subject} from 'rxjs';

import { LanguageCode } from '../../models/form-field-language-value.model';
import { VocabularyOptions } from '../../../../../core/submission/vocabularies/models/vocabulary-options.model';
import {hasValue} from '../../../../empty.util';
import { FormFieldMetadataValueObject } from '../../models/form-field-metadata-value.model';
import { RelationshipOptions } from '../../models/relationship-options.model';

export interface DsDynamicInputModelConfig extends DynamicInputModelConfig {
  vocabularyOptions?: VocabularyOptions;
  languageCodes?: LanguageCode[];
  language?: string;
  place?: number;
  value?: any;
  typeBindRelations?: DynamicFormControlRelation[];
  relationship?: RelationshipOptions;
  repeatable: boolean;
  metadataFields: string[];
  submissionId: string;
  hasSelectableMetadata: boolean;
  metadataValue?: FormFieldMetadataValueObject;
  isModelOfInnerForm?: boolean;
  hideErrorMessages?: boolean;
}

export class DsDynamicInputModel extends DynamicInputModel {

  @serializable() vocabularyOptions: VocabularyOptions;
  @serializable() private _languageCodes: LanguageCode[];
  @serializable() private _language: string;
  @serializable() languageUpdates: Subject<string>;
  @serializable() place: number;
  @serializable() typeBindRelations: DynamicFormControlRelation[];
  @serializable() typeBindHidden = false;
  @serializable() relationship?: RelationshipOptions;
  @serializable() repeatable?: boolean;
  @serializable() metadataFields: string[];
  @serializable() submissionId: string;
  @serializable() hasSelectableMetadata: boolean;
  @serializable() metadataValue: FormFieldMetadataValueObject;
  @serializable() isModelOfInnerForm: boolean;
  @serializable() hideErrorMessages?: boolean;


  constructor(config: DsDynamicInputModelConfig, layout?: DynamicFormControlLayout) {
    super(config, layout);
    this.repeatable = config.repeatable;
    this.metadataFields = config.metadataFields;
    this.hint = config.hint;
    this.readOnly = config.readOnly;
    this.value = config.value;
    this.relationship = config.relationship;
    this.submissionId = config.submissionId;
    this.hasSelectableMetadata = config.hasSelectableMetadata;
    this.metadataValue = config.metadataValue;
    this.place = config.place;
    this.isModelOfInnerForm = (hasValue(config.isModelOfInnerForm) ? config.isModelOfInnerForm : false);
    this.hideErrorMessages = config.hideErrorMessages;

    this.language = config.language;
    if (!this.language) {
      // Onebox
      if (config.value instanceof FormFieldMetadataValueObject) {
        this.language = config.value.language;
      } else if (Array.isArray(config.value)) {
        // Tag of Authority
        if (config.value[0].language) {
          this.language = config.value[0].language;
        }
      }
    }
    this.languageCodes = config.languageCodes;

    this.languageUpdates = new Subject<string>();
    this.languageUpdates.subscribe((lang: string) => {
      this.language = lang;
    });

    this.typeBindRelations = config.typeBindRelations ? config.typeBindRelations : [];

    this.vocabularyOptions = config.vocabularyOptions;
  }

  get hasAuthority(): boolean {
    return this.vocabularyOptions && hasValue(this.vocabularyOptions.name);
  }

  get hasLanguages(): boolean {
    return this.languageCodes && this.languageCodes.length > 1;
  }

  get language(): string {
    return this._language;
  }

  set language(language: string) {
    this._language = language;
  }

  get languageCodes(): LanguageCode[] {
    return this._languageCodes;
  }

  set languageCodes(languageCodes: LanguageCode[]) {
    this._languageCodes = languageCodes;
    if (!this.language || this.language === '') {
      this.language = this.languageCodes ? this.languageCodes[0].code : null;
    }
  }
}
