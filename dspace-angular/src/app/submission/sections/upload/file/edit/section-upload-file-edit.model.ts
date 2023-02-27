import {
  DynamicDatePickerModelConfig,
  DynamicFormArrayModelConfig,
  DynamicFormControlLayout,
  DynamicFormGroupModelConfig,
  DynamicSelectModelConfig,
  MATCH_ENABLED,
  OR_OPERATOR,
} from '@ng-dynamic-forms/core';

export const BITSTREAM_METADATA_FORM_GROUP_CONFIG: DynamicFormGroupModelConfig = {
  id: 'metadata',
  group: []
};
export const BITSTREAM_METADATA_FORM_GROUP_LAYOUT: DynamicFormControlLayout = {
  element: {
    container: 'form-group',
    label: 'col-form-label'
  },
  grid: {
    label: 'col-sm-3'
  }
};
export const BITSTREAM_ACCESS_CONDITION_GROUP_CONFIG: DynamicFormGroupModelConfig = {
  id: 'accessConditionGroup',
  group: []
};

export const BITSTREAM_ACCESS_CONDITION_GROUP_LAYOUT: DynamicFormControlLayout = {
  element: {
    host: 'form-group access-condition-group col',
    container: 'pl-1 pr-1',
    control: 'form-row '
  }
};

export const BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_CONFIG: DynamicFormArrayModelConfig = {
  id: 'accessConditions',
  groupFactory: null,
};
export const BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT: DynamicFormControlLayout = {
  grid: {
    group: 'form-row pt-4',
  }
};

export const BITSTREAM_FORM_ACCESS_CONDITION_TYPE_CONFIG: DynamicSelectModelConfig<any> = {
  id: 'name',
  label: 'submission.sections.upload.form.access-condition-label',
  hint: 'submission.sections.upload.form.access-condition-hint',
  options: []
};
export const BITSTREAM_FORM_ACCESS_CONDITION_TYPE_LAYOUT: DynamicFormControlLayout = {
  element: {
    host: 'col-12',
    label: 'col-form-label name-label'
  }
};

export const BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'startDate',
  label: 'submission.sections.upload.form.from-label',
  hint: 'submission.sections.upload.form.from-hint',
  placeholder: 'submission.sections.upload.form.from-placeholder',
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
    required: 'submission.sections.upload.form.date-required-from'
  }
};
export const BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    label: 'col-form-label'
  },
  grid: {
    host: 'col-6'
  }
};

export const BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_CONFIG: DynamicDatePickerModelConfig = {
  id: 'endDate',
  label: 'submission.sections.upload.form.until-label',
  hint: 'submission.sections.upload.form.until-hint',
  placeholder: 'submission.sections.upload.form.until-placeholder',
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
    required: 'submission.sections.upload.form.date-required-until'
  }
};
export const BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_LAYOUT: DynamicFormControlLayout = {
  element: {
    label: 'col-form-label'
  },
  grid: {
    host: 'col-6'
  }
};
