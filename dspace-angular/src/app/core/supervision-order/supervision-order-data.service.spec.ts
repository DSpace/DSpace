import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { SupervisionOrderDataService } from './supervision-order-data.service';
import { ActionType } from './models/action-type.model';
import { RequestParam } from '../cache/models/request-param.model';
import { PageInfo } from '../shared/page-info.model';
import { buildPaginatedList } from '../data/paginated-list.model';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { RestResponse } from '../cache/response.models';
import { RequestEntry } from '../data/request-entry.model';
import { FindListOptions } from '../data/find-list-options.model';
import { GroupDataService } from '../eperson/group-data.service';

describe('SupervisionOrderService', () => {
  let scheduler: TestScheduler;
  let service: SupervisionOrderDataService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let halService: HALEndpointService;
  let responseCacheEntry: RequestEntry;
  let groupService: GroupDataService;

  const supervisionOrder: any = {
    id: '1',
    name: null,
    description: null,
    action: ActionType.READ,
    startDate: null,
    endDate: null,
    type: 'supervisionOrder',
    uuid: 'supervision-order-1',
    _links: {
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/supervisionorder/1'
      },
    }
  };

  const anothersupervisionOrder: any = {
    id: '2',
    name: null,
    description: null,
    action: ActionType.WRITE,
    startDate: null,
    endDate: null,
    type: 'supervisionOrder',
    uuid: 'supervision-order-2',
    _links: {
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/supervisionorder/1'
      },
    }
  };
  const endpointURL = `https://rest.api/rest/api/supervisionorder`;
  const requestURL = `https://rest.api/rest/api/supervisionorder/${supervisionOrder.id}`;
  const requestUUID = '8b3c613a-5a4b-438b-9686-be1d5b4a1c5a';
  const supervisionOrderId = '1';
  const groupUUID = '8b39g7ya-5a4b-438b-9686-be1d5b4a1c5a';
  const itemUUID = '8b39g7ya-5a4b-438b-851f-be1d5b4a1c5a';
  const supervisionOrderType = 'NONE';

  const pageInfo = new PageInfo();
  const array = [supervisionOrder, anothersupervisionOrder];
  const paginatedList = buildPaginatedList(pageInfo, array);
  const supervisionOrderRD = createSuccessfulRemoteDataObject(supervisionOrder);
  const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);

  const groupEndpoint = 'group_EP';

  beforeEach(() => {
    scheduler = getTestScheduler();

    halService = jasmine.createSpyObj('halService', {
      getEndpoint: cold('a', { a: endpointURL })
    });

    responseCacheEntry = new RequestEntry();
    responseCacheEntry.request = { href: 'https://rest.api/' } as any;
    responseCacheEntry.response = new RestResponse(true, 200, 'Success');

    requestService = jasmine.createSpyObj('requestService', {
      generateRequestId: requestUUID,
      send: true,
      removeByHrefSubstring: {},
      getByHref: observableOf(responseCacheEntry),
      getByUUID: observableOf(responseCacheEntry),
      setStaleByHrefSubstring: {},
    });
    rdbService = jasmine.createSpyObj('rdbService', {
      buildSingle: hot('a|', {
        a: supervisionOrderRD
      }),
      buildList: hot('a|', {
        a: paginatedListRD
      }),
      buildFromRequestUUID: hot('a|', {
        a: supervisionOrderRD
      }),
      buildFromRequestUUIDAndAwait: hot('a|', {
        a: supervisionOrderRD
      })
    });
    groupService = jasmine.createSpyObj('groupService', {
      getBrowseEndpoint: hot('a', {
        a: groupEndpoint
      }),
      getIDHrefObs: cold('a', {
        a: 'https://rest.api/rest/api/group/groups/' + groupUUID
      }),
    });
    groupService = jasmine.createSpyObj('groupService', {
      getIDHrefObs: cold('a', {
        a: 'https://rest.api/rest/api/group/groups/' + groupUUID
      }),
    });
    objectCache = {} as ObjectCacheService;
    const notificationsService = {} as NotificationsService;
    const comparator = {} as any;

    service = new SupervisionOrderDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
      notificationsService,
      comparator,
      groupService,
    );

    spyOn(service, 'findById').and.callThrough();
    spyOn(service, 'findByHref').and.callThrough();
    spyOn(service, 'invalidateByHref').and.returnValue(observableOf(true));
    spyOn((service as any).createData, 'create').and.callThrough();
    spyOn((service as any).deleteData, 'delete').and.callThrough();
    spyOn((service as any).patchData, 'update').and.callThrough();
    spyOn((service as any).searchData, 'searchBy').and.callThrough();
    spyOn((service as any).searchData, 'getSearchByHref').and.returnValue(observableOf(requestURL));
  });

  describe('create', () => {
    it('should proxy the call to createData.create with group UUID', () => {
      scheduler.schedule(() => service.create(supervisionOrder, itemUUID, groupUUID, supervisionOrderType));
      const params = [
        new RequestParam('uuid', itemUUID),
        new RequestParam('group', groupUUID),
        new RequestParam('type', supervisionOrderType),
      ];
      scheduler.flush();

      expect((service as any).createData.create).toHaveBeenCalledWith(supervisionOrder, ...params);
    });

    it('should proxy the call to createData.create with group UUID', () => {
      scheduler.schedule(() => service.create(supervisionOrder, itemUUID, groupUUID, supervisionOrderType));
      const params = [
        new RequestParam('uuid', itemUUID),
        new RequestParam('group', groupUUID),
        new RequestParam('type', supervisionOrderType),
      ];
      scheduler.flush();

      expect((service as any).createData.create).toHaveBeenCalledWith(supervisionOrder, ...params);
    });

    it('should return a RemoteData<supervisionOrder> for the object with the given id', () => {
      const result = service.create(supervisionOrder, itemUUID, groupUUID, supervisionOrderType);
      const expected = cold('a|', {
        a: supervisionOrderRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('delete', () => {
    it('should proxy the call to deleteData.delete', () => {
      scheduler.schedule(() => service.delete(supervisionOrderId));
      scheduler.flush();

      expect((service as any).deleteData.delete).toHaveBeenCalledWith(supervisionOrderId);
    });
  });

  describe('update', () => {
    it('should proxy the call to updateData.update', () => {
      scheduler.schedule(() => service.update(supervisionOrder));
      scheduler.flush();

      expect((service as any).patchData.update).toHaveBeenCalledWith(supervisionOrder);
    });
  });

  describe('findById', () => {
    it('should return a RemoteData<supervisionOrder> for the object with the given id', () => {
      const result = service.findById(supervisionOrderId);
      const expected = cold('a|', {
        a: supervisionOrderRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('findByHref', () => {
    it('should return a RemoteData<supervisionOrder> for the object with the given URL', () => {
      const result = service.findByHref(requestURL);
      const expected = cold('a|', {
        a: supervisionOrderRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('searchByGroup', () => {
    it('should proxy the call to searchData.searchBy', () => {
      const options = new FindListOptions();
      options.searchParams = [new RequestParam('uuid', groupUUID)];
      scheduler.schedule(() => service.searchByGroup(groupUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByGroupMethod, options, true, true);
    });

    it('should proxy the call to searchData.searchBy with additional search param', () => {
      const options = new FindListOptions();
      options.searchParams = [
        new RequestParam('uuid', groupUUID),
        new RequestParam('item', itemUUID),
      ];
      scheduler.schedule(() => service.searchByGroup(groupUUID, itemUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByGroupMethod, options, true, true);
    });

    it('should return a RemoteData<PaginatedList<supervisionOrder>) for the search', () => {
      const result = service.searchByGroup(groupUUID);
      const expected = cold('a|', {
        a: paginatedListRD
      });
      expect(result).toBeObservable(expected);
    });

  });

  describe('searchByItem', () => {
    it('should proxy the call to searchData.searchBy', () => {
      const options = new FindListOptions();
      options.searchParams = [new RequestParam('uuid', itemUUID)];
      scheduler.schedule(() => service.searchByItem(itemUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByItemMethod, options, true, true);
    });

    it('should return a RemoteData<PaginatedList<supervisionOrder>) for the search', () => {
      const result = service.searchByItem(itemUUID);
      const expected = cold('a|', {
        a: paginatedListRD
      });
      expect(result).toBeObservable(expected);
    });
  });

});
