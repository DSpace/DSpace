import { Store } from '@ngrx/store';

import { getTestScheduler } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';

import { HALEndpointService } from '../shared/hal-endpoint.service';
import { SubmissionJsonPatchOperationsService } from './submission-json-patch-operations.service';
import { RequestService } from '../data/request.service';
import { SubmissionPatchRequest } from '../data/request.models';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { CoreState } from '../core-state.model';

describe('SubmissionJsonPatchOperationsService', () => {
  let scheduler: TestScheduler;
  let service: SubmissionJsonPatchOperationsService;
  const requestService = {} as RequestService;
  const store = {} as Store<CoreState>;
  const rdbService = {} as RemoteDataBuildService;
  const halEndpointService = {} as HALEndpointService;

  function initTestService() {
    return new SubmissionJsonPatchOperationsService(
      requestService,
      store,
      rdbService,
      halEndpointService
    );
  }

  beforeEach(() => {
    scheduler = getTestScheduler();
    service = initTestService();
  });

  it('should instantiate SubmissionJsonPatchOperationsService properly', () => {
    expect(service).toBeDefined();
    expect((service as any).patchRequestConstructor).toEqual(SubmissionPatchRequest);
  });

});
