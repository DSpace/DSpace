import {
  DynamicDateControlModel,
  DynamicDatePickerModelConfig,
  DynamicFormControlLayout,
  DynamicFormControlModel,
  DynamicFormControlRelation,
  serializable
} from '@ng-dynamic-forms/core';
import {BehaviorSubject, Subject} from 'rxjs';
import {isEmpty, isNotUndefined} from '../../../../../empty.util';
import {MetadataValue} from '../../../../../../core/shared/metadata.models';

export const DYNAMIC_FORM_CONTROL_TYPE_DSDATEPICKER = 'DATE';

export interface DynamicDsDateControlModelConfig extends DynamicDatePickerModelConfig {
  legend?: string;
  typeBindRelations?: DynamicFormControlRelation[];
}

/**
 * Dynamic Date Picker Model class
 */
export class DynamicDsDatePickerModel extends DynamicDateControlModel {
  @serializable() hiddenUpdates: Subject<boolean>;
  @serializable() typeBindRelations: DynamicFormControlRelation[];
  @serializable() readonly type: string = DYNAMIC_FORM_CONTROL_TYPE_DSDATEPICKER;
  @serializable() metadataValue: MetadataValue;
  malformedDate: boolean;
  legend: string;
  hasLanguages = false;
  repeatable = false;

  constructor(config: DynamicDsDateControlModelConfig, layout?: DynamicFormControlLayout) {
    super(config, layout);
    this.malformedDate = false;
    this.legend = config.legend;
    this.metadataValue = (config as any).metadataValue;
    this.typeBindRelations = config.typeBindRelations ? config.typeBindRelations : [];
    this.hiddenUpdates = new BehaviorSubject<boolean>(this.hidden);

    // This was a subscription, then an async setTimeout, but it seems unnecessary
    const parentModel = this.getRootParent(this);
    if (parentModel && isNotUndefined(parentModel.hidden)) {
      parentModel.hidden = this.hidden;
    }
  }

  private getRootParent(model: any): DynamicFormControlModel {
    if (isEmpty(model) || isEmpty(model.parent)) {
      return model;
    } else {
      return this.getRootParent(model.parent);
    }
  }

}
