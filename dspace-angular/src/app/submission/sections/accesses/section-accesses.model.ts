import {
  DynamicDatePickerModelConfig,
  DynamicFormArrayModelConfig,
  DynamicFormControlLayout,
  DynamicFormGroupModelConfig,
  DynamicSelectModelConfig,
  MATCH_ENABLED,
  OR_OPERATOR,
} from '@ng-dynamic-forms/core';
import { DynamicCheckboxModelConfig } from '@ng-dynamic-forms/core/lib/model/checkbox/dynamic-checkbox.model';


export const ACCESS_FORM_CHECKBOX_CONFIG: DynamicCheckboxModelConfig = {
  id: 'discoverable',
  name: 'discoverable'
};

export const ACCESS_FORM_CHECKBOX_LAYOUT = {

  element: {
    container: 'custom-control custom-checkbox pl-1',
    control: 'custom-control-input',
    label: 'custom-control-label pt-1'
  }
};

export const ACCESS_CONDITION_GROUP_CONFIG: DynamicFormGroupModelConfig = {
  id: 'accessConditionGroup',
  group: []
};

export const ACCESS_CONDITION_GROUP_LAYOUT: DynamicFormControlLayout = {
  element: {
    host: 'form-group access-condition-group col',
    container: 'pl-1 pr-1',
    control: 'form-row '
  }
};

export const ACCESS_CONDITIONS_FORM_ARRAY_CONFIG: DynamicFormArrayModelConfig = {
  id: 'accessCondition',
  groupFactory: null,
};
export const ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT: DynamicFormControlLayout = {
  grid: {
    group: 'form-row pt-4',
  }
};

export const FORM_ACCESS_CONDITION_TYPE_CONFIG: DynamicSelectModelConfig<any> = {
  id: 'name',
  label: 'submission.sections.accesses.form.access-condition-label',
  hint: 'submission.sections.accesses.form.access-condition-hint',
  options: []
};
export const FORM_ACCESS_CONDITION_TYPE_LAYOUT: DynamicFormControlLayout = {
  element: {
    host: 'col-12',
    label: 'col-form-label name-label'
  }
};

export const FORM_ACCESS_CONDITION_START_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'startDate',
  label: 'submission.sections.accesses.form.from-label',
  hint: 'submission.sections.accesses.form.from-hint',
  placeholder: 'submission.sections.accesses.form.from-placeholder',
  inline: false,
  toggleIcon: 'far fa-calendar-alt',
  relations: [
    {
      match: MATCH_ENABLED,
      operator: OR_OPERATOR,
      when: []
    }
  ],
  required: true,
  validators: {
    required: null
  },
  errorMessages: {
    required: 'submission.sections.accesses.form.date-required-from'
  }
};
export const FORM_ACCESS_CONDITION_START_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    label: 'col-form-label'
  },
  grid: {
    host: 'col-6'
  }
};

export const FORM_ACCESS_CONDITION_END_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'endDate',
  label: 'submission.sections.accesses.form.until-label',
  hint: 'submission.sections.accesses.form.until-hint',
  placeholder: 'submission.sections.accesses.form.until-placeholder',
  inline: false,
  toggleIcon: 'far fa-calendar-alt',
  relations: [
    {
      match: MATCH_ENABLED,
      operator: OR_OPERATOR,
      when: []
    }
  ],
  required: true,
  validators: {
    required: null
  },
  errorMessages: {
    required: 'submission.sections.accesses.form.date-required-until'
  }
};
export const FORM_ACCESS_CONDITION_END_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    label: 'col-form-label'
  },
  grid: {
    host: 'col-6'
  }
};
