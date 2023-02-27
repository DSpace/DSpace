
export const SECTION_LICENSE_FORM_LAYOUT = {

  granted: {
    element: {
      container: 'custom-control custom-checkbox pl-1',
      control: 'custom-control-input',
      label: 'custom-control-label pt-1'
    }
  }
};

export const SECTION_LICENSE_FORM_MODEL = [
  {
    id: 'granted',
    label: 'submission.sections.license.granted-label',
    required: true,
    value: false,
    validators: {
      required: null
    },
    errorMessages: {
      required: 'submission.sections.license.required',
      notgranted: 'submission.sections.license.notgranted'
    },
    type: 'CHECKBOX',
  }
];
