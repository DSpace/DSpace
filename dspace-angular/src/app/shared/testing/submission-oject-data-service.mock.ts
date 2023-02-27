export const mockSubmissionObjectDataService = jasmine.createSpyObj('SubmissionObjectDataService', {
  getHrefByID: jasmine.createSpy('getHrefByID'),
  findById: jasmine.createSpy('findById')
});
