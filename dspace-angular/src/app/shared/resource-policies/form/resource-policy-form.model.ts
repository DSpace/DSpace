import {
  DynamicDatePickerModelConfig,
  DynamicFormControlLayout,
  DynamicFormGroupModelConfig,
  DynamicFormOptionConfig,
  DynamicSelectModelConfig,
} from '@ng-dynamic-forms/core';

import { DsDynamicInputModelConfig } from '../../form/builder/ds-dynamic-form-ui/models/ds-dynamic-input.model';
import { DsDynamicTextAreaModelConfig } from '../../form/builder/ds-dynamic-form-ui/models/ds-dynamic-textarea.model';
import { PolicyType } from '../../../core/resource-policy/models/policy-type.model';
import { ActionType } from '../../../core/resource-policy/models/action-type.model';

const policyTypeList: DynamicFormOptionConfig<any>[] = [
  {
    label: PolicyType.TYPE_SUBMISSION,
    value: PolicyType.TYPE_SUBMISSION
  },
  {
    label: PolicyType.TYPE_WORKFLOW,
    value: PolicyType.TYPE_WORKFLOW
  },
  {
    label: PolicyType.TYPE_INHERITED,
    value: PolicyType.TYPE_INHERITED
  },
  {
    label: PolicyType.TYPE_CUSTOM,
    value: PolicyType.TYPE_CUSTOM
  },
];

const policyActionList: DynamicFormOptionConfig<any>[] = [
  {
    label: ActionType.READ.toString(),
    value: ActionType.READ
  },
  {
    label: ActionType.WRITE.toString(),
    value: ActionType.WRITE
  },
  {
    label: ActionType.REMOVE.toString(),
    value: ActionType.REMOVE
  },
  {
    label: ActionType.ADMIN.toString(),
    value: ActionType.ADMIN
  },
  {
    label: ActionType.DELETE.toString(),
    value: ActionType.DELETE
  },
  {
    label: ActionType.WITHDRAWN_READ.toString(),
    value: ActionType.WITHDRAWN_READ
  },
  {
    label: ActionType.DEFAULT_BITSTREAM_READ.toString(),
    value: ActionType.DEFAULT_BITSTREAM_READ
  },
  {
    label: ActionType.DEFAULT_ITEM_READ.toString(),
    value: ActionType.DEFAULT_ITEM_READ
  }
];

export const RESOURCE_POLICY_FORM_NAME_CONFIG: DsDynamicInputModelConfig = {
  id: 'name',
  label: 'resource-policies.form.name.label',
  metadataFields: [],
  repeatable: false,
  submissionId: '',
  hasSelectableMetadata: false
};

export const RESOURCE_POLICY_FORM_DESCRIPTION_CONFIG: DsDynamicTextAreaModelConfig = {
  id: 'description',
  label: 'resource-policies.form.description.label',
  metadataFields: [],
  repeatable: false,
  rows: 10,
  submissionId: '',
  hasSelectableMetadata: false
};

export const RESOURCE_POLICY_FORM_POLICY_TYPE_CONFIG: DynamicSelectModelConfig<any> = {
  id: 'policyType',
  label: 'resource-policies.form.policy-type.label',
  options: policyTypeList,
  required: true,
  validators: {
    required: null
  },
  errorMessages: {
    required: 'resource-policies.form.policy-type.required'
  }
};

export const RESOURCE_POLICY_FORM_ACTION_TYPE_CONFIG: DynamicSelectModelConfig<any> = {
  id: 'action',
  label: 'resource-policies.form.action-type.label',
  options: policyActionList,
  required: true,
  validators: {
    required: null
  },
  errorMessages: {
    required: 'resource-policies.form.action-type.required'
  }
};

export const RESOURCE_POLICY_FORM_DATE_GROUP_CONFIG: DynamicFormGroupModelConfig = {
  id: 'date',
  group: []
};
export const RESOURCE_POLICY_FORM_DATE_GROUP_LAYOUT: DynamicFormControlLayout = {
  element: {
    control: 'form-row',
  }
};

export const RESOURCE_POLICY_FORM_START_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'start',
  label: 'resource-policies.form.date.start.label',
  placeholder: 'resource-policies.form.date.start.label',
  inline: false,
  toggleIcon: 'far fa-calendar-alt'
};

export const RESOURCE_POLICY_FORM_START_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    container: 'p-0',
    label: 'col-form-label'
  },
  grid: {
    host: 'col-md-6'
  }
};

export const RESOURCE_POLICY_FORM_END_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'end',
  label: 'resource-policies.form.date.end.label',
  placeholder: 'resource-policies.form.date.end.label',
  inline: false,
  toggleIcon: 'far fa-calendar-alt'
};
export const RESOURCE_POLICY_FORM_END_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    container: 'p-0',
    label: 'col-form-label'
  },
  grid: {
    host: 'col-md-6'
  }
};
