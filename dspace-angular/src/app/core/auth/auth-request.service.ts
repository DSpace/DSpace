import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, switchMap, tap, take } from 'rxjs/operators';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { isNotEmpty } from '../../shared/empty.util';
import { GetRequest, PostRequest, } from '../data/request.models';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { RemoteData } from '../data/remote-data';
import { AuthStatus } from './models/auth-status.model';
import { ShortLivedToken } from './models/short-lived-token.model';
import { URLCombiner } from '../url-combiner/url-combiner';
import { RestRequest } from '../data/rest-request.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';

/**
 * Abstract service to send authentication requests
 */
export abstract class AuthRequestService {
  protected linkName = 'authn';
  protected shortlivedtokensEndpoint = 'shortlivedtokens';

  constructor(protected halService: HALEndpointService,
              protected requestService: RequestService,
              private rdbService: RemoteDataBuildService
              ) {
  }

  /**
   * Fetch the response to a request from the cache, once it's completed.
   * @param requestId the UUID of the request for which to retrieve the response
   * @protected
   */
  protected fetchRequest(requestId: string, ...linksToFollow: FollowLinkConfig<AuthStatus>[]): Observable<RemoteData<AuthStatus>> {
    return this.rdbService.buildFromRequestUUID<AuthStatus>(requestId, ...linksToFollow).pipe(
      getFirstCompletedRemoteData(),
    );
  }

  protected getEndpointByMethod(endpoint: string, method: string, ...linksToFollow: FollowLinkConfig<AuthStatus>[]): string {
    let url = isNotEmpty(method) ? `${endpoint}/${method}` : `${endpoint}`;
    if (linksToFollow?.length > 0) {
      linksToFollow.forEach((link: FollowLinkConfig<AuthStatus>, index: number) => {
        url += ((index === 0) ? '?' : '&') + `embed=${link.name}`;
      });
    }

    return url;
  }

  /**
   * Send a POST request to an authentication endpoint
   * @param method    the method to send to (e.g. 'status')
   * @param body      the data to send (optional)
   * @param options   the HTTP options for the request
   */
  public postToEndpoint(method: string, body?: any, options?: HttpOptions): Observable<RemoteData<AuthStatus>> {
    const requestId = this.requestService.generateRequestId();

    const endpoint$ = this.halService.getEndpoint(this.linkName);

    endpoint$.pipe(
      filter((href: string) => isNotEmpty(href)),
      map((endpointURL) => this.getEndpointByMethod(endpointURL, method)),
      distinctUntilChanged(),
      map((endpointURL: string) => new PostRequest(requestId, endpointURL, body, options)),
      take(1)
    ).subscribe((request: PostRequest) => {
      this.requestService.send(request);
    });

    return endpoint$.pipe(
      switchMap(() => this.fetchRequest(requestId)),
    );
  }

  /**
   * Send a GET request to an authentication endpoint
   * @param method    the method to send to (e.g. 'status')
   * @param options   the HTTP options for the request
   */
  public getRequest(method: string, options?: HttpOptions, ...linksToFollow: FollowLinkConfig<any>[]): Observable<RemoteData<AuthStatus>> {
    const requestId = this.requestService.generateRequestId();

    const endpoint$ = this.halService.getEndpoint(this.linkName);

    endpoint$.pipe(
      filter((href: string) => isNotEmpty(href)),
      map((endpointURL) => this.getEndpointByMethod(endpointURL, method, ...linksToFollow)),
      distinctUntilChanged(),
      map((endpointURL: string) => new GetRequest(requestId, endpointURL, undefined, options)),
      take(1)
    ).subscribe((request: GetRequest) => {
      this.requestService.send(request);
    });

    return endpoint$.pipe(
      switchMap(() => this.fetchRequest(requestId, ...linksToFollow)),
    );
  }
  /**
   * Factory function to create the request object to send.
   *
   * @param href The href to send the request to
   * @protected
   */
  protected abstract createShortLivedTokenRequest(href: string): Observable<PostRequest>;

  /**
   * Send a request to retrieve a short-lived token which provides download access of restricted files
   */
  public getShortlivedToken(): Observable<string> {
    return this.halService.getEndpoint(this.linkName).pipe(
      filter((href: string) => isNotEmpty(href)),
      distinctUntilChanged(),
      map((href: string) => new URLCombiner(href, this.shortlivedtokensEndpoint).toString()),
      switchMap((endpointURL: string) => this.createShortLivedTokenRequest(endpointURL)),
      tap((request: RestRequest) => this.requestService.send(request)),
      switchMap((request: RestRequest) => this.rdbService.buildFromRequestUUID<ShortLivedToken>(request.uuid)),
      getFirstCompletedRemoteData(),
      map((response: RemoteData<ShortLivedToken>) => {
        if (response.hasSucceeded) {
          return response.payload.value;
        } else {
          return null;
        }
      })
    );
  }
}
