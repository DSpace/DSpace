import { RequestService } from './request.service';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { fakeAsync, tick } from '@angular/core/testing';
import { GetRequest } from './request.models';
import { ObjectCacheService } from '../cache/object-cache.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { Observable } from 'rxjs';
import { RemoteData } from './remote-data';
import { hasNoValue } from '../../shared/empty.util';
import { AccessStatusDataService } from './access-status-data.service';
import { Item } from '../shared/item.model';

const url = 'fake-url';

describe('AccessStatusDataService', () => {
  let service: AccessStatusDataService;
  let requestService: RequestService;
  let notificationsService: any;
  let rdbService: RemoteDataBuildService;
  let objectCache: ObjectCacheService;
  let halService: any;

  const itemId = '8b3c613a-5a4b-438b-9686-be1d5b4a1c5a';
  const mockItem: Item = Object.assign(new Item(), {
    id: itemId,
    name: 'test-item',
    _links: {
      accessStatus: {
        href: `https://rest.api/items/${itemId}/accessStatus`
      },
      self: {
        href: `https://rest.api/items/${itemId}`
      }
    }
  });

  describe('when the requests are successful', () => {
    beforeEach(() => {
      createService();
    });

    describe('when calling findAccessStatusFor', () => {
      let contentSource$;

      beforeEach(() => {
        contentSource$ = service.findAccessStatusFor(mockItem);
      });

      it('should send a new GetRequest', fakeAsync(() => {
        contentSource$.subscribe();
        tick();
        expect(requestService.send).toHaveBeenCalledWith(jasmine.any(GetRequest), true);
      }));
    });
  });

  /**
   * Create an AccessStatusDataService used for testing
   * @param reponse$   Supply a RemoteData to be returned by the REST API (optional)
   */
  function createService(reponse$?: Observable<RemoteData<any>>) {
    requestService = getMockRequestService();
    let buildResponse$ = reponse$;
    if (hasNoValue(reponse$)) {
      buildResponse$ = createSuccessfulRemoteDataObject$({});
    }
    rdbService = jasmine.createSpyObj('rdbService', {
      buildFromRequestUUID: buildResponse$,
      buildSingle: buildResponse$
    });
    objectCache = jasmine.createSpyObj('objectCache', {
      remove: jasmine.createSpy('remove')
    });
    halService = new HALEndpointServiceStub(url);
    notificationsService = new NotificationsServiceStub();
    service = new AccessStatusDataService(requestService, rdbService, objectCache, halService);
  }
});
