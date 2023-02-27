import { of as observableOf } from 'rxjs';

import { MyDSpaceConfigurationService } from './my-dspace-configuration.service';
import { PaginatedSearchOptions } from '../shared/search/models/paginated-search-options.model';
import { PaginationComponentOptions } from '../shared/pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../core/cache/models/sort-options.model';
import { SearchFilter } from '../shared/search/models/search-filter.model';
import { ActivatedRouteStub } from '../shared/testing/active-router.stub';
import { RoleServiceMock } from '../shared/mocks/role-service.mock';
import { cold, hot } from 'jasmine-marbles';
import { MyDSpaceConfigurationValueType } from './my-dspace-configuration-value-type';
import { PaginationServiceStub } from '../shared/testing/pagination-service.stub';
import { Context } from '../core/shared/context.model';
import { HALEndpointServiceStub } from '../shared/testing/hal-endpoint-service.stub';
import { getMockRemoteDataBuildService } from '../shared/mocks/remote-data-build.service.mock';

describe('MyDSpaceConfigurationService', () => {
  let service: MyDSpaceConfigurationService;
  const value1 = 'random value';
  const prefixFilter = {
    'f.namedresourcetype': ['another value'],
    'f.dateSubmitted.min': ['2013'],
    'f.dateSubmitted.max': ['2018']
  };
  const defaults = new PaginatedSearchOptions({
    pagination: Object.assign(new PaginationComponentOptions(), { currentPage: 1, pageSize: 20 }),
    sort: new SortOptions('score', SortDirection.DESC),
    query: '',
    scope: ''
  });

  const backendFilters = [
    new SearchFilter('f.namedresourcetype', ['another value']),
    new SearchFilter('f.dateSubmitted', ['[2013 TO 2018]'], 'equals')
  ];

  const spy = jasmine.createSpyObj('RouteService', {
    getQueryParameterValue: observableOf(value1),
    getQueryParamsWithPrefix: observableOf(prefixFilter),
    getRouteParameterValue: observableOf(''),
    getRouteDataValue: observableOf({})
  });

  const paginationService = new PaginationServiceStub();
  const activatedRoute: any = new ActivatedRouteStub();

  const roleService: any = new RoleServiceMock();

  const linkService: any = {};
  const halService: any = new HALEndpointServiceStub('');
  const requestService: any = {};
  const rdb: any = getMockRemoteDataBuildService();

  beforeEach(() => {
    service = new MyDSpaceConfigurationService(roleService, spy, paginationService as any, activatedRoute, linkService, halService, requestService, rdb);
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
      service.getCurrentSort('page-id', defaults.sort);
    });
    it('should call getCurrentSort on the paginationService with the provided id and sort options', () => {
      expect((service as any).paginationService.getCurrentSort).toHaveBeenCalledWith('page-id', defaults.sort);
    });
  });

  describe('when getCurrentPagination is called', () => {
    beforeEach(() => {
      service.getCurrentPagination('page-id', defaults.pagination);
    });
    it('should call getCurrentPagination on the paginationService with the provided id and sort options', () => {
      expect((service as any).paginationService.getCurrentPagination).toHaveBeenCalledWith('page-id', defaults.pagination);
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
        (service as any).subscribeToPaginatedSearchOptions('id', defaults);
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

  describe('when getAvailableConfigurationTypes is called', () => {

    it('should return properly list when user is submitter', () => {
      roleService.setSubmitter(true);
      roleService.setController(false);
      roleService.setAdmin(false);

      const list$ = service.getAvailableConfigurationTypes();

      expect(list$).toBeObservable(cold('(b|)', {
        b: [
          MyDSpaceConfigurationValueType.Workspace
        ]
      }));
    });

    it('should return properly list when user is controller', () => {
      roleService.setSubmitter(false);
      roleService.setController(true);
      roleService.setAdmin(false);

      const list$ = service.getAvailableConfigurationTypes();

      expect(list$).toBeObservable(cold('(b|)', {
        b: [
          MyDSpaceConfigurationValueType.SupervisedItems,
          MyDSpaceConfigurationValueType.Workflow
        ]
      }));
    });

    it('should return properly list when user is admin', () => {
      roleService.setSubmitter(false);
      roleService.setController(false);
      roleService.setAdmin(true);

      const list$ = service.getAvailableConfigurationTypes();

      expect(list$).toBeObservable(cold('(b|)', {
        b: [
          MyDSpaceConfigurationValueType.SupervisedItems,
          MyDSpaceConfigurationValueType.Workflow
        ]
      }));
    });

    it('should return properly list when user is submitter and controller', () => {
      roleService.setSubmitter(true);
      roleService.setController(true);
      roleService.setAdmin(false);

      const list$ = service.getAvailableConfigurationTypes();

      expect(list$).toBeObservable(cold('(b|)', {
        b: [
          MyDSpaceConfigurationValueType.Workspace,
          MyDSpaceConfigurationValueType.SupervisedItems,
          MyDSpaceConfigurationValueType.Workflow
        ]
      }));
    });
  });

  describe('when getAvailableConfigurationOptions is called', () => {

    it('should return properly options list', () => {
      spyOn(service, 'getAvailableConfigurationTypes').and.returnValue(hot('a', {
        a: [
          MyDSpaceConfigurationValueType.Workspace,
          MyDSpaceConfigurationValueType.Workflow
        ]
      }));

      const list$ = service.getAvailableConfigurationOptions();

      expect(list$).toBeObservable(cold('(b|)', {
        b: [
          {
            value: MyDSpaceConfigurationValueType.Workspace,
            label: `mydspace.show.${MyDSpaceConfigurationValueType.Workspace}`,
            context: Context.Workspace
          },
          {
            value: MyDSpaceConfigurationValueType.Workflow,
            label: `mydspace.show.${MyDSpaceConfigurationValueType.Workflow}`,
            context: Context.Workflow
          }
        ]
      }));
    });
  });
});
