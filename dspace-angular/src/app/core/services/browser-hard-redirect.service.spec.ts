import { TestBed } from '@angular/core/testing';
import { BrowserHardRedirectService } from './browser-hard-redirect.service';

describe('BrowserHardRedirectService', () => {
  let origin: string;
  let mockLocation: Location;
  let service: BrowserHardRedirectService;

  beforeEach(() => {
    origin = 'https://test-host.com:4000';
    mockLocation = {
      href: undefined,
      pathname: '/pathname',
      search: '/search',
      origin,
      replace: (url: string) => {
        mockLocation.href = url;
      }
    } as Location;
    spyOn(mockLocation, 'replace');

    service = new BrowserHardRedirectService(mockLocation);

    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('when performing a redirect', () => {

    const redirect = 'test redirect';

    beforeEach(() => {
      service.redirect(redirect);
    });

    it('should call location.replace with the new url', () => {
      expect(mockLocation.replace).toHaveBeenCalledWith(redirect);
    });
  });

  describe('when requesting the current route', () => {

    it('should return the location origin', () => {
      expect(service.getCurrentRoute()).toEqual(mockLocation.pathname + mockLocation.search);
    });
  });

  describe('when requesting the origin', () => {

    it('should return the location origin', () => {
      expect(service.getCurrentOrigin()).toEqual(origin);
    });
  });

});
