import { of as observableOf } from 'rxjs';

import { FormService } from '../form/form.service';

/**
 * Mock for [[FormService]]
 */
export function getMockFormService(
  id$: string = 'random_id'
): FormService {
  return jasmine.createSpyObj('FormService', {
    getFormData: jasmine.createSpy('getFormData'),
    initForm: jasmine.createSpy('initForm'),
    removeForm: jasmine.createSpy('removeForm'),
    getForm: observableOf({}),
    getUniqueId: id$,
    resetForm: {},
    validateAllFormFields: jasmine.createSpy('validateAllFormFields'),
    isValid: jasmine.createSpy('isValid'),
    isFormInitialized: observableOf(true),
    addError: jasmine.createSpy('addError'),
    removeError: jasmine.createSpy('removeError'),
  });

}
