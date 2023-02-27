import { ForwardClientIpInterceptor } from './forward-client-ip.interceptor';
import { DspaceRestService } from '../dspace-rest/dspace-rest.service';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { REQUEST } from '@nguniversal/express-engine/tokens';

describe('ForwardClientIpInterceptor', () => {
  let service: DspaceRestService;
  let httpMock: HttpTestingController;

  let requestUrl;
  let clientIp;

  beforeEach(() => {
    requestUrl = 'test-url';
    clientIp = '1.2.3.4';

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        DspaceRestService,
        {
          provide: HTTP_INTERCEPTORS,
          useClass: ForwardClientIpInterceptor,
          multi: true,
        },
        { provide: REQUEST, useValue: { get: () => undefined, connection: { remoteAddress: clientIp } } }
      ],
    });

    service = TestBed.inject(DspaceRestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should add an X-Forwarded-For header matching the client\'s IP', () => {
    service.get(requestUrl).subscribe((response) => {
      expect(response).toBeTruthy();
    });

    const httpRequest = httpMock.expectOne(requestUrl);
    expect(httpRequest.request.headers.get('X-Forwarded-For')).toEqual(clientIp);
  });
});
