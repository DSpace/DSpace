import { TestScheduler } from 'rxjs/testing';
import { getTestScheduler } from 'jasmine-marbles';

import { SubmissionRestService } from './submission-rest.service';
import { RequestService } from '../data/request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { getMockRemoteDataBuildService } from '../../shared/mocks/remote-data-build.service.mock';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import {
  SubmissionDeleteRequest,
  SubmissionPatchRequest,
  SubmissionPostRequest,
  SubmissionRequest
} from '../data/request.models';
import { FormFieldMetadataValueObject } from '../../shared/form/builder/models/form-field-metadata-value.model';

describe('SubmissionRestService test suite', () => {
  let scheduler: TestScheduler;
  let service: SubmissionRestService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let halService: any;

  const resourceEndpointURL = 'https://rest.api/endpoint';
  const resourceEndpoint = 'workspaceitems';
  const resourceScope = '260';
  const body = { test: new FormFieldMetadataValueObject('test') };
  const resourceHref = resourceEndpointURL + '/' + resourceEndpoint + '/' + resourceScope + '?projection=full';
  const timestampResponse = 1545994811992;

  function initTestService() {
    return new SubmissionRestService(
      rdbService,
      requestService,
      halService
    );
  }

  beforeEach(() => {
    requestService = getMockRequestService();
    rdbService = getMockRemoteDataBuildService();
    scheduler = getTestScheduler();
    halService = new HALEndpointServiceStub(resourceEndpointURL);
    service = initTestService();

  });

  describe('deleteById', () => {
    it('should send a new SubmissionDeleteRequest', () => {
      const expected = new SubmissionDeleteRequest(requestService.generateRequestId(), resourceHref);
      scheduler.schedule(() => service.deleteById(resourceScope, resourceEndpoint).subscribe());
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(expected);
    });
  });

  describe('getDataById', () => {
    it('should send a new SubmissionRequest', () => {
      const expected = new SubmissionRequest(requestService.generateRequestId(), resourceHref);
      scheduler.schedule(() => service.getDataById(resourceEndpoint, resourceScope).subscribe());
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(expected);
    });
  });

  describe('postToEndpoint', () => {
    it('should send a new SubmissionPostRequest', () => {
      const expected = new SubmissionPostRequest(requestService.generateRequestId(), resourceHref, body);
      scheduler.schedule(() => service.postToEndpoint(resourceEndpoint, body, resourceScope).subscribe());
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(expected);
    });
  });

  describe('patchToEndpoint', () => {
    it('should send a new SubmissionPatchRequest', () => {
      const expected = new SubmissionPatchRequest(requestService.generateRequestId(), resourceHref, body);
      scheduler.schedule(() => service.patchToEndpoint(resourceEndpoint, body, resourceScope).subscribe());
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(expected);
    });
  });
});
