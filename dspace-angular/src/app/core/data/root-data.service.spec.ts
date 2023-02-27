import { RootDataService } from './root-data.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { Observable, of } from 'rxjs';
import { RemoteData } from './remote-data';
import { Root } from './root.model';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { cold } from 'jasmine-marbles';

describe('RootDataService', () => {
  let service: RootDataService;
  let halService: HALEndpointService;
  let restService;
  let rootEndpoint;
  let findByHrefSpy;

  beforeEach(() => {
    rootEndpoint = 'root-endpoint';
    halService = jasmine.createSpyObj('halService', {
      getRootHref: rootEndpoint,
    });
    restService = jasmine.createSpyObj('halService', {
      get: jasmine.createSpy('get'),
    });
    service = new RootDataService(null, null, null, halService, restService);

    findByHrefSpy = spyOn(service as any, 'findByHref');
    findByHrefSpy.and.returnValue(createSuccessfulRemoteDataObject$({}));
  });

  describe('findRoot', () => {
    let result$: Observable<RemoteData<Root>>;

    beforeEach(() => {
      result$ = service.findRoot();
    });

    it('should call findByHref using the root endpoint', (done) => {
      result$.subscribe(() => {
        expect(findByHrefSpy).toHaveBeenCalledWith(rootEndpoint, true, true);
        done();
      });
    });
  });

  describe('checkServerAvailability', () => {
    let result$: Observable<boolean>;

    it('should return observable of true when root endpoint is available', () => {
      const mockResponse = {
        statusCode: 200,
        statusText: 'OK'
      } as RawRestResponse;

      restService.get.and.returnValue(of(mockResponse));
      result$ = service.checkServerAvailability();

      expect(result$).toBeObservable(cold('(a|)', {
        a: true
      }));
    });

    it('should return observable of false when root endpoint is not available', () => {
      const mockResponse = {
        statusCode: 500,
        statusText: 'Internal Server Error'
      } as RawRestResponse;

      restService.get.and.returnValue(of(mockResponse));
      result$ = service.checkServerAvailability();

      expect(result$).toBeObservable(cold('(a|)', {
        a: false
      }));
    });

  });
});
