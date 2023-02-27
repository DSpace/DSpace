import { cold, hot } from 'jasmine-marbles';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from './hal-endpoint.service';
import { EndpointMapRequest } from '../data/request.models';
import { combineLatest as observableCombineLatest, of as observableOf } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';

describe('HALEndpointService', () => {
  let service: HALEndpointService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let envConfig;
  const endpointMap = {
    test: {
      href: 'https://rest.api/test'
    },
    foo: {
      href: 'https://rest.api/foo'
    },
    bar: {
      href: 'https://rest.api/bar'
    },
    endpoint: {
      href: 'https://rest.api/endpoint'
    },
    link: {
      href: 'https://rest.api/link'
    },
    another: {
      href: 'https://rest.api/another'
    },
  };
  const start = 'http://start.com';
  const one = 'http://one.com';
  const two = 'http://two.com';
  const endpointMaps = {
    [start]: {
      one: {
        href: one
      },
      two: {
        href: 'empty'
      },
      endpoint: {
        href: 'https://rest.api/endpoint'
      },
      link: {
        href: 'https://rest.api/link'
      },
      another: {
        href: 'https://rest.api/another'
      },
    },
    [one]: {
      one: {
        href: 'empty',
      },
      two: {
        href: two,
      },
      bar: {
        href: 'https://rest.api/bar',
      }
    }
  };
  const linkPath = 'test';

  beforeEach(() => {
    requestService = getMockRequestService();
    rdbService = jasmine.createSpyObj('rdbService', {
      buildFromHref: createSuccessfulRemoteDataObject$({
        _links: endpointMap
      })
    });

    envConfig = {
      rest: { baseUrl: 'https://rest.api/' }
    } as any;

    service = new HALEndpointService(
      requestService,
      rdbService
    );
  });

  describe('getRootEndpointMap', () => {
    it('should send a new EndpointMapRequest', () => {
      (service as any).getRootEndpointMap();
      const expected = new EndpointMapRequest(requestService.generateRequestId(), `${environment.rest.baseUrl}/api`);
      expect(requestService.send).toHaveBeenCalledWith(expected, true);
    });

    it('should return an Observable of the endpoint map', (done) => {
      (service as any).getRootEndpointMap().subscribe((result) => {
        expect(result).toEqual(endpointMap);
        done();
      });
    });

  });

  describe('getEndpoint', () => {

    beforeEach(() => {
      envConfig = {
        rest: { baseUrl: 'https://rest.api/' }
      } as any;
    });

    it(`should return the endpoint URL for the service's linkPath`, () => {
      spyOn(service as any, 'getEndpointAt').and
        .returnValue(hot('a-', { a: 'https://rest.api/test' }));
      const result = service.getEndpoint(linkPath);

      const expected = cold('(b|)', { b: endpointMap.test.href });
      expect(result).toBeObservable(expected);
    });

    it('should return undefined for a linkPath that isn\'t in the endpoint map', () => {
      spyOn(service as any, 'getEndpointAt').and
        .returnValue(hot('a-', { a: undefined }));
      const result = service.getEndpoint('unknown');
      const expected = cold('(b|)', { b: undefined });
      expect(result).toBeObservable(expected);
    });
  });

  describe('getEndpointAt', () => {
    it('should throw an error when the list of hal endpoint names is empty', () => {
      const endpointAtWithoutEndpointNames = () => {
        (service as any).getEndpointAt('');
      };
      expect(endpointAtWithoutEndpointNames).toThrow();
    });

    it('should be at least called as many times as the length of halNames', () => {
      spyOn(service as any, 'getEndpointMapAt').and.returnValue(observableOf(endpointMap));
      spyOn((service as any), 'getEndpointAt').and.callThrough();

      (service as any).getEndpointAt('', 'endpoint').subscribe();

      expect((service as any).getEndpointAt.calls.count()).toEqual(1);

      (service as any).getEndpointAt.calls.reset();

      (service as any).getEndpointAt('', 'endpoint', 'another').subscribe();

      expect((service as any).getEndpointAt.calls.count()).toBeGreaterThanOrEqual(2);

      (service as any).getEndpointAt.calls.reset();

      (service as any).getEndpointAt('', 'endpoint', 'another', 'foo', 'bar', 'test').subscribe();

      expect((service as any).getEndpointAt.calls.count()).toBeGreaterThanOrEqual(5);
    });

    it('should return the correct endpoint', (done) => {
      spyOn(service as any, 'getEndpointMapAt').and.callFake((param) => {
        return observableOf(endpointMaps[param]);
      });

      observableCombineLatest<string[]>([
        (service as any).getEndpointAt(start, 'one'),
        (service as any).getEndpointAt(start, 'one', 'two'),
      ]).subscribe(([endpoint1, endpoint2]) => {
        expect(endpoint1).toEqual(one);
        expect(endpoint2).toEqual(two);
        done();
      });
    });
  });

  describe('isEnabledOnRestApi', () => {
    beforeEach(() => {
      service = new HALEndpointService(
        requestService,
        rdbService
      );

    });

    it('should return undefined as long as getRootEndpointMap hasn\'t fired', () => {
      spyOn(service as any, 'getRootEndpointMap').and
        .returnValue(hot('----'));

      const result = service.isEnabledOnRestApi(linkPath);
      const expected = cold('b---', { b: undefined });
      expect(result).toBeObservable(expected);
    });

    it('should return true if the service\'s linkPath is in the endpoint map', () => {
      spyOn(service as any, 'getRootEndpointMap').and
        .returnValue(hot('--a-', { a: endpointMap }));
      const result = service.isEnabledOnRestApi(linkPath);
      const expected = cold('b-c-', { b: undefined, c: true });
      expect(result).toBeObservable(expected);
    });

    it('should return false if the service\'s linkPath isn\'t in the endpoint map', () => {
      spyOn(service as any, 'getRootEndpointMap').and
        .returnValue(hot('--a-', { a: endpointMap }));

      const result = service.isEnabledOnRestApi('unknown');
      const expected = cold('b-c-', { b: undefined, c: false });
      expect(result).toBeObservable(expected);
    });

  });

});
