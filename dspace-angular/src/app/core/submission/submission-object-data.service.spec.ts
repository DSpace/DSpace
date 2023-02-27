import { SubmissionService } from '../../submission/submission.service';
import { RemoteData } from '../data/remote-data';
import { SubmissionObject } from './models/submission-object.model';
import { SubmissionObjectDataService } from './submission-object-data.service';
import { SubmissionScopeType } from './submission-scope-type';
import { WorkflowItemDataService } from './workflowitem-data.service';
import { WorkspaceitemDataService } from './workspaceitem-data.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';

describe('SubmissionObjectDataService', () => {
  let service: SubmissionObjectDataService;
  let submissionService: SubmissionService;
  let workspaceitemDataService: WorkspaceitemDataService;
  let workflowItemDataService: WorkflowItemDataService;
  let halService: HALEndpointService;

  const submissionId = '1234';
  const wsiResult = 'wsiResult' as any;
  const wfiResult = 'wfiResult' as any;

  beforeEach(() => {
    workspaceitemDataService = jasmine.createSpyObj('WorkspaceitemDataService', {
      findById: wsiResult
    });
    workflowItemDataService = jasmine.createSpyObj('WorkflowItemDataService', {
      findById: wfiResult
    });
    halService = jasmine.createSpyObj('HALEndpointService', {
      getEndpoint: '/workspaceItem'
    });
  });

  describe('findById', () => {
    it('should call SubmissionService.getSubmissionScope to determine the type of submission object', () => {
      submissionService = jasmine.createSpyObj('SubmissionService', {
        getSubmissionScope: {}
      });
      service = new SubmissionObjectDataService(workspaceitemDataService, workflowItemDataService, submissionService, halService);
      service.findById(submissionId);
      expect(submissionService.getSubmissionScope).toHaveBeenCalled();
    });

    describe('when the submission ID refers to a WorkspaceItem', () => {
      beforeEach(() => {
        submissionService = jasmine.createSpyObj('SubmissionService', {
          getSubmissionScope: SubmissionScopeType.WorkspaceItem
        });
        service = new SubmissionObjectDataService(workspaceitemDataService, workflowItemDataService, submissionService, halService);
      });

      it('should forward the result of WorkspaceitemDataService.findByIdAndIDType()', () => {
        const result = service.findById(submissionId);
        expect(workspaceitemDataService.findById).toHaveBeenCalledWith(submissionId, true, true);
        expect(result).toBe(wsiResult);
      });
    });

    describe('when the submission ID refers to a WorkflowItem', () => {
      beforeEach(() => {
        submissionService = jasmine.createSpyObj('SubmissionService', {
          getSubmissionScope: SubmissionScopeType.WorkflowItem
        });
        service = new SubmissionObjectDataService(workspaceitemDataService, workflowItemDataService, submissionService, halService);
      });

      it('should forward the result of WorkflowItemDataService.findByIdAndIDType()', () => {
        const result = service.findById(submissionId);
        expect(workflowItemDataService.findById).toHaveBeenCalledWith(submissionId, true, true);
        expect(result).toBe(wfiResult);
      });
    });

    describe('when the type of submission object is unknown', () => {
      beforeEach(() => {
        submissionService = jasmine.createSpyObj('SubmissionService', {
          getSubmissionScope: 'Something else'
        });
        service = new SubmissionObjectDataService(workspaceitemDataService, workflowItemDataService, submissionService, halService);
      });

      it('shouldn\'t call any data service methods', () => {
        service.findById(submissionId);
        expect(workspaceitemDataService.findById).not.toHaveBeenCalled();
        expect(workflowItemDataService.findById).not.toHaveBeenCalled();
      });

      it('should return a RemoteData containing an error', (done) => {
        const result = service.findById(submissionId);
        result.subscribe((rd: RemoteData<SubmissionObject>) => {
          expect(rd.hasFailed).toBe(true);
          expect(rd.errorMessage).toBeDefined();
          done();
        });
      });
    });

  });
});
