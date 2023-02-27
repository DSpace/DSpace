import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { ResourcePolicyDataService } from './resource-policy-data.service';
import { PolicyType } from './models/policy-type.model';
import { ActionType } from './models/action-type.model';
import { RequestParam } from '../cache/models/request-param.model';
import { PageInfo } from '../shared/page-info.model';
import { buildPaginatedList } from '../data/paginated-list.model';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { RestResponse } from '../cache/response.models';
import { RequestEntry } from '../data/request-entry.model';
import { FindListOptions } from '../data/find-list-options.model';
import { EPersonDataService } from '../eperson/eperson-data.service';
import { GroupDataService } from '../eperson/group-data.service';
import { RestRequestMethod } from '../data/rest-request-method';

describe('ResourcePolicyService', () => {
  let scheduler: TestScheduler;
  let service: ResourcePolicyDataService;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let halService: HALEndpointService;
  let responseCacheEntry: RequestEntry;
  let ePersonService: EPersonDataService;
  let groupService: GroupDataService;

  const resourcePolicy: any = {
    id: '1',
    name: null,
    description: null,
    policyType: PolicyType.TYPE_SUBMISSION,
    action: ActionType.READ,
    startDate: null,
    endDate: null,
    type: 'resourcepolicy',
    uuid: 'resource-policy-1',
    _links: {
      eperson: {
        href: 'https://rest.api/rest/api/eperson'
      },
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/resourcepolicies/1'
      },
    }
  };

  const anotherResourcePolicy: any = {
    id: '2',
    name: null,
    description: null,
    policyType: PolicyType.TYPE_SUBMISSION,
    action: ActionType.WRITE,
    startDate: null,
    endDate: null,
    type: 'resourcepolicy',
    uuid: 'resource-policy-2',
    _links: {
      eperson: {
        href: 'https://rest.api/rest/api/eperson'
      },
      group: {
        href: 'https://rest.api/rest/api/group'
      },
      self: {
        href: 'https://rest.api/rest/api/resourcepolicies/1'
      },
    }
  };
  const endpointURL = `https://rest.api/rest/api/resourcepolicies`;
  const requestURL = `https://rest.api/rest/api/resourcepolicies/${resourcePolicy.id}`;
  const requestUUID = '8b3c613a-5a4b-438b-9686-be1d5b4a1c5a';
  const resourcePolicyId = '1';
  const epersonUUID = '8b39g7ya-5a4b-438b-9686-be1d5b4a1c5a';
  const groupUUID = '8b39g7ya-5a4b-36987-9686-be1d5b4a1c5a';
  const resourceUUID = '8b39g7ya-5a4b-438b-851f-be1d5b4a1c5a';

  const pageInfo = new PageInfo();
  const array = [resourcePolicy, anotherResourcePolicy];
  const paginatedList = buildPaginatedList(pageInfo, array);
  const resourcePolicyRD = createSuccessfulRemoteDataObject(resourcePolicy);
  const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);

  const ePersonEndpoint = 'EPERSON_EP';

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
        a: resourcePolicyRD
      }),
      buildList: hot('a|', {
        a: paginatedListRD
      }),
      buildFromRequestUUID: hot('a|', {
        a: resourcePolicyRD
      }),
      buildFromRequestUUIDAndAwait: hot('a|', {
        a: resourcePolicyRD
      })
    });
    ePersonService = jasmine.createSpyObj('ePersonService', {
      getBrowseEndpoint: hot('a', {
        a: ePersonEndpoint
      }),
      getIDHrefObs: cold('a', {
        a: 'https://rest.api/rest/api/eperson/epersons/' + epersonUUID
      }),
    });
    groupService = jasmine.createSpyObj('groupService', {
      getIDHrefObs: cold('a', {
        a: 'https://rest.api/rest/api/eperson/groups/' + groupUUID
      }),
    });
    objectCache = {} as ObjectCacheService;
    const notificationsService = {} as NotificationsService;
    const comparator = {} as any;

    service = new ResourcePolicyDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
      notificationsService,
      comparator,
      ePersonService,
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
    it('should proxy the call to createData.create with eperson UUID', () => {
      scheduler.schedule(() => service.create(resourcePolicy, resourceUUID, epersonUUID));
      const params = [
        new RequestParam('resource', resourceUUID),
        new RequestParam('eperson', epersonUUID),
      ];
      scheduler.flush();

      expect((service as any).createData.create).toHaveBeenCalledWith(resourcePolicy, ...params);
    });

    it('should proxy the call to createData.create with group UUID', () => {
      scheduler.schedule(() => service.create(resourcePolicy, resourceUUID, null, groupUUID));
      const params = [
        new RequestParam('resource', resourceUUID),
        new RequestParam('group', groupUUID),
      ];
      scheduler.flush();

      expect((service as any).createData.create).toHaveBeenCalledWith(resourcePolicy, ...params);
    });

    it('should return a RemoteData<ResourcePolicy> for the object with the given id', () => {
      const result = service.create(resourcePolicy, resourceUUID, epersonUUID);
      const expected = cold('a|', {
        a: resourcePolicyRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('delete', () => {
    it('should proxy the call to deleteData.delete', () => {
      scheduler.schedule(() => service.delete(resourcePolicyId));
      scheduler.flush();

      expect((service as any).deleteData.delete).toHaveBeenCalledWith(resourcePolicyId);
    });
  });

  describe('update', () => {
    it('should proxy the call to updateData.update', () => {
      scheduler.schedule(() => service.update(resourcePolicy));
      scheduler.flush();

      expect((service as any).patchData.update).toHaveBeenCalledWith(resourcePolicy);
    });
  });

  describe('findById', () => {
    it('should return a RemoteData<ResourcePolicy> for the object with the given id', () => {
      const result = service.findById(resourcePolicyId);
      const expected = cold('a|', {
        a: resourcePolicyRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('findByHref', () => {
    it('should return a RemoteData<ResourcePolicy> for the object with the given URL', () => {
      const result = service.findByHref(requestURL);
      const expected = cold('a|', {
        a: resourcePolicyRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('searchByEPerson', () => {
    it('should proxy the call to searchData.searchBy', () => {
      const options = new FindListOptions();
      options.searchParams = [new RequestParam('uuid', epersonUUID)];
      scheduler.schedule(() => service.searchByEPerson(epersonUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByEPersonMethod, options, true, true);
    });

    it('should proxy the call to searchData.searchBy with additional search param', () => {
      const options = new FindListOptions();
      options.searchParams = [
        new RequestParam('uuid', epersonUUID),
        new RequestParam('resource', resourceUUID),
      ];
      scheduler.schedule(() => service.searchByEPerson(epersonUUID, resourceUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByEPersonMethod, options, true, true);
    });

    it('should return a RemoteData<PaginatedList<ResourcePolicy>) for the search', () => {
      const result = service.searchByEPerson(epersonUUID, resourceUUID);
      const expected = cold('a|', {
        a: paginatedListRD
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
        new RequestParam('resource', resourceUUID),
      ];
      scheduler.schedule(() => service.searchByGroup(groupUUID, resourceUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByGroupMethod, options, true, true);
    });

    it('should return a RemoteData<PaginatedList<ResourcePolicy>) for the search', () => {
      const result = service.searchByGroup(groupUUID);
      const expected = cold('a|', {
        a: paginatedListRD
      });
      expect(result).toBeObservable(expected);
    });

  });

  describe('searchByResource', () => {
    it('should proxy the call to searchData.searchBy', () => {
      const options = new FindListOptions();
      options.searchParams = [new RequestParam('uuid', resourceUUID)];
      scheduler.schedule(() => service.searchByResource(resourceUUID));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByResourceMethod, options, true, true);
    });

    it('should proxy the call to searchData.searchBy with additional search param', () => {
      const action = ActionType.READ;
      const options = new FindListOptions();
      options.searchParams = [
        new RequestParam('uuid', resourceUUID),
        new RequestParam('action', action),
      ];
      scheduler.schedule(() => service.searchByResource(resourceUUID, action));
      scheduler.flush();

      expect((service as any).searchData.searchBy).toHaveBeenCalledWith((service as any).searchByResourceMethod, options, true, true);
    });

    it('should return a RemoteData<PaginatedList<ResourcePolicy>) for the search', () => {
      const result = service.searchByResource(resourceUUID);
      const expected = cold('a|', {
        a: paginatedListRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('updateTarget', () => {
    beforeEach(() => {
      scheduler.schedule(() => service.create(resourcePolicy, resourceUUID, epersonUUID));
    });

    it('should send a PUT request to update the EPerson', () => {
      service.updateTarget(resourcePolicyId, requestURL, epersonUUID, 'eperson');
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(jasmine.objectContaining({
        method: RestRequestMethod.PUT,
        uuid: requestUUID,
        href: `${resourcePolicy._links.self.href}/eperson`,
        body: 'https://rest.api/rest/api/eperson/epersons/' + epersonUUID,
      }));
    });

    it('should invalidate the ResourcePolicy', () => {
      service.updateTarget(resourcePolicyId, requestURL, epersonUUID, 'eperson');
      scheduler.flush();

      expect(rdbService.buildFromRequestUUIDAndAwait).toHaveBeenCalled();
      expect((rdbService.buildFromRequestUUIDAndAwait as jasmine.Spy).calls.argsFor(0)[0]).toBe(requestService.generateRequestId());
      const callback = (rdbService.buildFromRequestUUIDAndAwait as jasmine.Spy).calls.argsFor(0)[1];
      callback();

      expect(service.invalidateByHref).toHaveBeenCalledWith(resourcePolicy._links.self.href);
    });
  });

});
