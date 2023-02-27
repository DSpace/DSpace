import { BitstreamFormatDataService } from './bitstream-format-data.service';
import { RestResponse } from '../cache/response.models';
import { Observable, of as observableOf } from 'rxjs';
import { Action, Store } from '@ngrx/store';
import { ObjectCacheService } from '../cache/object-cache.service';
import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { BitstreamFormat } from '../shared/bitstream-format.model';
import { waitForAsync } from '@angular/core/testing';
import { BitstreamFormatsRegistryDeselectAction, BitstreamFormatsRegistryDeselectAllAction, BitstreamFormatsRegistrySelectAction } from '../../admin/admin-registries/bitstream-formats/bitstream-format.actions';
import { TestScheduler } from 'rxjs/testing';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { CoreState } from '../core-state.model';
import { RequestEntry } from './request-entry.model';
import { testFindAllDataImplementation } from './base/find-all-data.spec';
import { testDeleteDataImplementation } from './base/delete-data.spec';

describe('BitstreamFormatDataService', () => {
  let service: BitstreamFormatDataService;
  let requestService;
  let scheduler: TestScheduler;

  const bitstreamFormatsEndpoint = 'https://rest.api/core/bitstream-formats';
  const bitstreamFormatsIdEndpoint = 'https://rest.api/core/bitstream-formats/format-id';

  const responseCacheEntry = new RequestEntry();
  responseCacheEntry.response = new RestResponse(true, 200, 'Success');

  const store = {
    dispatch(action: Action) {
      // Do Nothing
    }
  } as Store<CoreState>;

  const requestUUIDs = ['some', 'uuid'];

  const objectCache = jasmine.createSpyObj('objectCache', {
    getByHref: observableOf({ requestUUIDs })
  }) as ObjectCacheService;

  const halEndpointService = {
    getEndpoint(linkPath: string): Observable<string> {
      return cold('a', { a: bitstreamFormatsEndpoint });
    }
  } as HALEndpointService;

  const notificationsService = {} as NotificationsService;

  let rd;
  let rdbService: RemoteDataBuildService;

  function initTestService(halService) {
    rd = createSuccessfulRemoteDataObject({});
    rdbService = jasmine.createSpyObj('rdbService', {
      buildFromRequestUUID: observableOf(rd),
      buildFromRequestUUIDAndAwait: observableOf(rd),
    });

    return new BitstreamFormatDataService(
      requestService,
      rdbService,
      objectCache,
      halService,
      notificationsService,
      store,
    );
  }

  describe('composition', () => {
    const initService = () => new BitstreamFormatDataService(null, null, null, null, null, null);
    testFindAllDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });

  describe('getBrowseEndpoint', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
    }));
    it('should get the browse endpoint', () => {
      const result = service.getBrowseEndpoint();
      const expected = cold('b', { b: bitstreamFormatsEndpoint });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getUpdateEndpoint', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
    }));
    it('should get the update endpoint', () => {
      const formatId = 'format-id';

      const result = service.getUpdateEndpoint(formatId);
      const expected = cold('b', { b: bitstreamFormatsIdEndpoint });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getCreateEndpoint', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
    }));
    it('should get the create endpoint ', () => {

      const result = service.getCreateEndpoint();
      const expected = cold('b', { b: bitstreamFormatsEndpoint });

      expect(result).toBeObservable(expected);
    });
  });

  describe('updateBitstreamFormat', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
    }));
    it('should update the bitstream format', () => {
      const updatedBistreamFormat = new BitstreamFormat();
      updatedBistreamFormat.uuid = 'updated-uuid';

      const expected = cold('(b|)', { b: rd });
      const result = service.updateBitstreamFormat(updatedBistreamFormat);

      expect(result).toBeObservable(expected);

    });
  });

  describe('createBitstreamFormat', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
    }));
    it('should create a new bitstream format', () => {
      const newFormat = new BitstreamFormat();
      newFormat.uuid = 'new-uuid';

      const expected = cold('(b|)', { b: rd });
      const result = service.createBitstreamFormat(newFormat);

      expect(result).toBeObservable(expected);
    });
  });

  describe('clearBitStreamFormatRequests', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      const halService = {
        getEndpoint(linkPath: string): Observable<string> {
          return observableOf(bitstreamFormatsEndpoint);
        }
      } as HALEndpointService;
      service = initTestService(halService);
      service.clearBitStreamFormatRequests().subscribe();
    }));
    it('should remove the bitstream format hrefs in the request service', () => {
      expect(requestService.removeByHrefSubstring).toHaveBeenCalledWith(bitstreamFormatsEndpoint);
    });
  });

  describe('selectBitstreamFormat', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
      spyOn(store, 'dispatch');
    }));
    it('should add a selected bitstream to the store', () => {
      const format = new BitstreamFormat();
      format.uuid = 'uuid';

      service.selectBitstreamFormat(format);
      expect(store.dispatch).toHaveBeenCalledWith(new BitstreamFormatsRegistrySelectAction(format));
    });
  });

  describe('deselectBitstreamFormat', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
      spyOn(store, 'dispatch');
    }));
    it('should remove a bitstream from the store', () => {
      const format = new BitstreamFormat();
      format.uuid = 'uuid';

      service.deselectBitstreamFormat(format);
      expect(store.dispatch).toHaveBeenCalledWith(new BitstreamFormatsRegistryDeselectAction(format));
    });
  });

  describe('deselectAllBitstreamFormats', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: cold('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      service = initTestService(halEndpointService);
      spyOn(store, 'dispatch');

    }));
    it('should remove all bitstreamFormats from the store', () => {
      service.deselectAllBitstreamFormats();
      expect(store.dispatch).toHaveBeenCalledWith(new BitstreamFormatsRegistryDeselectAllAction());
    });
  });

  describe('delete', () => {
    beforeEach(waitForAsync(() => {
      scheduler = getTestScheduler();
      requestService = jasmine.createSpyObj('requestService', {
        send: {},
        getByHref: observableOf(responseCacheEntry),
        getByUUID: hot('a', { a: responseCacheEntry }),
        setStaleByUUID: observableOf(true),
        generateRequestId: 'request-id',
        removeByHrefSubstring: {}
      });
      const halService = {
        getEndpoint(linkPath: string): Observable<string> {
          return observableOf(bitstreamFormatsEndpoint);
        }
      } as HALEndpointService;
      service = initTestService(halService);
    }));
    it('should delete a bitstream format', () => {
      const format = new BitstreamFormat();
      format.uuid = 'format-uuid';
      format.id = 'format-id';

      const expected = cold('(b|)', { b: rd });
      const result = service.delete(format.id);

      expect(result).toBeObservable(expected);
    });
  });
});
