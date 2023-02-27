import { cold, getTestScheduler } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { RouterMock } from '../../shared/mocks/router.mock';
import { ResearcherProfile } from '../profile/model/researcher-profile.model';
import { Item } from '../shared/item.model';
import { AddOperation, RemoveOperation } from 'fast-json-patch';
import { ConfigurationProperty } from '../shared/configuration-property.model';
import { ConfigurationDataService } from '../data/configuration-data.service';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { NativeWindowRefMock } from '../../shared/mocks/mock-native-window-ref';
import { URLCombiner } from '../url-combiner/url-combiner';
import { OrcidAuthService } from './orcid-auth.service';
import { ResearcherProfileDataService } from '../profile/researcher-profile-data.service';

describe('OrcidAuthService', () => {
  let scheduler: TestScheduler;
  let service: OrcidAuthService;
  let serviceAsAny: any;

  let researcherProfileService: jasmine.SpyObj<ResearcherProfileDataService>;
  let configurationDataService: ConfigurationDataService;
  let nativeWindowService: NativeWindowRefMock;
  let routerStub: any;

  const researcherProfileId = 'beef9946-rt56-479e-8f11-b90cbe9f7241';
  const itemId = 'beef9946-rt56-479e-8f11-b90cbe9f7241';

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

  beforeEach(() => {
    scheduler = getTestScheduler();
    routerStub = new RouterMock();
    researcherProfileService = jasmine.createSpyObj('ResearcherProfileService', {
      findById: jasmine.createSpy('findById'),
      patch: jasmine.createSpy('patch'),
      updateByOrcidOperations: jasmine.createSpy('updateByOrcidOperations'),
    });
    configurationDataService = jasmine.createSpyObj('configurationDataService', {
      findByPropertyName: jasmine.createSpy('findByPropertyName')
    });
    nativeWindowService = new NativeWindowRefMock();

    service = new OrcidAuthService(
      nativeWindowService,
      configurationDataService,
      researcherProfileService,
      routerStub);

    serviceAsAny = service;
  });


  describe('isLinkedToOrcid', () => {
    it('should return true when item has metadata', () => {
      const result = service.isLinkedToOrcid(mockItemLinkedToOrcid);
      expect(result).toBeTrue();
    });

    it('should return true when item has no metadata', () => {
      const result = service.isLinkedToOrcid(mockItemUnlinkedToOrcid);
      expect(result).toBeFalse();
    });
  });

  describe('onlyAdminCanDisconnectProfileFromOrcid', () => {
    it('should return true when property is only_admin', () => {
      spyOn((service as any), 'getOrcidDisconnectionAllowedUsersConfiguration').and.returnValue(createSuccessfulRemoteDataObject$(disconnectionAllowAdmin));
      const result = service.onlyAdminCanDisconnectProfileFromOrcid();
      const expected = cold('(a|)', {
        a: true
      });
      expect(result).toBeObservable(expected);
    });

    it('should return false on faild', () => {
      spyOn((service as any), 'getOrcidDisconnectionAllowedUsersConfiguration').and.returnValue(createFailedRemoteDataObject$());
      const result = service.onlyAdminCanDisconnectProfileFromOrcid();
      const expected = cold('(a|)', {
        a: false
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('ownerCanDisconnectProfileFromOrcid', () => {
    it('should return true when property is admin_and_owner', () => {
      spyOn((service as any), 'getOrcidDisconnectionAllowedUsersConfiguration').and.returnValue(createSuccessfulRemoteDataObject$(disconnectionAllowAdminOwner));
      const result = service.ownerCanDisconnectProfileFromOrcid();
      const expected = cold('(a|)', {
        a: true
      });
      expect(result).toBeObservable(expected);
    });

    it('should return false on faild', () => {
      spyOn((service as any), 'getOrcidDisconnectionAllowedUsersConfiguration').and.returnValue(createFailedRemoteDataObject$());
      const result = service.ownerCanDisconnectProfileFromOrcid();
      const expected = cold('(a|)', {
        a: false
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('linkOrcidByItem', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();
      researcherProfileService.patch.and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
      researcherProfileService.findById.and.returnValue(createSuccessfulRemoteDataObject$(researcherProfile));
    });

    it('should call updateByOrcidOperations method properly', () => {
      const operations: AddOperation<string>[] = [{
        path: '/orcid',
        op: 'add',
        value: 'test-code'
      }];

      scheduler.schedule(() => service.linkOrcidByItem(mockItemUnlinkedToOrcid, 'test-code').subscribe());
      scheduler.flush();

      expect(researcherProfileService.patch).toHaveBeenCalledWith(researcherProfile, operations);
    });
  });

  describe('unlinkOrcidByItem', () => {
    beforeEach(() => {
      scheduler = getTestScheduler();
      researcherProfileService.patch.and.returnValue(createSuccessfulRemoteDataObject$(researcherProfilePatched));
      researcherProfileService.findById.and.returnValue(createSuccessfulRemoteDataObject$(researcherProfile));
    });

    it('should call updateByOrcidOperations method properly', () => {
      const operations: RemoveOperation[] = [{
        path: '/orcid',
        op: 'remove'
      }];

      scheduler.schedule(() => service.unlinkOrcidByItem(mockItemLinkedToOrcid).subscribe());
      scheduler.flush();

      expect(researcherProfileService.patch).toHaveBeenCalledWith(researcherProfile, operations);
    });
  });

  describe('getOrcidAuthorizeUrl', () => {
    beforeEach(() => {
      routerStub.setRoute('/entities/person/uuid/orcid');
      (service as any).configurationService.findByPropertyName.and.returnValues(
        createSuccessfulRemoteDataObject$(authorizeUrl),
        createSuccessfulRemoteDataObject$(appClientId),
        createSuccessfulRemoteDataObject$(orcidScope)
      );
    });

    it('should build the url properly', () => {
      const result = service.getOrcidAuthorizeUrl(mockItemUnlinkedToOrcid);
      const redirectUri: string = new URLCombiner(nativeWindowService.nativeWindow.origin, encodeURIComponent(routerStub.url.split('?')[0])).toString();
      const url = 'orcid.authorize-url?client_id=orcid.application-client-id&redirect_uri=' + redirectUri + '&response_type=code&scope=/authenticate /read-limited';

      const expected = cold('(a|)', {
        a: url
      });
      expect(result).toBeObservable(expected);
    });
  });

  describe('getOrcidAuthorizationScopesByItem', () => {
    it('should return list of scopes saved in the item', () => {
      const orcidScopes = [
        '/authenticate',
        '/read-limited',
        '/activities/update',
        '/person/update'
      ];
      const result = service.getOrcidAuthorizationScopesByItem(mockItemLinkedToOrcid);
      expect(result).toEqual(orcidScopes);
    });
  });

  describe('getOrcidAuthorizationScopes', () => {
    it('should return list of scopes by configuration', () => {
      (service as any).configurationService.findByPropertyName.and.returnValue(
        createSuccessfulRemoteDataObject$(orcidScope)
      );
      const orcidScopes = [
        '/authenticate',
        '/read-limited'
      ];
      const expected = cold('(a|)', {
        a: orcidScopes
      });
      const result = service.getOrcidAuthorizationScopes();
      expect(result).toBeObservable(expected);
    });
  });
});
