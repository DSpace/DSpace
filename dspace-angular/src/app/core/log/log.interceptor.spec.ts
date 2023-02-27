import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { Router } from '@angular/router';

import { LogInterceptor } from './log.interceptor';
import { DspaceRestService } from '../dspace-rest/dspace-rest.service';
import { RestRequestMethod } from '../data/rest-request-method';
import { CookieService } from '../services/cookie.service';
import { CookieServiceMock } from '../../shared/mocks/cookie.service.mock';
import { RouterStub } from '../../shared/testing/router.stub';
import { CorrelationIdService } from '../../correlation-id/correlation-id.service';
import { UUIDService } from '../shared/uuid.service';
import { StoreModule } from '@ngrx/store';
import { appReducers, storeModuleConfig } from '../../app.reducer';


describe('LogInterceptor', () => {
  let service: DspaceRestService;
  let httpMock: HttpTestingController;
  let cookieService: CookieService;
  let correlationIdService: CorrelationIdService;
  const router = Object.assign(new RouterStub(),{url : '/statistics'});

  // Mock payload/statuses are dummy content as we are not testing the results
  // of any below requests. We are only testing for X-XSRF-TOKEN header.
  const mockPayload = {
    id: 1
  };
  const mockStatusCode = 200;
  const mockStatusText = 'SUCCESS';


  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        StoreModule.forRoot(appReducers, storeModuleConfig),
      ],
      providers: [
        DspaceRestService,
        // LogInterceptor,
        {
          provide: HTTP_INTERCEPTORS,
          useClass: LogInterceptor,
          multi: true,
        },
        { provide: CookieService, useValue: new CookieServiceMock() },
        { provide: Router, useValue: router },
        { provide: CorrelationIdService, useClass: CorrelationIdService },
        { provide: UUIDService, useClass: UUIDService },
      ],
    });

    service = TestBed.inject(DspaceRestService);
    httpMock = TestBed.inject(HttpTestingController);
    cookieService = TestBed.inject(CookieService);
    correlationIdService = TestBed.inject(CorrelationIdService);

    cookieService.set('CORRELATION-ID','123455');
    correlationIdService.initCorrelationId();
  });


  it('headers should be set', (done) => {
    service.request(RestRequestMethod.POST, 'server/api/core/items', 'test', { withCredentials: false }).subscribe((response) => {
      expect(response).toBeTruthy();
      done();
    });

    const httpRequest = httpMock.expectOne('server/api/core/items');
    httpRequest.flush(mockPayload, { status: mockStatusCode, statusText: mockStatusText });
    expect(httpRequest.request.headers.has('X-CORRELATION-ID')).toBeTrue();
    expect(httpRequest.request.headers.has('X-REFERRER')).toBeTrue();
  });

  it('headers should have the right values', (done) => {
    service.request(RestRequestMethod.POST, 'server/api/core/items', 'test', { withCredentials: false }).subscribe((response) => {
      expect(response).toBeTruthy();
      done();
    });

    const httpRequest = httpMock.expectOne('server/api/core/items');
    httpRequest.flush(mockPayload, { status: mockStatusCode, statusText: mockStatusText });
    expect(httpRequest.request.headers.get('X-CORRELATION-ID')).toEqual('123455');
    expect(httpRequest.request.headers.get('X-REFERRER')).toEqual('/statistics');
  });
});
