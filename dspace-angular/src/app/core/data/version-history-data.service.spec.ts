import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { VersionHistoryDataService } from './version-history-data.service';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { VersionDataService } from './version-data.service';
import { fakeAsync, waitForAsync } from '@angular/core/testing';
import { VersionHistory } from '../shared/version-history.model';
import { Version } from '../shared/version.model';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { Item } from '../shared/item.model';
import { of } from 'rxjs';
import SpyObj = jasmine.SpyObj;

const url = 'fake-url';

describe('VersionHistoryDataService', () => {
  let service: VersionHistoryDataService;

  let requestService: RequestService;
  let notificationsService: any;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let versionService: SpyObj<VersionDataService>;
  let halService: any;

  const versionHistoryId = 'version-history-id';
  const versionHistoryDraftId = 'version-history-draft-id';
  const version1Id = 'version-1-id';
  const version2Id = 'version-1-id';
  const item1Uuid = 'item-1-uuid';
  const item2Uuid = 'item-2-uuid';
  const versionHistory = Object.assign(new VersionHistory(), {
    id: versionHistoryId,
    draftVersion: false,
  });
  const versionHistoryDraft = Object.assign(new VersionHistory(), {
    id: versionHistoryDraftId,
    draftVersion: true,
  });
  const version1 = Object.assign(new Version(), {
    id: version1Id,
    version: 1,
    created: new Date(2020, 1, 1),
    summary: 'first version',
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory),
    _links: {
      self: {
        href: 'version1-url',
      },
    },
  });
  const version2 = Object.assign(new Version(), {
    id: version2Id,
    version: 2,
    summary: 'second version',
    created: new Date(2020, 1, 2),
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory),
    _links: {
      self: {
        href: 'version2-url',
      },
    },
  });
  const versions = [version1, version2];
  versionHistory.versions = createSuccessfulRemoteDataObject$(createPaginatedList(versions));
  const item1 = Object.assign(new Item(), {
    uuid: item1Uuid,
    handle: '123456789/1',
    version: createSuccessfulRemoteDataObject$(version1),
    _links: {
      self: {
        href: '/items/' + item2Uuid,
      }
    }
  });
  const item2 = Object.assign(new Item(), {
    uuid: item2Uuid,
    handle: '123456789/2',
    version: createSuccessfulRemoteDataObject$(version2),
    _links: {
      self: {
        href: '/items/' + item2Uuid,
      }
    }
  });
  const items = [item1, item2];
  version1.item = createSuccessfulRemoteDataObject$(item1);
  version2.item = createSuccessfulRemoteDataObject$(item2);

  /**
   * Create a VersionHistoryDataService used for testing
   * @param requestEntry$   Supply a requestEntry to be returned by the REST API (optional)
   */
  function createService(requestEntry$?) {
    requestService = getMockRequestService(requestEntry$);
    rdbService = jasmine.createSpyObj('rdbService', {
      buildList: jasmine.createSpy('buildList'),
      buildFromRequestUUID: jasmine.createSpy('buildFromRequestUUID'),
    });
    objectCache = jasmine.createSpyObj('objectCache', {
      remove: jasmine.createSpy('remove'),
    });
    versionService = jasmine.createSpyObj('objectCache', {
      findByHref: jasmine.createSpy('findByHref'),
      findListByHref: jasmine.createSpy('findListByHref'),
      getHistoryFromVersion: jasmine.createSpy('getHistoryFromVersion'),
    });
    halService = new HALEndpointServiceStub(url);
    notificationsService = new NotificationsServiceStub();

    service = new VersionHistoryDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
      versionService,
    );
  }

  beforeEach(() => {
    createService();
  });

  describe('getVersions', () => {
    let result;

    beforeEach(() => {
      result = service.getVersions('1');
    });

    it('should call versionService.findListByHref', () => {
      expect(versionService.findListByHref).toHaveBeenCalled();
    });
  });

  describe('when getVersions is called', () => {
    beforeEach(waitForAsync(() => {
      service.getVersions(versionHistoryId);
    }));
    it('findListByHref should have been called', () => {
      expect(versionService.findListByHref).toHaveBeenCalled();
    });
  });

  describe('when getBrowseEndpoint is called', () => {
    it('should return the correct value', () => {
      service.getBrowseEndpoint().subscribe((res) => {
        expect(res).toBe(url + '/versionhistories');
      });
    });
  });

  describe('when getVersionsEndpoint is called', () => {
    it('should return the correct value', () => {
      service.getVersionsEndpoint(versionHistoryId).subscribe((res) => {
        expect(res).toBe(url + '/versionhistories/version-history-id/versions');
      });
    });
  });

  describe('when cache is invalidated', () => {
    it('should call setStaleByHrefSubstring', () => {
      service.invalidateVersionHistoryCache(versionHistoryId);
      expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('versioning/versionhistories/' + versionHistoryId);
    });
  });

  describe('isLatest$', () => {
    beforeEach(waitForAsync(() => {
      spyOn(service, 'getLatestVersion$').and.returnValue(of(version2));
    }));
    it('should return false for version1', () => {
      service.isLatest$(version1).subscribe((res) => {
        expect(res).toBe(false);
      });
    });
    it('should return true for version2', () => {
      service.isLatest$(version2).subscribe((res) => {
        expect(res).toBe(true);
      });
    });
  });

  describe('hasDraftVersion$', () => {
    beforeEach(waitForAsync(() => {
      versionService.findByHref.and.returnValue(createSuccessfulRemoteDataObject$<Version>(version1));
    }));
    it('should return false if draftVersion is false', fakeAsync(() => {
      versionService.getHistoryFromVersion.and.returnValue(of(versionHistory));
      service.hasDraftVersion$('href').subscribe((res) => {
        expect(res).toBeFalse();
      });
    }));
    it('should return true if draftVersion is true', fakeAsync(() => {
      versionService.getHistoryFromVersion.and.returnValue(of(versionHistoryDraft));
      service.hasDraftVersion$('href').subscribe((res) => {
        expect(res).toBeTrue();
      });
    }));
  });

});
