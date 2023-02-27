import { Inject, Injectable } from '@angular/core';
import { Request, Response } from 'express';
import { REQUEST, RESPONSE } from '@nguniversal/express-engine/tokens';
import { HardRedirectService } from './hard-redirect.service';

/**
 * Service for performing hard redirects within the server app module
 */
@Injectable()
export class ServerHardRedirectService extends HardRedirectService {

  constructor(
    @Inject(REQUEST) protected req: Request,
    @Inject(RESPONSE) protected res: Response,
  ) {
    super();
  }

  /**
   * Perform a hard redirect to URL
   * @param url
   */
  redirect(url: string) {

    if (url === this.req.url) {
      return;
    }

    if (this.res.finished) {
      const req: any = this.req;
      req._r_count = (req._r_count || 0) + 1;

      console.warn('Attempted to redirect on a finished response. From',
        this.req.url, 'to', url);

      if (req._r_count > 10) {
        console.error('Detected a redirection loop. killing the nodejs process');
        process.exit(1);
      }
    } else {
      // attempt to use the already set status
      let status = this.res.statusCode || 0;
      if (status < 300 || status >= 400) {
        // temporary redirect
        status = 302;
      }

      console.log(`Redirecting from ${this.req.url} to ${url} with ${status}`);
      this.res.redirect(status, url);
      this.res.end();
      // I haven't found a way to correctly stop Angular rendering.
      // So we just let it end its work, though we have already closed
      // the response.
    }
  }

  /**
   * Get the URL of the current route
   */
  getCurrentRoute(): string {
    return this.req.originalUrl;
  }

  /**
   * Get the origin of the current URL
   * i.e. <scheme> "://" <hostname> [ ":" <port> ]
   * e.g. if the URL is https://demo7.dspace.org/search?query=test,
   * the origin would be https://demo7.dspace.org
   */
  getCurrentOrigin(): string {
    return this.req.protocol + '://' + this.req.headers.host;
  }
}
