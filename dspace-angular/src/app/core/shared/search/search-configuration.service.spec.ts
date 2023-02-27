/* eslint-disable no-empty, @typescript-eslint/no-empty-function */
import { SearchConfigurationService } from './search-configuration.service';
import { ActivatedRouteStub } from '../../../shared/testing/active-router.stub';
import { PaginationComponentOptions } from '../../../shared/pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../cache/models/sort-options.model';
import { PaginatedSearchOptions } from '../../../shared/search/models/paginated-search-options.model';
import { SearchFilter } from '../../../shared/search/models/search-filter.model';
import { combineLatest as observableCombineLatest, Observable, of as observableOf } from 'rxjs';
import { PaginationServiceStub } from '../../../shared/testing/pagination-service.stub';
import { map } from 'rxjs/operators';
import { RemoteData } from '../../data/remote-data';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { getMockRequestService } from '../../../shared/mocks/request.service.mock';
import { RequestEntry } from '../../data/request-entry.model';
import { SearchObjects } from '../../../shared/search/models/search-objects.model';

describe('SearchConfigurationService', () => {
  let service: SearchConfigurationService;
  const value1 = 'random value';
  const prefixFilter = {
    'f.author': ['another value'],
    'f.date.min': ['2013'],
    'f.date.max': ['2018']
  };
  const defaults = new PaginatedSearchOptions({
    pagination: Object.assign(new PaginationComponentOptions(), { id: 'page-id', currentPage: 1, pageSize: 20 }),
    sort: new SortOptions('score', SortDirection.DESC),
    configuration: 'default',
    query: '',
    scope: ''
  });

  const backendFilters = [
    new SearchFilter('f.author', ['another value']),
    new SearchFilter('f.date', ['[2013 TO 2018]'], 'equals')
  ];

  const routeService = jasmine.createSpyObj('RouteService', {
    getQueryParameterValue: observableOf(value1),
    getQueryParamsWithPrefix: observableOf(prefixFilter),
    getRouteParameterValue: observableOf('')
  });

  const paginationService = new PaginationServiceStub();


  const activatedRoute: any = new ActivatedRouteStub();
  const linkService: any = {};
  const requestService: any = getMockRequestService();
  const halService: any = {
    getEndpoint: () => {
    }
  };

  const rdb: any = {
    toRemoteDataObservable: (requestEntryObs: Observable<RequestEntry>, payloadObs: Observable<any>) => {
      return observableCombineLatest([requestEntryObs, payloadObs]).pipe(
        map(([req, pay]) => {
          return { req, pay };
        })
      );
    },
    aggregate: (input: Observable<RemoteData<any>>[]): Observable<RemoteData<any[]>> => {
      return createSuccessfulRemoteDataObject$([]);
    },
    buildFromHref: (href: string): Observable<RemoteData<any>> => {
      return createSuccessfulRemoteDataObject$(Object.assign(new SearchObjects(), {
        page: []
      }));
    }
  };
  beforeEach(() => {
    service = new SearchConfigurationService(routeService, paginationService as any, activatedRoute, linkService, halService, requestService, rdb);
  });

  describe('when the scope is called', () => {
    beforeEach(() => {
      service.getCurrentScope('');
    });
    it('should call getQueryParameterValue on the routeService with parameter name \'scope\'', () => {
      expect((service as any).routeService.getQueryParameterValue).toHaveBeenCalledWith('scope');
    });
  });

  describe('when getCurrentConfiguration is called', () => {
    beforeEach(() => {
      service.getCurrentConfiguration('');
    });
    it('should call getQueryParameterValue on the routeService with parameter name \'configuration\'', () => {
      expect((service as any).routeService.getQueryParameterValue).toHaveBeenCalledWith('configuration');
    });
  });

  describe('when getCurrentQuery is called', () => {
    beforeEach(() => {
      service.getCurrentQuery('');
    });
    it('should call getQueryParameterValue on the routeService with parameter name \'query\'', () => {
      expect((service as any).routeService.getQueryParameterValue).toHaveBeenCalledWith('query');
    });
  });

  describe('when getCurrentDSOType is called', () => {
    beforeEach(() => {
      service.getCurrentDSOType();
    });
    it('should call getQueryParameterValue on the routeService with parameter name \'dsoType\'', () => {
      expect((service as any).routeService.getQueryParameterValue).toHaveBeenCalledWith('dsoType');
    });
  });

  describe('when getCurrentFrontendFilters is called', () => {
    beforeEach(() => {
      service.getCurrentFrontendFilters();
    });
    it('should call getQueryParamsWithPrefix on the routeService with parameter prefix \'f.\'', () => {
      expect((service as any).routeService.getQueryParamsWithPrefix).toHaveBeenCalledWith('f.');
    });
  });

  describe('when getCurrentFilters is called', () => {
    let parsedValues$;
    beforeEach(() => {
      parsedValues$ = service.getCurrentFilters();
    });
    it('should call getQueryParamsWithPrefix on the routeService with parameter prefix \'f.\'', () => {
      expect((service as any).routeService.getQueryParamsWithPrefix).toHaveBeenCalledWith('f.');
      parsedValues$.subscribe((values) => {
        expect(values).toEqual(backendFilters);
      });
    });
  });

  describe('when getCurrentSort is called', () => {
    beforeEach(() => {
      service.getCurrentSort(defaults.pagination.id, {} as any);
    });
    it('should call getCurrentSort on the paginationService with the provided id and sort options', () => {
      expect((service as any).paginationService.getCurrentSort).toHaveBeenCalledWith(defaults.pagination.id, {});
    });
  });

  describe('when getCurrentPagination is called', () => {
    beforeEach(() => {
      service.getCurrentPagination(defaults.pagination.id, defaults.pagination);
    });
    it('should call getCurrentPagination on the paginationService with the provided id and sort options', () => {
      expect((service as any).paginationService.getCurrentPagination).toHaveBeenCalledWith(defaults.pagination.id, defaults.pagination);
    });
  });

  describe('when subscribeToSearchOptions or subscribeToPaginatedSearchOptions is called', () => {
    beforeEach(() => {
      spyOn(service, 'getCurrentPagination').and.callThrough();
      spyOn(service, 'getCurrentSort').and.callThrough();
      spyOn(service, 'getCurrentScope').and.callThrough();
      spyOn(service, 'getCurrentConfiguration').and.callThrough();
      spyOn(service, 'getCurrentQuery').and.callThrough();
      spyOn(service, 'getCurrentDSOType').and.callThrough();
      spyOn(service, 'getCurrentFilters').and.callThrough();
    });

    describe('when subscribeToSearchOptions is called', () => {
      beforeEach(() => {
        (service as any).subscribeToSearchOptions(defaults);
      });
      it('should call all getters it needs, but not call any others', () => {
        expect(service.getCurrentPagination).not.toHaveBeenCalled();
        expect(service.getCurrentSort).not.toHaveBeenCalled();
        expect(service.getCurrentScope).toHaveBeenCalled();
        expect(service.getCurrentConfiguration).toHaveBeenCalled();
        expect(service.getCurrentQuery).toHaveBeenCalled();
        expect(service.getCurrentDSOType).toHaveBeenCalled();
        expect(service.getCurrentFilters).toHaveBeenCalled();
      });
    });

    describe('when subscribeToPaginatedSearchOptions is called', () => {
      beforeEach(() => {
        (service as any).subscribeToPaginatedSearchOptions(defaults.pagination.id, defaults);
      });
      it('should call all getters it needs', () => {
        expect(service.getCurrentPagination).toHaveBeenCalled();
        expect(service.getCurrentSort).toHaveBeenCalled();
        expect(service.getCurrentScope).toHaveBeenCalled();
        expect(service.getCurrentConfiguration).toHaveBeenCalled();
        expect(service.getCurrentQuery).toHaveBeenCalled();
        expect(service.getCurrentDSOType).toHaveBeenCalled();
        expect(service.getCurrentFilters).toHaveBeenCalled();
      });
    });
  });

  describe('when getSearchConfigurationFor is called with a scope', () => {
    const endPoint = 'http://endpoint.com/test/config';
    const scope = 'test';
    const requestUrl = endPoint + '?scope=' + scope;
    beforeEach(() => {
      spyOn((service as any).halService, 'getEndpoint').and.returnValue(observableOf(endPoint));
      service.getSearchConfigurationFor(scope).subscribe((t) => {
      }); // subscribe to make sure all methods are called
    });

    it('should call getEndpoint on the halService', () => {
      expect((service as any).halService.getEndpoint).toHaveBeenCalled();
    });

    it('should send out the request on the request service', () => {
      expect((service as any).requestService.send).toHaveBeenCalled();
    });

    it('should call send containing a request with the correct request url', () => {
      expect((service as any).requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({ href: requestUrl }), true);
    });
  });

  describe('when getSearchConfigurationFor is called without a scope', () => {
    const endPoint = 'http://endpoint.com/test/config';
    beforeEach(() => {
      spyOn((service as any).halService, 'getEndpoint').and.returnValue(observableOf(endPoint));
      spyOn((service as any).rdb, 'buildFromHref').and.callThrough();
      service.getSearchConfigurationFor(null).subscribe((t) => {
      }); // subscribe to make sure all methods are called
    });

    it('should call getEndpoint on the halService', () => {
      expect((service as any).halService.getEndpoint).toHaveBeenCalled();
    });

    it('should send out the request on the request service', () => {
      expect((service as any).requestService.send).toHaveBeenCalled();
    });

    it('should call send containing a request with the correct request url', () => {
      expect((service as any).requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({ href: endPoint }), true);
    });
  });
  describe('when getConfig is called without a scope', () => {
    const endPoint = 'http://endpoint.com/test/config';
    beforeEach(() => {
      spyOn((service as any).halService, 'getEndpoint').and.returnValue(observableOf(endPoint));
      spyOn((service as any).rdb, 'buildFromHref').and.callThrough();
      service.getConfig(null).subscribe((t) => {
      }); // subscribe to make sure all methods are called
    });

    it('should call getEndpoint on the halService', () => {
      expect((service as any).halService.getEndpoint).toHaveBeenCalled();
    });

    it('should send out the request on the request service', () => {
      expect((service as any).requestService.send).toHaveBeenCalled();
    });

    it('should call send containing a request with the correct request url', () => {
      expect((service as any).requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({ href: endPoint }), true);
    });
  });

  describe('when getConfig is called with a scope', () => {
    const endPoint = 'http://endpoint.com/test/config';
    const scope = 'test';
    const requestUrl = endPoint + '?scope=' + scope;
    beforeEach(() => {
      spyOn((service as any).halService, 'getEndpoint').and.returnValue(observableOf(endPoint));
      service.getConfig(scope).subscribe((t) => {
      }); // subscribe to make sure all methods are called
    });

    it('should call getEndpoint on the halService', () => {
      expect((service as any).halService.getEndpoint).toHaveBeenCalled();
    });

    it('should send out the request on the request service', () => {
      expect((service as any).requestService.send).toHaveBeenCalled();
    });

    it('should call send containing a request with the correct request url', () => {
      expect((service as any).requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({ href: requestUrl }), true);
    });
  });
});
