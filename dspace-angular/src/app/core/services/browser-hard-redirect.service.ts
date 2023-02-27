import { Inject, Injectable, InjectionToken } from '@angular/core';
import { HardRedirectService } from './hard-redirect.service';

export const LocationToken = new InjectionToken('Location');

export function locationProvider(): Location {
  return window.location;
}

/**
 * Service for performing hard redirects within the browser app module
 */
@Injectable({providedIn: 'root'})
export class BrowserHardRedirectService extends HardRedirectService {

  constructor(
    @Inject(LocationToken) protected location: Location,
  ) {
    super();
  }

  /**
   * Perform a hard redirect to URL
   * @param url
   */
  redirect(url: string) {
    this.location.replace(url);
  }

  /**
   * Get the current route, with query params included
   * e.g. /search?page=1&query=open%20access&f.dateIssued.min=1980&f.dateIssued.max=2020
   */
  getCurrentRoute(): string {
    return this.location.pathname + this.location.search;
  }

  /**
   * Get the origin of the current URL
   * i.e. <scheme> "://" <hostname> [ ":" <port> ]
   * e.g. if the URL is https://demo7.dspace.org/search?query=test,
   * the origin would be https://demo7.dspace.org
   */
  getCurrentOrigin(): string {
    return this.location.origin;
  }
}
