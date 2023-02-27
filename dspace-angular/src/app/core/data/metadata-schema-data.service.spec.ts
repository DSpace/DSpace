import { RequestService } from './request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { MetadataSchemaDataService } from './metadata-schema-data.service';
import { of as observableOf } from 'rxjs';
import { RestResponse } from '../cache/response.models';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { MetadataSchema } from '../metadata/metadata-schema.model';
import { CreateRequest, PutRequest } from './request.models';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { getMockRemoteDataBuildService } from '../../shared/mocks/remote-data-build.service.mock';
import { testFindAllDataImplementation } from './base/find-all-data.spec';
import { testDeleteDataImplementation } from './base/delete-data.spec';

describe('MetadataSchemaDataService', () => {
  let metadataSchemaService: MetadataSchemaDataService;
  let requestService: RequestService;
  let halService: HALEndpointService;
  let notificationsService: NotificationsService;
  let rdbService: RemoteDataBuildService;

  const endpoint = 'api/metadataschema/endpoint';

  function init() {
    requestService = jasmine.createSpyObj('requestService', {
      generateRequestId: '34cfed7c-f597-49ef-9cbe-ea351f0023c2',
      send: {},
      getByUUID: observableOf({ response: new RestResponse(true, 200, 'OK') }),
      removeByHrefSubstring: {},
    });
    halService = Object.assign(new HALEndpointServiceStub(endpoint));
    notificationsService = jasmine.createSpyObj('notificationsService', {
      error: {},
    });
    rdbService = getMockRemoteDataBuildService();
    metadataSchemaService = new MetadataSchemaDataService(
      requestService,
      rdbService,
      null,
      halService,
      notificationsService,
    );
  }

  beforeEach(() => {
    init();
  });

  describe('composition', () => {
    const initService = () => new MetadataSchemaDataService(null, null, null, null, null);

    testFindAllDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });

  describe('createOrUpdateMetadataSchema', () => {
    let schema: MetadataSchema;

    beforeEach(() => {
      schema = Object.assign(new MetadataSchema(), {
        prefix: 'dc',
        namespace: 'namespace',
        _links: {
          self: { href: 'selflink' }
        }
      });
    });

    describe('called with a new metadata schema', () => {
      it('should send a CreateRequest', (done) => {
        metadataSchemaService.createOrUpdateMetadataSchema(schema).subscribe(() => {
          expect(requestService.send).toHaveBeenCalledWith(jasmine.any(CreateRequest));
          done();
        });
      });
    });

    describe('called with an existing metadata schema', () => {
      beforeEach(() => {
        schema = Object.assign(schema, {
          id: 'id-of-existing-schema'
        });
      });

      it('should send a PutRequest', (done) => {
        metadataSchemaService.createOrUpdateMetadataSchema(schema).subscribe(() => {
          expect(requestService.send).toHaveBeenCalledWith(jasmine.any(PutRequest));
          done();
        });
      });
    });
  });

  describe('clearRequests', () => {
    it('should remove requests on the data service\'s endpoint', (done) => {
      metadataSchemaService.clearRequests().subscribe(() => {
        expect(requestService.removeByHrefSubstring).toHaveBeenCalledWith(`${endpoint}/${(metadataSchemaService as any).linkPath}`);
        done();
      });
    });
  });
});
