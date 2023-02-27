import { HttpClient, HttpHeaders } from '@angular/common/http';

import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';

import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { PageInfo } from '../shared/page-info.model';
import { buildPaginatedList } from '../data/paginated-list.model';
import { createNoContentRemoteDataObject$, createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { RestResponse } from '../cache/response.models';
import { RequestEntry } from '../data/request-entry.model';
import { ResearcherProfileDataService } from './researcher-profile-data.service';
import { RouterMock } from '../../shared/mocks/router.mock';
import { ResearcherProfile } from './model/researcher-profile.model';
import { Item } from '../shared/item.model';
import { ReplaceOperation } from 'fast-json-patch';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { PostRequest } from '../data/request.models';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { ConfigurationProperty } from '../shared/configuration-property.model';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { testCreateDataImplementation } from '../data/base/create-data.spec';
import { testSearchDataImplementation } from '../data/base/search-data.spec';
import { testPatchDataImplementation } from '../data/base/patch-data.spec';
import { testDeleteDataImplementation } from '../data/base/delete-data.spec';

describe('ResearcherProfileService', () => {
  let scheduler: TestScheduler;
  let service: ResearcherProfileDataService;
  let serviceAsAny: any;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let halService: HALEndpointService;
  let responseCacheEntry: RequestEntry;
  let routerStub: any;

  const researcherProfileId = 'beef9946-rt56-479e-8f11-b90cbe9f7241';
  const itemId = 'beef9946-rt56-479e-8f11-b90cbe9f7241';
  const researcherProfileItem: Item = Object.assign(new Item(), {
    id: itemId,
    _links: {
      self: {
        href: `https://rest.api/rest/api/items/${itemId}`
      },
    }
  });
  const researcherProfile: ResearcherProfile = Object.assign(new ResearcherProfile(), {
    id: researcherProfileId,
    visible: false,
    type: 'profile',
    _links: {
      item: {
        href: `https://rest.api/rest/api/profiles/${researcherProfileId}/item`
      },
      self: {
        href: `https://rest.api/rest/api/profiles/${researcherProfileId}`
      },
    }
  });

  const researcherProfilePatched: ResearcherProfile = Object.assign(new ResearcherProfile(), {
    id: researcherProfileId,
    visible: true,
    type: 'profile',
    _links: {
      item: {
        href: `https://rest.api/rest/api/profiles/${researcherProfileId}/item`
      },
      self: {
        href: `https://rest.api/rest/api/profiles/${researcherProfileId}`
      },
    }
  });

  const researcherProfileId2 = 'agbf9946-f4ce-479e-8f11-b90cbe9f7241';
  const anotherResearcherProfile: ResearcherProfile = Object.assign(new ResearcherProfile(), {
    id: researcherProfileId2,
    visible: false,
    type: 'profile',
    _links: {
      self: {
        href: `https://rest.api/rest/api/profiles/${researcherProfileId2}`
      },
    }
  });

  const mockItemUnlinkedToOrcid: Item = Object.assign(new Item(), {
    id: 'mockItemUnlinkedToOrcid',
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [{
        value: 'test person'
      }],
      'dspace.entity.type': [{
        'value': 'Person'
      }],
      'dspace.object.owner': [{
        'value': 'test person',
        'language': null,
        'authority': 'researcher-profile-id',
        'confidence': 600,
        'place': 0
      }],
    }
  });

  const mockItemLinkedToOrcid: Item = Object.assign(new Item(), {
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([])),
    metadata: {
      'dc.title': [{
        value: 'test person'
      }],
      'dspace.entity.type': [{
        'value': 'Person'
      }],
      'dspace.object.owner': [{
        'value': 'test person',
        'language': null,
        'authority': 'researcher-profile-id',
        'confidence': 600,
        'place': 0
      }],
      'dspace.orcid.authenticated': [{
        'value': '2022-06-10T15:15:12.952872',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }],
      'dspace.orcid.scope': [{
        'value': '/authenticate',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }, {
        'value': '/read-limited',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 1
      }, {
        'value': '/activities/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 2
      }, {
        'value': '/person/update',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 3
      }],
      'person.identifier.orcid': [{
        'value': 'orcid-id',
        'language': null,
        'authority': null,
        'confidence': -1,
        'place': 0
      }]
    }
  });

  const disconnectionAllowAdmin = {
    uuid: 'orcid.disconnection.allowed-users',
    name: 'orcid.disconnection.allowed-users',
    values: ['only_admin']
  } as ConfigurationProperty;

  const disconnectionAllowAdminOwner = {
    uuid: 'orcid.disconnection.allowed-users',
    name: 'orcid.disconnection.allowed-users',
    values: ['admin_and_owner']
  } as ConfigurationProperty;

  const authorizeUrl = {
    uuid: 'orcid.authorize-url',
    name: 'orcid.authorize-url',
    values: ['orcid.authorize-url']
  } as ConfigurationProperty;
  const appClientId = {
    uuid: 'orcid.application-client-id',
    name: 'orcid.application-client-id',
    values: ['orcid.application-client-id']
  } as ConfigurationProperty;
  const orcidScope = {
    uuid: 'orcid.scope',
    name: 'orcid.scope',
    values: ['/authenticate', '/read-limited']
  } as ConfigurationProperty;

  const endpointURL = `https://rest.api/rest/api/profiles`;
  const endpointURLWithEmbed = 'https://rest.api/rest/api/profiles?embed=item';
  const sourceUri = `https://rest.api/rest/api/external-source/profile`;
  const requestURL = `https://rest.api/rest/api/profiles/${researcherProfileId}`;
  const requestUUID = '8b3c613a-5a4b-438b-9686-be1d5b4a1c5a';

  const pageInfo = new PageInfo();
  const array = [researcherProfile, anotherResearcherProfile];
  const paginatedList = buildPaginatedList(pageInfo, array);
  const researcherProfileRD = createSuccessfulRemoteDataObject(researcherProfile);
  const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);

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
      setStaleByHrefSubstring: jasmine.createSpy('setStaleByHrefSubstring')
    });
    rdbService = jasmine.createSpyObj('rdbService', {
      buildSingle: hot('a|', {
        a: researcherProfileRD
      }),
      buildList: hot('a|', {
        a: paginatedListRD
      }),
      buildFromRequestUUID: hot('a|', {
        a: researcherProfileRD
      })
    });
    objectCache = {} as ObjectCacheService;
    const notificationsService = {} as NotificationsService;
    const http = {} as HttpClient;
    const comparator = {} as any;
    routerStub = new RouterMock();
    const itemService = jasmine.createSpyObj('ItemService', {
      findByHref: jasmine.createSpy('findByHref')
    });

    service = new ResearcherProfileDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
      notificationsService,
      routerStub,
      comparator,
      itemService,
    );
    serviceAsAny = service;

    spyOn((service as any), 'findById').and.callThrough();
    spyOn((service as any), 'findByHref').and.callThrough();
    spyOn((service as any), 'getLinkPath').and.returnValue(observableOf(endpointURL));
    spyOn((service as any).createData, 'create').and.callThrough();
    spyOn((service as any).patchData, 'update').and.callThrough();
    spyOn((service as any).searchData, 'searchBy').and.callThrough();
    spyOn((service as any).deleteData, 'delete').and.callThrough();
  });

  describe('composition', () => {
    const initService = () => new ResearcherProfileDataService(null, null, null, null, null, null, null, null);
    testCreateDataImplementation(initService);
    testSearchDataImplementation(initService);
    testPatchDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });

  describe('create', () => {
    it('should proxy the call to createData.create with eperson UUID', () => {
      scheduler.schedule(() => service.create());
      scheduler.flush();

      expect((service as any).createData.create).toHaveBeenCalled();
    });

    it('should return the RemoteData<ResearcherProfile> created', () => {
      const result = service.create();
      const expected = cold('a|', {
        a: researcherProfileRD
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('delete', () => {
    it('should proxy the call to deleteData.delete', () => {
      scheduler.schedule(() => service.delete(researcherProfile.id));
      scheduler.flush();

      expect((service as any).deleteData.delete).toHaveBeenCalledWith(researcherProfile.id, undefined);
    });
  });

  describe('findRelatedItemId', () => {
    describe('with a related item', () => {

      beforeEach(() => {
        (service as any).itemService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$(researcherProfileItem));
      });

      it('should proxy the call to dataservice.findById with eperson UUID', () => {
        scheduler.schedule(() => service.findRelatedItemId(researcherProfile));
        scheduler.flush();

        expect((service as any).itemService.findByHref).toHaveBeenCalledWith(researcherProfile._links.item.href, false);
      });

      it('should return a ResearcherProfile object with the given id', () => {
        const result = service.findRelatedItemId(researcherProfile);
        const expected = cold('(a|)', {
          a: itemId
        });
        expect(result).toBeObservable(expected);
      });
    });

    describe('without a related item', () => {

      beforeEach(() => {
        (service as any).itemService.findByHref.and.returnValue(createNoContentRemoteDataObject$());
      });

      it('should proxy the call to dataservice.findById with eperson UUID', () => {
        scheduler.schedule(() => service.findRelatedItemId(researcherProfile));
        scheduler.flush();

        expect((service as any).itemService.findByHref).toHaveBeenCalledWith(researcherProfile._links.item.href, false);
      });

      it('should not return a ResearcherProfile object with the given id', () => {
        const result = service.findRelatedItemId(researcherProfile);
        const expected = cold('(a|)', {
          a: null
        });
        expect(result).toBeObservable(expected);
      });
    });
  });

  describe('setVisibility', () => {
    beforeEach(() => {
      spyOn((service as any).patchData, 'patch').and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
      (service.findById as jasmine.Spy).and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
    });

    it('should proxy the call to patchData.patch', () => {
      const replaceOperation: ReplaceOperation<boolean> = {
        path: '/visible',
        op: 'replace',
        value: true,
      };

      scheduler.schedule(() => service.setVisibility(researcherProfile, true));
      scheduler.flush();

      expect((service as any).patchData.patch).toHaveBeenCalledWith(researcherProfile, [replaceOperation]);
    });
  });

  describe('createFromExternalSource', () => {

    beforeEach(() => {
      spyOn((service as any).patchData, 'patch').and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
      (service.findById as jasmine.Spy).and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
    });

    it('should proxy the call to patchData.patch', () => {
      const options: HttpOptions = Object.create({});
      let headers = new HttpHeaders();
      headers = headers.append('Content-Type', 'text/uri-list');
      options.headers = headers;
      const request = new PostRequest(requestUUID, endpointURLWithEmbed, sourceUri, options);

      scheduler.schedule(() => service.createFromExternalSource(sourceUri));
      scheduler.flush();

      expect((service as any).requestService.send).toHaveBeenCalledWith(request);
      expect((service as any).rdbService.buildFromRequestUUID).toHaveBeenCalledWith(requestUUID, followLink('item'));

    });
  });

  describe('updateByOrcidOperations', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();
      spyOn((service as any).patchData, 'patch').and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
    });

    it('should call patch method properly', () => {
      scheduler.schedule(() => service.patch(researcherProfile, []).subscribe());
      scheduler.flush();

      expect((service as any).patchData.patch).toHaveBeenCalledWith(researcherProfile, []);
    });
  });
});
