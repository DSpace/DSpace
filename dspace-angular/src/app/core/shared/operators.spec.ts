import { cold, getTestScheduler, hot } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { getMockRequestService } from '../../shared/mocks/request.service.mock';
import { GetRequest } from '../data/request.models';
import { RequestService } from '../data/request.service';
import {
  getAllSucceededRemoteData,
  getFirstSucceededRemoteData,
  getRemoteDataPayload
} from './operators';
import { of as observableOf } from 'rxjs';
import {
  createFailedRemoteDataObject,
  createSuccessfulRemoteDataObject
} from '../../shared/remote-data.utils';
import { getRequestFromRequestHref, getRequestFromRequestUUID, getResponseFromEntry, sendRequest } from './request.operators';
import { redirectOn4xx } from './authorized.operators';
import { RequestEntry } from '../data/request-entry.model';

/* eslint-disable @typescript-eslint/no-shadow */

describe('Core Module - RxJS Operators', () => {
  let scheduler: TestScheduler;
  let requestService: RequestService;
  const testRequestHref = 'https://rest.api/';
  const testRequestUUID = 'https://rest.api/';

  const testRCEs = {
    a: { response: { isSuccessful: true, resourceSelfLinks: ['a', 'b', 'c', 'd'] } },
    b: { response: { isSuccessful: false, resourceSelfLinks: ['e', 'f'] } },
    c: { response: { isSuccessful: undefined, resourceSelfLinks: ['g', 'h', 'i'] } },
    d: { response: { isSuccessful: true, resourceSelfLinks: ['j', 'k', 'l', 'm', 'n'] } },
    e: { response: { isSuccessful: 1, resourceSelfLinks: [] } },
    f: { response: undefined },
    g: undefined
  };

  const testResponses = {
    a: testRCEs.a.response,
    b: testRCEs.b.response,
    c: testRCEs.c.response,
    d: testRCEs.d.response,
    e: testRCEs.e.response
  };

  beforeEach(() => {
    scheduler = getTestScheduler();
  });

  describe('getRequestFromRequestHref', () => {

    it('should return the RequestEntry corresponding to the self link in the source', () => {
      requestService = getMockRequestService();

      const source = hot('a', { a: testRequestHref });
      const result = source.pipe(getRequestFromRequestHref(requestService));
      const expected = cold('a', { a: new RequestEntry() });

      expect(result).toBeObservable(expected);
    });

    it('should use the requestService to fetch the request by its self link', () => {
      requestService = getMockRequestService();

      const source = hot('a', { a: testRequestHref });
      scheduler.schedule(() => source.pipe(getRequestFromRequestHref(requestService)).subscribe());
      scheduler.flush();

      expect(requestService.getByHref).toHaveBeenCalledWith(testRequestHref);
    });

    it('shouldn\'t return anything if there is no request matching the self link', () => {
      requestService = getMockRequestService(cold('a', { a: undefined }));

      const source = hot('a', { a: testRequestUUID });
      const result = source.pipe(getRequestFromRequestHref(requestService));
      const expected = cold('-');

      expect(result).toBeObservable(expected);
    });
  });

  describe('getRequestFromRequestUUID', () => {

    it('should return the RequestEntry corresponding to the request uuid in the source', () => {
      requestService = getMockRequestService();

      const source = hot('a', { a: testRequestUUID });
      const result = source.pipe(getRequestFromRequestUUID(requestService));
      const expected = cold('a', { a: new RequestEntry() });

      expect(result).toBeObservable(expected);
    });

    it('should use the requestService to fetch the request by its request uuid', () => {
      requestService = getMockRequestService();

      const source = hot('a', { a: testRequestUUID });
      scheduler.schedule(() => source.pipe(getRequestFromRequestUUID(requestService)).subscribe());
      scheduler.flush();

      expect(requestService.getByUUID).toHaveBeenCalledWith(testRequestUUID);
    });

    it('shouldn\'t return anything if there is no request matching the request uuid', () => {
      requestService = getMockRequestService(cold('a', { a: undefined }));

      const source = hot('a', { a: testRequestUUID });
      const result = source.pipe(getRequestFromRequestUUID(requestService));
      const expected = cold('-');

      expect(result).toBeObservable(expected);
    });
  });

  describe('sendRequest', () => {
    it('should call requestService.send with the source request', () => {
      requestService = getMockRequestService();
      const testRequest = new GetRequest('6b789e31-f026-4ff8-8993-4eb3b730c841', testRequestHref);
      const source = hot('a', { a: testRequest });
      scheduler.schedule(() => source.pipe(sendRequest(requestService)).subscribe());
      scheduler.flush();

      expect(requestService.send).toHaveBeenCalledWith(testRequest);
    });
  });

  describe('getRemoteDataPayload', () => {
    it('should return the payload of the source RemoteData', () => {
      const testRD = { a: { payload: 'a' } };
      const source = hot('a', testRD);
      const result = source.pipe(getRemoteDataPayload());
      const expected = cold('a', {
        a: testRD.a.payload,
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getResponseFromEntry', () => {
    it('should return the response for all not empty request entries, when they have a value', () => {
      const source = hot('abcdefg', testRCEs);
      const result = source.pipe(getResponseFromEntry());
      const expected = cold('abcde--', {
        a: testRCEs.a.response,
        b: testRCEs.b.response,
        c: testRCEs.c.response,
        d: testRCEs.d.response,
        e: testRCEs.e.response
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getFirstSucceededRemoteData', () => {
    it('should return the first() hasSucceeded RemoteData Observable', () => {
      const testRD = {
        a: createFailedRemoteDataObject(),
        b: createFailedRemoteDataObject(),
        c: createSuccessfulRemoteDataObject('c'),
        d: createSuccessfulRemoteDataObject('d'),
      };
      const source = hot('abcd', testRD);
      const result = source.pipe(getFirstSucceededRemoteData());
      const expected = cold('--(c|)', testRD);

      expect(result).toBeObservable(expected);

    });
  });

  describe('redirectOn4xx', () => {
    let router;
    let authService;
    let testScheduler;

    beforeEach(() => {
      testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });
      router = jasmine.createSpyObj('router', ['navigateByUrl']);
      authService = jasmine.createSpyObj('authService', {
        isAuthenticated: observableOf(true),
        setRedirectUrl: {}
      });
    });

    it('should call navigateByUrl to a 404 page, when the remote data contains a 404 error, and not emit anything', () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        const testRD = createFailedRemoteDataObject('Object was not found', 404);
        const source = cold('a', { a: testRD });
        const expected = '-';
        const values = {};

        expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
        flush();
        expect(router.navigateByUrl).toHaveBeenCalledWith('/404', { skipLocationChange: true });
      });
    });

    it('should call navigateByUrl to a 404 page, when the remote data contains a 422 error, and not emit anything', () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        const testRD = createFailedRemoteDataObject('Unprocessable Entity', 422);
        const source = cold('a', { a: testRD });
        const expected = '-';
        const values = {};

        expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
        flush();
        expect(router.navigateByUrl).toHaveBeenCalledWith('/404', { skipLocationChange: true });
      });
    });

    it('should call navigateByUrl to a 401 page, when the remote data contains a 403 error, and not emit anything', () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        const testRD = createFailedRemoteDataObject('Forbidden', 403);
        const source = cold('a', { a: testRD });
        const expected = '-';
        const values = {};

        expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
        flush();
        expect(router.navigateByUrl).toHaveBeenCalledWith('/403', { skipLocationChange: true });
      });
    });

    it('should not call navigateByUrl to a 404, 403 or 401 page, when the remote data contains another error than a 404, 422, 403 or 401, and emit the source rd', () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        const testRD = createFailedRemoteDataObject('Something went wrong', 500);
        const source = cold('a', { a: testRD });
        const expected = 'a';
        const values = { a: testRD };

        expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
        flush();
        expect(router.navigateByUrl).not.toHaveBeenCalled();
      });
    });

    it('should not call navigateByUrl to a 404, 403 or 401 page, when the remote data contains no error, and emit the source rd', () => {
      testScheduler.run(({ cold, expectObservable, flush }) => {
        const testRD = createSuccessfulRemoteDataObject(undefined);
        const source = cold('a', { a: testRD });
        const expected = 'a';
        const values = { a: testRD };

        expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
        flush();
        expect(router.navigateByUrl).not.toHaveBeenCalled();
      });
    });

    describe('when the user is not authenticated', () => {
      beforeEach(() => {
        (authService.isAuthenticated as jasmine.Spy).and.returnValue(observableOf(false));
      });

      it('should set the redirect url and navigate to login when the remote data contains a 401 error, and not emit anything', () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          const testRD = createFailedRemoteDataObject('The current user is unauthorized', 401);
          const source = cold('a', { a: testRD });
          const expected = '-';
          const values = {};

          expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
          flush();
          expect(authService.setRedirectUrl).toHaveBeenCalled();
          expect(router.navigateByUrl).toHaveBeenCalledWith('login');
        });
      });

      it('should set the redirect url and navigate to login when the remote data contains a 403 error, and not emit anything', () => {
        testScheduler.run(({ cold, expectObservable, flush }) => {
          const testRD = createFailedRemoteDataObject('Forbidden', 403);
          const source = cold('a', { a: testRD });
          const expected = '-';
          const values = {};

          expectObservable(source.pipe(redirectOn4xx(router, authService))).toBe(expected, values);
          flush();
          expect(authService.setRedirectUrl).toHaveBeenCalled();
          expect(router.navigateByUrl).toHaveBeenCalledWith('login');
        });
      });
    });
  });

  describe('getResponseFromEntry', () => {
    it('should return the response for all not empty request entries, when they have a value', () => {
      const source = hot('abcdefg', testRCEs);
      const result = source.pipe(getResponseFromEntry());
      const expected = cold('abcde--', {
        a: testRCEs.a.response,
        b: testRCEs.b.response,
        c: testRCEs.c.response,
        d: testRCEs.d.response,
        e: testRCEs.e.response
      });

      expect(result).toBeObservable(expected);
    });
  });

  describe('getAllSucceededRemoteData', () => {
    it('should return all hasSucceeded RemoteData Observables', () => {
      const testRD = {
        a: createFailedRemoteDataObject(),
        b: createFailedRemoteDataObject(),
        c: createSuccessfulRemoteDataObject('c'),
        d: createSuccessfulRemoteDataObject('d'),
      };
      const source = hot('abcd', testRD);
      const result = source.pipe(getAllSucceededRemoteData());
      const expected = cold('--cd', testRD);

      expect(result).toBeObservable(expected);

    });

  });
});
