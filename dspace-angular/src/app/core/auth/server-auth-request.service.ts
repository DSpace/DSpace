import { Injectable } from '@angular/core';
import { AuthRequestService } from './auth-request.service';
import { PostRequest } from '../data/request.models';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import {
  HttpHeaders,
  HttpClient,
  HttpResponse
} from '@angular/common/http';
import {
  XSRF_REQUEST_HEADER,
  XSRF_RESPONSE_HEADER,
  DSPACE_XSRF_COOKIE
} from '../xsrf/xsrf.interceptor';
import { map } from 'rxjs/operators';
import { Observable } from 'rxjs';

/**
 * Server side version of the service to send authentication requests
 */
@Injectable()
export class ServerAuthRequestService extends AuthRequestService {

  constructor(
    halService: HALEndpointService,
    requestService: RequestService,
    rdbService: RemoteDataBuildService,
    protected httpClient: HttpClient,
  ) {
    super(halService, requestService, rdbService);
  }

  /**
   * Factory function to create the request object to send.
   *
   * @param href The href to send the request to
   * @protected
   */
  protected createShortLivedTokenRequest(href: string): Observable<PostRequest> {
    // First do a call to the root endpoint in order to get an XSRF token
    return this.httpClient.get(this.halService.getRootHref(), { observe: 'response' }).pipe(
      // retrieve the XSRF token from the response header
      map((response: HttpResponse<any>) => response.headers.get(XSRF_RESPONSE_HEADER)),
      // Use that token to create an HttpHeaders object
      map((xsrfToken: string) => new HttpHeaders()
          .set('Content-Type', 'application/json; charset=utf-8')
          // set the token as the XSRF header
          .set(XSRF_REQUEST_HEADER, xsrfToken)
          // and as the DSPACE-XSRF-COOKIE
          .set('Cookie', `${DSPACE_XSRF_COOKIE}=${xsrfToken}`)),
      map((headers: HttpHeaders) =>
        // Create a new PostRequest using those headers and the given href
        new PostRequest(
          this.requestService.generateRequestId(),
          href,
          {},
          {
            headers: headers,
          },
        )
      )
    );
  }

}
