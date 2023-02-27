import { StatisticsService } from './statistics.service';
import { RequestService } from '../core/data/request.service';
import { HALEndpointServiceStub } from '../shared/testing/hal-endpoint-service.stub';
import { getMockRequestService } from '../shared/mocks/request.service.mock';
import isEqual from 'lodash/isEqual';
import { DSpaceObjectType } from '../core/shared/dspace-object-type.model';
import { SearchOptions } from '../shared/search/models/search-options.model';
import { RestRequest } from '../core/data/rest-request.model';

describe('StatisticsService', () => {
  let service: StatisticsService;
  let requestService: jasmine.SpyObj<RequestService>;
  const restURL = 'https://rest.api';
  const halService: any = new HALEndpointServiceStub(restURL);

  function initTestService() {
    return new StatisticsService(
      requestService,
      halService,
    );
  }

  describe('trackViewEvent', () => {
    requestService = getMockRequestService();
    service = initTestService();

    it('should send a request to track an item view ', () => {
      const mockItem: any = {uuid: 'mock-item-uuid', type: 'item'};
      service.trackViewEvent(mockItem);
      const request: RestRequest = requestService.send.calls.mostRecent().args[0];
      expect(request.body).toBeDefined('request.body');
      const body = JSON.parse(request.body);
      expect(body.targetId).toBe('mock-item-uuid');
      expect(body.targetType).toBe('item');
    });
  });

  describe('trackSearchEvent', () => {
    requestService = getMockRequestService();
    service = initTestService();

    const mockSearch: any = new SearchOptions({
      query: 'mock-query',
    });

    const page = {
      size: 10,
      totalElements: 248,
      totalPages: 25,
      number: 4
    };
    const sort = {by: 'search-field', order: 'ASC'};
    service.trackSearchEvent(mockSearch, page, sort);
    const request: RestRequest = requestService.send.calls.mostRecent().args[0];
    const body = JSON.parse(request.body);

    it('should specify the right query', () => {
      expect(body.query).toBe('mock-query');
    });

    it('should specify the pagination info', () => {
      expect(body.page).toEqual({
        size: 10,
        totalElements: 248,
        totalPages: 25,
        number: 4
      });
    });

    it('should specify the sort options', () => {
      expect(body.sort).toEqual({
        by: 'search-field',
        order: 'asc'
      });
    });
  });

  describe('trackSearchEvent with optional parameters', () => {
    requestService = getMockRequestService();
    service = initTestService();

    const mockSearch: any = new SearchOptions({
      query: 'mock-query',
      configuration: 'mock-configuration',
      dsoTypes: [DSpaceObjectType.ITEM],
      scope: 'mock-scope'
    });

    const page = {
      size: 10,
      totalElements: 248,
      totalPages: 25,
      number: 4
    };
    const sort = {by: 'search-field', order: 'ASC'};
    const filters = [
      {
        filter: 'title',
        operator: 'notcontains',
        value: 'dolor sit',
        label: 'dolor sit'
      },
      {
        filter: 'author',
        operator: 'authority',
        value: '9zvxzdm4qru17or5a83wfgac',
        label: 'Amet, Consectetur'
      }
    ];
    service.trackSearchEvent(mockSearch, page, sort, filters);
    const request: RestRequest = requestService.send.calls.mostRecent().args[0];
    const body = JSON.parse(request.body);

    it('should specify the dsoType', () => {
      expect(body.dsoType).toBe('item');
    });

    it('should specify the scope', () => {
      expect(body.scope).toBe('mock-scope');
    });

    it('should specify the configuration', () => {
      expect(body.configuration).toBe('mock-configuration');
    });

    it('should specify the filters', () => {
      expect(isEqual(body.appliedFilters, [
        {
          filter: 'title',
          operator: 'notcontains',
          value: 'dolor sit',
          label: 'dolor sit'
        },
        {
          filter: 'author',
          operator: 'authority',
          value: '9zvxzdm4qru17or5a83wfgac',
          label: 'Amet, Consectetur'
        }
      ])).toBe(true);
    });
  });

});
