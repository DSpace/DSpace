import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { of as observableOf } from 'rxjs';
import { RestRequestMethod } from '../../../core/data/rest-request-method';
import { EndpointMockingRestService } from './endpoint-mocking-rest.service';
import { ResponseMapMock } from './mocks/response-map.mock';

describe('EndpointMockingRestService', () => {
  let service: EndpointMockingRestService;

  let serverHttpResponse: HttpResponse<any>;

  let mockResponseMap: ResponseMapMock;

  beforeEach(() => {
    serverHttpResponse = {
      body: { bar: false },
      headers: new HttpHeaders(),
      statusText: '200'
    } as HttpResponse<any>;

    mockResponseMap = new Map([
      ['/foo', { bar: true }]
    ]);

    const httpStub = jasmine.createSpyObj('http', {
      get: observableOf(serverHttpResponse),
      request: observableOf(serverHttpResponse)
    });

    service = new EndpointMockingRestService(mockResponseMap, httpStub);
  });

  describe('get', () => {
    describe('when the URL is mocked', () => {
      it('should return the mock data', (done) => {
        service.get('https://rest.com/api/foo').subscribe((response) => {
          expect(response.payload).toEqual({ bar: true });
          done();
        });
      });
    });

    describe('when the URL isn\'t mocked', () => {
      it('should return the server data', (done) => {
        service.get('https://rest.com/api').subscribe((response) => {
          expect(response.payload).toEqual({ bar: false });
          done();
        });
      });
    });
  });

  describe('request', () => {
    describe('when the URL is mocked', () => {
      it('should return the mock data', (done) => {
        service.request(RestRequestMethod.GET, 'https://rest.com/api/foo').subscribe((response) => {
          expect(response.payload).toEqual({ bar: true });
          done();
        });
      });
    });

    describe('when the URL isn\'t mocked', () => {
      it('should return the server data', (done) => {
        service.request(RestRequestMethod.GET, 'https://rest.com/api').subscribe((response) => {
          expect(response.payload).toEqual({ bar: false });
          done();
        });
      });
    });
  });
});
