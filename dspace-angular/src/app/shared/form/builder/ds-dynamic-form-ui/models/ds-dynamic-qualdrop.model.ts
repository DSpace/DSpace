import {
  DynamicFormControlLayout,
  DynamicFormGroupModel,
  DynamicFormGroupModelConfig,
  serializable
} from '@ng-dynamic-forms/core';
import { DsDynamicInputModel } from './ds-dynamic-input.model';
import { Subject } from 'rxjs';

import { LanguageCode } from '../../models/form-field-language-value.model';

export const QUALDROP_GROUP_SUFFIX = '_QUALDROP_GROUP';
export const QUALDROP_METADATA_SUFFIX = '_QUALDROP_METADATA';
export const QUALDROP_VALUE_SUFFIX = '_QUALDROP_VALUE';

export interface DsDynamicQualdropModelConfig extends DynamicFormGroupModelConfig {
  languageCodes?: LanguageCode[];
  language?: string;
  readOnly: boolean;
  required: boolean;
  hint?: string;
}

export class DynamicQualdropModel extends DynamicFormGroupModel {
  @serializable() private _language: string;
  @serializable() private _languageCodes: LanguageCode[];
  @serializable() languageUpdates: Subject<string>;
  @serializable() hasLanguages = false;
  @serializable() readOnly: boolean;
  @serializable() hint: string;
  @serializable() required: boolean;
  isCustomGroup = true;

  constructor(config: DsDynamicQualdropModelConfig, layout?: DynamicFormControlLayout) {
    super(config, layout);

    this.readOnly = config.readOnly;
    this.required = config.required;
    this.language = config.language;
    this.languageCodes = config.languageCodes;

    this.languageUpdates = new Subject<string>();
    this.languageUpdates.subscribe((lang: string) => {
      this.language = lang;
    });

    this.hint = config.hint;
  }

  get value() {
    return (this.get(1) as DsDynamicInputModel).value;
  }

  get qualdropId(): string {
    return (this.get(0) as DsDynamicInputModel).value.toString();
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
    if (!this.language || this.language === null || this.language === '') {
      this.language = this.languageCodes ? this.languageCodes[0].code : null;
    }
  }

}
