import { ItemTemplateDataService } from './item-template-data.service';
import { RestResponse } from '../cache/response.models';
import { RequestService } from './request.service';
import { Observable, of as observableOf } from 'rxjs';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { Store } from '@ngrx/store';
import { BrowseService } from '../browse/browse.service';
import { cold } from 'jasmine-marbles';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { CollectionDataService } from './collection-data.service';
import { RestRequestMethod } from './rest-request-method';
import { Item } from '../shared/item.model';
import { RestRequest } from './rest-request.model';
import { CoreState } from '../core-state.model';
import { RequestEntry } from './request-entry.model';
import { testCreateDataImplementation } from './base/create-data.spec';
import { testPatchDataImplementation } from './base/patch-data.spec';
import { testDeleteDataImplementation } from './base/delete-data.spec';
import createSpyObj = jasmine.createSpyObj;

describe('ItemTemplateDataService', () => {
  let service: ItemTemplateDataService;
  let byCollection: any;

  const item = new Item();
  const collectionEndpoint = 'https://rest.api/core/collections/4af28e99-6a9c-4036-a199-e1b587046d39';
  const itemEndpoint = `${collectionEndpoint}/itemtemplate`;
  const scopeID = '4af28e99-6a9c-4036-a199-e1b587046d39';
  const requestService = {
    generateRequestId(): string {
      return scopeID;
    },
    send(request: RestRequest) {
      // Do nothing
    },
    getByHref(requestHref: string) {
      const responseCacheEntry = new RequestEntry();
      responseCacheEntry.response = new RestResponse(true, 200, 'OK');
      return observableOf(responseCacheEntry);
    },
    getByUUID(uuid: string) {
      const responseCacheEntry = new RequestEntry();
      responseCacheEntry.response = new RestResponse(true, 200, 'OK');
      return observableOf(responseCacheEntry);
    },
    commit(method?: RestRequestMethod) {
      // Do nothing
    }
  } as RequestService;
  const rdbService = {} as RemoteDataBuildService;
  const store = {} as Store<CoreState>;
  const browseService = {} as BrowseService;
  const objectCache = {
    getObjectBySelfLink(self) {
      return observableOf({});
    },
    addPatch(self, operations) {
      // Do nothing
    },
  } as any;
  const halEndpointService = {
    getEndpoint(linkPath: string): Observable<string> {
      return cold('a', { a: itemEndpoint });
    }
  } as HALEndpointService;
  const notificationsService = {} as NotificationsService;
  const comparator = {
    diff(first, second) {
      return [{}];
    }
  } as any;
  const collectionService = {
    getIDHrefObs(id): Observable<string> {
      return observableOf(collectionEndpoint);
    }
  } as CollectionDataService;

  function initTestService() {
    service = new ItemTemplateDataService(
      requestService,
      rdbService,
      objectCache,
      halEndpointService,
      notificationsService,
      comparator,
      browseService,
      undefined,
      collectionService,
    );
    byCollection = (service as any).byCollection;
  }

  beforeEach(() => {
    initTestService();
  });

  describe('composition', () => {
    const initService = () => new ItemTemplateDataService(null, null, null, null, null, null, null, null, null);
    testCreateDataImplementation(initService);
    testPatchDataImplementation(initService);
    testDeleteDataImplementation(initService);
  });

  describe('findByCollectionID', () => {
    it('should call findByCollectionID on the collection-based data service', () => {
      spyOn(byCollection, 'findById');
      service.findByCollectionID(scopeID);
      expect(byCollection.findById).toHaveBeenCalled();
    });
  });

  describe('createByCollectionID', () => {
    it('should call createTemplate on the collection-based data service', () => {
      spyOn(byCollection, 'createTemplate');
      service.createByCollectionID(item, scopeID);
      expect(byCollection.createTemplate).toHaveBeenCalledWith(item, scopeID);
    });
  });

  describe('byCollection', () => {
    beforeEach(() => {
      byCollection.createData = createSpyObj('createData', {
        createOnEndpoint: 'TEST createOnEndpoint',
      });
    });

    describe('getIDHrefObs', () => {
      it('should point to the Item template of a given Collection', () => {
        expect(byCollection.getIDHrefObs(scopeID)).toBeObservable(cold('a', { a: jasmine.stringMatching(`/collections/${scopeID}/itemtemplate`) }));
      });
    });

    describe('createTemplate', () => {
      it('should forward to CreateDataImpl.createOnEndpoint', () => {
        spyOn(byCollection, 'getIDHrefObs').and.returnValue('TEST getIDHrefObs');

        const out = byCollection.createTemplate(item, scopeID);

        expect(byCollection.getIDHrefObs).toHaveBeenCalledWith(scopeID);
        expect(byCollection.createData.createOnEndpoint).toHaveBeenCalledWith(item, 'TEST getIDHrefObs');
        expect(out).toBe('TEST createOnEndpoint');
      });
    });
  });
});

