import { AuthRequestService } from './auth-request.service';
import { RequestService } from '../data/request.service';
import { ServerAuthRequestService } from './server-auth-request.service';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Observable, of as observableOf } from 'rxjs';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { PostRequest } from '../data/request.models';
import {
  XSRF_REQUEST_HEADER,
  XSRF_RESPONSE_HEADER
} from '../xsrf/xsrf.interceptor';

describe(`ServerAuthRequestService`, () => {
  let href: string;
  let requestService: RequestService;
  let service: AuthRequestService;
  let httpClient: HttpClient;
  let httpResponse: HttpResponse<any>;
  let halService: HALEndpointService;
  const mockToken = 'mock-token';

  beforeEach(() => {
    href = 'https://rest.api/auth/shortlivedtokens';
    requestService = jasmine.createSpyObj('requestService', {
      'generateRequestId': '8bb0582d-5013-4337-af9c-763beb25aae2'
    });
    let headers = new HttpHeaders();
    headers = headers.set(XSRF_RESPONSE_HEADER, mockToken);
    httpResponse = {
      body: { bar: false },
      headers: headers,
      statusText: '200'
    } as HttpResponse<any>;
    httpClient = jasmine.createSpyObj('httpClient', {
      get: observableOf(httpResponse),
    });
    halService = jasmine.createSpyObj('halService', {
      'getRootHref': '/api'
    });
    service = new ServerAuthRequestService(halService, requestService, null, httpClient);
  });

  describe(`createShortLivedTokenRequest`, () => {
    it(`should return a PostRequest`, (done) => {
      const obs = (service as any).createShortLivedTokenRequest(href) as Observable<PostRequest>;
      obs.subscribe((result: PostRequest) => {
        expect(result.constructor.name).toBe('PostRequest');
        done();
      });
    });

    it(`should return a request with the given href`, (done) => {
      const obs = (service as any).createShortLivedTokenRequest(href) as Observable<PostRequest>;
      obs.subscribe((result: PostRequest) => {
        expect(result.href).toBe(href);
        done();
      });
    });

    it(`should return a request with a xsrf header`, (done) => {
      const obs = (service as any).createShortLivedTokenRequest(href) as Observable<PostRequest>;
      obs.subscribe((result: PostRequest) => {
        expect(result.options.headers.get(XSRF_REQUEST_HEADER)).toBe(mockToken);
        done();
      });
    });
  });
});
