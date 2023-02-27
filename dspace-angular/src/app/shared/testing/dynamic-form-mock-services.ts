export const mockDynamicFormLayoutService = jasmine.createSpyObj('DynamicFormLayoutService', {
  getElementId: jasmine.createSpy('getElementId'),
  getClass: 'class',
});

export const mockDynamicFormValidationService = jasmine.createSpyObj('DynamicFormValidationService', {
  showErrorMessages: jasmine.createSpy('showErrorMessages')
});
