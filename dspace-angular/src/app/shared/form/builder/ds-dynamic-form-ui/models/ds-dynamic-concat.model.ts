import {
  DynamicFormControlLayout,
  DynamicFormControlRelation,
  DynamicFormGroupModel,
  DynamicFormGroupModelConfig,
  serializable
} from '@ng-dynamic-forms/core';

import { Subject } from 'rxjs';

import { hasNoValue, isNotEmpty } from '../../../../empty.util';
import { DsDynamicInputModel } from './ds-dynamic-input.model';
import { FormFieldMetadataValueObject } from '../../models/form-field-metadata-value.model';
import { RelationshipOptions } from '../../models/relationship-options.model';
import { MetadataValue } from '../../../../../core/shared/metadata.models';

export const CONCAT_GROUP_SUFFIX = '_CONCAT_GROUP';
export const CONCAT_FIRST_INPUT_SUFFIX = '_CONCAT_FIRST_INPUT';
export const CONCAT_SECOND_INPUT_SUFFIX = '_CONCAT_SECOND_INPUT';

export interface DynamicConcatModelConfig extends DynamicFormGroupModelConfig {
  separator: string;
  value?: any;
  hint?: string;
  typeBindRelations?: DynamicFormControlRelation[];
  relationship?: RelationshipOptions;
  repeatable: boolean;
  required: boolean;
  metadataFields: string[];
  submissionId: string;
  hasSelectableMetadata: boolean;
  metadataValue?: MetadataValue;
}

export class DynamicConcatModel extends DynamicFormGroupModel {

  @serializable() separator: string;
  @serializable() hasLanguages = false;
  @serializable() typeBindRelations: DynamicFormControlRelation[];
  @serializable() typeBindHidden = false;
  @serializable() relationship?: RelationshipOptions;
  @serializable() repeatable?: boolean;
  @serializable() required?: boolean;
  @serializable() hint?: string;
  @serializable() metadataFields: string[];
  @serializable() submissionId: string;
  @serializable() hasSelectableMetadata: boolean;
  @serializable() metadataValue: MetadataValue;

  isCustomGroup = true;
  valueUpdates: Subject<string>;

  constructor(config: DynamicConcatModelConfig, layout?: DynamicFormControlLayout) {

    super(config, layout);
    this.separator = config.separator + ' ';
    this.relationship = config.relationship;
    this.repeatable = config.repeatable;
    this.required = config.required;
    this.hint = config.hint;
    this.metadataFields = config.metadataFields;
    this.submissionId = config.submissionId;
    this.hasSelectableMetadata = config.hasSelectableMetadata;
    this.metadataValue = config.metadataValue;
    this.valueUpdates = new Subject<string>();
    this.valueUpdates.subscribe((value: string) => this.value = value);
    this.typeBindRelations = config.typeBindRelations ? config.typeBindRelations : [];
  }

  get value() {
    const [firstValue, secondValue] = this.group.map((inputModel: DsDynamicInputModel) =>
      (typeof inputModel.value === 'string') ?
        Object.assign(new FormFieldMetadataValueObject(), { value: inputModel.value, display: inputModel.value }) :
        (inputModel.value as any));
    if (isNotEmpty(firstValue) && isNotEmpty(firstValue.value) && isNotEmpty(secondValue) && isNotEmpty(secondValue.value)) {
      return Object.assign(new FormFieldMetadataValueObject(), firstValue, { value: firstValue.value + this.separator + secondValue.value });
    } else if (isNotEmpty(firstValue) && isNotEmpty(firstValue.value)) {
      return Object.assign(new FormFieldMetadataValueObject(), firstValue);
    } else if (isNotEmpty(secondValue) && isNotEmpty(secondValue.value)) {
      return Object.assign(new FormFieldMetadataValueObject(), secondValue);
    } else {
      return null;
    }
  }

  set value(value: string | FormFieldMetadataValueObject) {
    let values;
    let tempValue: string;

    if (typeof value === 'string') {
      tempValue = value;
    } else {
      tempValue = value.value;
    }
    if (hasNoValue(tempValue)) {
      tempValue = '';
    }
    values = [...tempValue.split(this.separator), null].map((v) =>
      Object.assign(new FormFieldMetadataValueObject(), value, { display: v, value: v }));

    if (values[0].value) {
      (this.get(0) as DsDynamicInputModel).value = values[0];
    } else {
      (this.get(0) as DsDynamicInputModel).value = undefined;
    }
    if (values[1].value) {
      (this.get(1) as DsDynamicInputModel).value = values[1];
    } else {
      (this.get(1) as DsDynamicInputModel).value = undefined;
    }
  }

}
