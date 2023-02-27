import { ExternalSourceDataService } from './external-source-data.service';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { ExternalSourceEntry } from '../shared/external-source-entry.model';
import { of as observableOf } from 'rxjs';
import { GetRequest } from './request.models';
import { testSearchDataImplementation } from './base/search-data.spec';

describe('ExternalSourceService', () => {
  let service: ExternalSourceDataService;

  let requestService;
  let rdbService;
  let halService;

  const entries = [
    Object.assign(new ExternalSourceEntry(), {
      id: '0001-0001-0001-0001',
      display: 'John Doe',
      value: 'John, Doe',
      metadata: {
        'dc.identifier.uri': [
          {
            value: 'https://orcid.org/0001-0001-0001-0001'
          }
        ]
      }
    }),
    Object.assign(new ExternalSourceEntry(), {
      id: '0001-0001-0001-0002',
      display: 'Sampson Megan',
      value: 'Sampson, Megan',
      metadata: {
        'dc.identifier.uri': [
          {
            value: 'https://orcid.org/0001-0001-0001-0002'
          }
        ]
      }
    })
  ];

  function init() {
    requestService = jasmine.createSpyObj('requestService', {
      generateRequestId: 'request-uuid',
      send: {}
    });
    rdbService = jasmine.createSpyObj('rdbService', {
      buildList: createSuccessfulRemoteDataObject$(createPaginatedList(entries))
    });
    halService = jasmine.createSpyObj('halService', {
      getEndpoint: observableOf('external-sources-REST-endpoint'),
    });
    service = new ExternalSourceDataService(requestService, rdbService, undefined, halService);
  }

  beforeEach(() => {
    init();
  });

  describe('composition', () => {
    const initService = () => new ExternalSourceDataService(null, null, null, null);
    testSearchDataImplementation(initService);
  });

  describe('getExternalSourceEntries', () => {
    let result;

    beforeEach(() => {
      result = service.getExternalSourceEntries('test');
    });

    it('should send a GetRequest', () => {
      expect(requestService.send).toHaveBeenCalledWith(jasmine.any(GetRequest), true);
    });

    it('should return the entries', () => {
      result.subscribe((resultRD) => {
        expect(resultRD.payload.page).toBe(entries);
      });
    });
  });
});
