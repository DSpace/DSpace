import {
  DynamicCheckboxModel,
  DynamicCheckboxModelConfig,
  DynamicFormControlLayout,
  serializable
} from '@ng-dynamic-forms/core';

export const DYNAMIC_FORM_CONTROL_TYPE_CUSTOM_SWITCH = 'CUSTOM_SWITCH';

/**
 * Model class for displaying a custom switch input in a form
 * Functions like a checkbox, but displays a switch instead
 */
export class DynamicCustomSwitchModel extends DynamicCheckboxModel {
  @serializable() readonly type: string = DYNAMIC_FORM_CONTROL_TYPE_CUSTOM_SWITCH;

  constructor(config: DynamicCheckboxModelConfig, layout?: DynamicFormControlLayout) {
    super(config, layout);
  }
}
