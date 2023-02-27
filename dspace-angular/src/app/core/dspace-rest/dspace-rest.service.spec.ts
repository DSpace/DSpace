import { inject, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DEFAULT_CONTENT_TYPE, DspaceRestService } from './dspace-rest.service';
import { DSpaceObject } from '../shared/dspace-object.model';
import { RestRequestMethod } from '../data/rest-request-method';
import { HttpHeaders } from '@angular/common/http';

describe('DspaceRestService', () => {
  let dspaceRestService: DspaceRestService;
  let httpMock: HttpTestingController;
  const url = 'http://www.dspace.org/';
  const mockError: any = {
    statusCode: 0,
    statusText: 'Unknown Error',
    message: 'Http failure response for http://www.dspace.org/: 0 '
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DspaceRestService]
    });

    dspaceRestService = TestBed.inject(DspaceRestService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', inject([DspaceRestService], (service: DspaceRestService) => {
    expect(service).toBeTruthy();
  }));

  describe('#get', () => {
    it('should return an Observable<RawRestResponse>', () => {
      const mockPayload = {
        page: 1
      };
      const mockStatusCode = 200;
      const mockStatusText = 'GREAT';

      dspaceRestService.get(url).subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.statusCode).toEqual(mockStatusCode);
        expect(response.statusText).toEqual(mockStatusText);
        expect(response.payload.page).toEqual(mockPayload.page);
      });

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('GET');
      req.flush(mockPayload, { status: mockStatusCode, statusText: mockStatusText });
    });
    it('should throw an error', () => {
      dspaceRestService.get(url).subscribe(() => undefined, (err) => {
        expect(err).toEqual(mockError);
      });
      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('GET');
      req.error(mockError);
    });

    it('should log an error', () => {
      spyOn(console, 'log');

      dspaceRestService.get(url).subscribe(() => undefined, (err) => {
        expect(console.log).toHaveBeenCalled();
      });

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('GET');
      req.error(mockError);
    });

    it('when no content-type header is provided, it should use application/json', () => {
      dspaceRestService.request(RestRequestMethod.POST, url, {}).subscribe();

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('Content-Type')).toContain(DEFAULT_CONTENT_TYPE);
    });
  });

  describe('#request', () => {
    it('should return an Observable<RawRestResponse>', () => {
      const mockPayload = {
        page: 1
      };
      const mockStatusCode = 200;
      const mockStatusText = 'GREAT';

      dspaceRestService.request(RestRequestMethod.POST, url, {}).subscribe((response) => {
        expect(response).toBeTruthy();
        expect(response.statusCode).toEqual(mockStatusCode);
        expect(response.statusText).toEqual(mockStatusText);
        expect(response.payload.page).toEqual(mockPayload.page);
      });

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('POST');
      req.flush(mockPayload, { status: mockStatusCode, statusText: mockStatusText });
    });

    it('when a content-type header is provided, it should not use application/json', () => {
      let headers = new HttpHeaders();
      headers = headers.set('Content-Type', 'text/html');
      dspaceRestService.request(RestRequestMethod.POST, url, {}, { headers }).subscribe();

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('Content-Type')).not.toContain(DEFAULT_CONTENT_TYPE);
    });

    it('when no content-type header is provided, it should use application/json', () => {
      dspaceRestService.request(RestRequestMethod.POST, url, {}).subscribe();

      const req = httpMock.expectOne(url);
      expect(req.request.headers.get('Content-Type')).toContain(DEFAULT_CONTENT_TYPE);
    });
  });

  describe('buildFormData', () => {
    it('should return the correct data', () => {
      const name = 'testname';
      const dso: DSpaceObject = {
        name: name
      } as DSpaceObject;
      const formdata = dspaceRestService.buildFormData(dso);
      expect(formdata.get('name')).toBe(name);
    });
  });
});
