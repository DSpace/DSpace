import { HttpClient } from '@angular/common/http';
import { Store } from '@ngrx/store';
import { compare, Operation } from 'fast-json-patch';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { Item } from '../shared/item.model';
import { ChangeAnalyzer } from './change-analyzer';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { BundleDataService } from './bundle-data.service';
import { HALLink } from '../shared/hal-link.model';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { Bundle } from '../shared/bundle.model';
import { CoreState } from '../core-state.model';
import { testPatchDataImplementation } from './base/patch-data.spec';

class DummyChangeAnalyzer implements ChangeAnalyzer<Item> {
  diff(object1: Item, object2: Item): Operation[] {
    return compare((object1 as any).metadata, (object2 as any).metadata);
  }
}

describe('BundleDataService', () => {
  let service: BundleDataService;
  let requestService;
  let halService;
  let rdbService;
  let notificationsService;
  let http;
  let comparator;
  let objectCache;
  let store;
  let item;
  let bundleLink;
  let bundleHALLink;

  function initTestService(): BundleDataService {
    bundleLink = '/items/0fdc0cd7-ff8c-433d-b33c-9b56108abc07/bundles';
    bundleHALLink = new HALLink();
    bundleHALLink.href = bundleLink;
    item = new Item();
    item._links = {
      bundles: bundleHALLink
    };
    requestService = getMockRequestService();
    halService = new HALEndpointServiceStub('url') as any;
    rdbService = {} as RemoteDataBuildService;
    notificationsService = {} as NotificationsService;
    http = {} as HttpClient;
    comparator = new DummyChangeAnalyzer() as any;
    objectCache = {

      addPatch: () => {
        /* empty */
      },
      getObjectBySelfLink: () => {
        /* empty */
      }
    } as any;
    store = {} as Store<CoreState>;
    return new BundleDataService(
      requestService,
      rdbService,
      store,
      objectCache,
      halService,
    );
  }

  beforeEach(() => {
    service = initTestService();
  });

  describe('composition', () => {
    const initService = () => new BundleDataService(null, null, null, null, null);

    testPatchDataImplementation(initService);
  });

  describe('findAllByItem', () => {
    beforeEach(() => {
      spyOn(service, 'findListByHref');
      service.findAllByItem(item);
    });

    it('should call findListByHref with the item\'s bundles link', () => {
      expect(service.findListByHref).toHaveBeenCalledWith(bundleLink, undefined, true, true);
    });
  });

  describe('findByItemAndName', () => {
    let bundles: Bundle[];

    beforeEach(() => {
      bundles = [
        Object.assign(new Bundle(), {
          id: 'ORIGINAL_BUNDLE',
          metadata: {
            'dc.title': [
              {
                value: 'ORIGINAL'
              }
            ]
          }
        }),
        Object.assign(new Bundle(), {
          id: 'THUMBNAIL_BUNDLE',
          metadata: {
            'dc.title': [
              {
                value: 'THUMBNAIL'
              }
            ]
          }
        }),
        Object.assign(new Bundle(), {
          id: 'EXTRA_BUNDLE',
          metadata: {
            'dc.title': [
              {
                value: 'EXTRA'
              }
            ]
          }
        }),
      ];
      spyOn(service, 'findAllByItem').and.returnValue(createSuccessfulRemoteDataObject$(createPaginatedList(bundles)));
    });

    it('should only return the requested bundle by name', (done) => {
      service.findByItemAndName(undefined, 'THUMBNAIL').subscribe((result) => {
        expect(result.payload.id).toEqual('THUMBNAIL_BUNDLE');
        done();
      });
    });
  });
});
