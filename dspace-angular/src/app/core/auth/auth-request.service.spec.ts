import { AuthRequestService } from './auth-request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { PostRequest } from '../data/request.models';
import { TestScheduler } from 'rxjs/testing';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { ShortLivedToken } from './models/short-lived-token.model';
import { RemoteData } from '../data/remote-data';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import objectContaining = jasmine.objectContaining;
import { AuthStatus } from './models/auth-status.model';
import { RestRequestMethod } from '../data/rest-request-method';
import { Observable, of as observableOf } from 'rxjs';

describe(`AuthRequestService`, () => {
  let halService: HALEndpointService;
  let endpointURL: string;
  let requestID: string;
  let shortLivedToken: ShortLivedToken;
  let shortLivedTokenRD: RemoteData<ShortLivedToken>;
  let requestService: RequestService;
  let rdbService: RemoteDataBuildService;
  let service;
  let testScheduler;

  const status = new AuthStatus();

  class TestAuthRequestService extends AuthRequestService {
    constructor(
      hes: HALEndpointService,
      rs: RequestService,
      rdbs: RemoteDataBuildService
    ) {
      super(hes, rs, rdbs);
    }

    protected createShortLivedTokenRequest(href: string): Observable<PostRequest> {
      return observableOf(new PostRequest(this.requestService.generateRequestId(), href));
    }
  }

  const init = (cold: typeof TestScheduler.prototype.createColdObservable) => {
    endpointURL = 'https://rest.api/auth';
    requestID = 'requestID';
    shortLivedToken = Object.assign(new ShortLivedToken(), {
      value: 'some-token'
    });
    shortLivedTokenRD = createSuccessfulRemoteDataObject(shortLivedToken);

    halService = jasmine.createSpyObj('halService', {
      'getEndpoint': cold('a', { a: endpointURL })
    });
    requestService = jasmine.createSpyObj('requestService', {
      'generateRequestId': requestID,
      'send': null,
    });
    rdbService = jasmine.createSpyObj('rdbService', {
      'buildFromRequestUUID': cold('a', { a: shortLivedTokenRD })
    });

    service = new TestAuthRequestService(halService, requestService, rdbService);

    spyOn(service as any, 'fetchRequest').and.returnValue(cold('a', { a: createSuccessfulRemoteDataObject(status) }));
  };

  beforeEach(() => {
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });
  });

  describe('REST request methods', () => {
    let options: HttpOptions;

    beforeEach(() => {
      options = Object.create({});
    });

    describe('GET', () => {
      it('should send a GET request to the right endpoint and return the auth status', () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          init(cold);

          expectObservable(service.getRequest('method', options)).toBe('a', {
            a: objectContaining({ payload: status }),
          });
          flush();

          expect(requestService.send).toHaveBeenCalledWith(objectContaining({
            uuid: requestID,
            href: endpointURL + '/method',
            method: RestRequestMethod.GET,
            body: undefined,
            options,
          }));
          expect((service as any).fetchRequest).toHaveBeenCalledWith(requestID);
        });
      });

      it('should send the request even if caller doesn\'t subscribe to the response', () => {
        testScheduler.run(({ cold, flush }) => {
          init(cold);

          service.getRequest('method', options);
          flush();

          expect(requestService.send).toHaveBeenCalledWith(objectContaining({
            uuid: requestID,
            href: endpointURL + '/method',
            method: RestRequestMethod.GET,
            body: undefined,
            options,
          }));
        });
      });
    });

    describe('POST', () => {
      it('should send a POST request to the right endpoint and return the auth status', () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          init(cold);

          expectObservable(service.postToEndpoint('method', { content: 'something' }, options)).toBe('a', {
            a: objectContaining({ payload: status }),
          });
          flush();

          expect(requestService.send).toHaveBeenCalledWith(objectContaining({
            uuid: requestID,
            href: endpointURL + '/method',
            method: RestRequestMethod.POST,
            body: { content: 'something' },
            options,
          }));
          expect((service as any).fetchRequest).toHaveBeenCalledWith(requestID);
        });
      });

      it('should send the request even if caller doesn\'t subscribe to the response', () => {
        testScheduler.run(({ cold, flush }) => {
          init(cold);

          service.postToEndpoint('method', { content: 'something' }, options);
          flush();

          expect(requestService.send).toHaveBeenCalledWith(objectContaining({
            uuid: requestID,
            href: endpointURL + '/method',
            method: RestRequestMethod.POST,
            body: { content: 'something' },
            options,
          }));
        });
      });
    });
  });

  describe(`getShortlivedToken`, () => {
    it(`should call createShortLivedTokenRequest with the url for the endpoint`, () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        init(cold);
        spyOn(service as any, 'createShortLivedTokenRequest');
        // expectObservable is needed to let testScheduler know to take it in to account, but since
        // we're not testing the outcome in this test, a .toBe(…) isn't necessary
        expectObservable(service.getShortlivedToken());
        flush();
        expect((service as any).createShortLivedTokenRequest).toHaveBeenCalledWith(`${endpointURL}/shortlivedtokens`);
      });
    });
  });
});
