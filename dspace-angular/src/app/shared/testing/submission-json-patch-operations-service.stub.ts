import { SubmissionPatchRequest } from '../../core/data/request.models';

export class SubmissionJsonPatchOperationsServiceStub {
  protected linkPath = 'workspaceitems';
  protected patchRequestConstructor: SubmissionPatchRequest;

  jsonPatchByResourceType = jasmine.createSpy('jsonPatchByResourceType');
  jsonPatchByResourceID = jasmine.createSpy('jsonPatchByResourceID');
  deletePendingJsonPatchOperations = jasmine.createSpy('deletePendingJsonPatchOperations');

}
