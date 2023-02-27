import { FeedbackDataService } from './feedback-data.service';
import { HALLink } from '../shared/hal-link.model';
import { Item } from '../shared/item.model';
import { HALEndpointServiceStub } from '../../shared/testing/hal-endpoint-service.stub';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { Store } from '@ngrx/store';
import { Feedback } from './models/feedback.model';
import { CoreState } from '../core-state.model';
import { testCreateDataImplementation } from '../data/base/create-data.spec';

describe('FeedbackDataService', () => {
  let service: FeedbackDataService;
  let requestService;
  let halService;
  let rdbService;
  let notificationsService;
  let objectCache;
  let store;
  let item;
  let bundleLink;
  let bundleHALLink;

  const feedbackPayload = Object.assign(new Feedback(), {
    email: 'test@email.com',
    message: 'message',
    page: '/home'
  });


  function initTestService(): FeedbackDataService {
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
    objectCache = {

      addPatch: () => {
        /* empty */
      },
      getObjectBySelfLink: () => {
        /* empty */
      }
    } as any;
    store = {} as Store<CoreState>;
    return new FeedbackDataService(
      requestService,
      rdbService,
      store,
      objectCache,
      halService,
      notificationsService,
    );
  }


  beforeEach(() => {
    service = initTestService();
  });

  describe('composition', () => {
    const initService = () => new FeedbackDataService(null, null, null, null, null, null);
    testCreateDataImplementation(initService);
  });

  describe('getFeedback', () => {
    beforeEach(() => {
      spyOn(service, 'getFeedback');
      service.getFeedback('3');
    });

    it('should call getFeedback with the feedback link', () => {
      expect(service.getFeedback).toHaveBeenCalledWith('3');
    });
  });

});
