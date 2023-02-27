import {
  DynamicFormArrayModel,
  DynamicFormArrayModelConfig,
  DynamicFormControlLayout,
  DynamicFormControlRelation,
  serializable
} from '@ng-dynamic-forms/core';
import { RelationshipOptions } from '../../models/relationship-options.model';
import { hasValue } from '../../../../empty.util';

export interface DynamicRowArrayModelConfig extends DynamicFormArrayModelConfig {
  notRepeatable: boolean;
  required: boolean;
  submissionId: string;
  relationshipConfig: RelationshipOptions;
  metadataKey: string;
  metadataFields: string[];
  hasSelectableMetadata: boolean;
  isDraggable: boolean;
  showButtons: boolean;
  typeBindRelations?: DynamicFormControlRelation[];
  isInlineGroupArray?: boolean;
}

export class DynamicRowArrayModel extends DynamicFormArrayModel {
  @serializable() notRepeatable = false;
  @serializable() required = false;
  @serializable() submissionId: string;
  @serializable() relationshipConfig: RelationshipOptions;
  @serializable() metadataKey: string;
  @serializable() metadataFields: string[];
  @serializable() hasSelectableMetadata: boolean;
  @serializable() isDraggable: boolean;
  @serializable() showButtons = true;
  @serializable() typeBindRelations: DynamicFormControlRelation[];
  isRowArray = true;
  isInlineGroupArray = false;

  constructor(config: DynamicRowArrayModelConfig, layout?: DynamicFormControlLayout) {
    super(config, layout);
    if (hasValue(config.notRepeatable)) {
      this.notRepeatable = config.notRepeatable;
    }
    if (hasValue(config.required)) {
      this.required = config.required;
    }
    if (hasValue(config.showButtons)) {
      this.showButtons = config.showButtons;
    }
    this.submissionId = config.submissionId;
    this.relationshipConfig = config.relationshipConfig;
    this.metadataKey = config.metadataKey;
    this.metadataFields = config.metadataFields;
    this.hasSelectableMetadata = config.hasSelectableMetadata;
    this.isDraggable = config.isDraggable;
    this.typeBindRelations = config.typeBindRelations ? config.typeBindRelations : [];
    this.isInlineGroupArray = config.isInlineGroupArray ? config.isInlineGroupArray : false;
  }
}
