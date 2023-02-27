import { Observable, of as observableOf } from 'rxjs';
import { Injectable } from '@angular/core';
import { AUTHORIZATION } from '../../shared/authorization.resource-type';
import { Authorization } from '../../shared/authorization.model';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { SiteDataService } from '../site-data.service';
import { followLink, FollowLinkConfig } from '../../../shared/utils/follow-link-config.model';
import { RemoteData } from '../remote-data';
import { PaginatedList } from '../paginated-list.model';
import { catchError, map, switchMap } from 'rxjs/operators';
import { hasNoValue, hasValue, isNotEmpty } from '../../../shared/empty.util';
import { RequestParam } from '../../cache/models/request-param.model';
import { AuthorizationSearchParams } from './authorization-search-params';
import { oneAuthorizationMatchesFeature } from './authorization-utils';
import { FeatureID } from './feature-id';
import { getFirstCompletedRemoteData } from '../../shared/operators';
import { FindListOptions } from '../find-list-options.model';
import { BaseDataService } from '../base/base-data.service';
import { SearchData, SearchDataImpl } from '../base/search-data';
import { dataService } from '../base/data-service.decorator';

/**
 * A service to retrieve {@link Authorization}s from the REST API
 */
@Injectable()
@dataService(AUTHORIZATION)
export class AuthorizationDataService extends BaseDataService<Authorization> implements SearchData<Authorization> {
  protected linkPath = 'authorizations';
  protected searchByObjectPath = 'object';

  private searchData: SearchDataImpl<Authorization>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected siteService: SiteDataService,
  ) {
    super('authorizations', requestService, rdbService, objectCache, halService);

    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
  }

  /**
   * Set all authorization requests to stale
   */
  invalidateAuthorizationsRequestCache() {
    this.requestService.setStaleByHrefSubstring(this.linkPath);
  }

  /**
   * Checks if an {@link EPerson} (or anonymous) has access to a specific object within a {@link Feature}
   * @param objectUrl                   URL to the object to search {@link Authorization}s for.
   *                                    If not provided, the repository's {@link Site} will be used.
   * @param ePersonUuid                 UUID of the {@link EPerson} to search {@link Authorization}s for.
   *                                    If not provided, the UUID of the currently authenticated {@link EPerson} will be used.
   * @param featureId                   ID of the {@link Feature} to check {@link Authorization} for
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   */
  isAuthorized(featureId?: FeatureID, objectUrl?: string, ePersonUuid?: string, useCachedVersionIfAvailable = true, reRequestOnStale = true): Observable<boolean> {
    return this.searchByObject(featureId, objectUrl, ePersonUuid, {}, useCachedVersionIfAvailable, reRequestOnStale, followLink('feature')).pipe(
      getFirstCompletedRemoteData(),
      map((authorizationRD) => {
        if (authorizationRD.statusCode !== 401 && hasValue(authorizationRD.payload) && isNotEmpty(authorizationRD.payload.page)) {
          return authorizationRD.payload.page;
        } else {
          return [];
        }
      }),
      catchError(() => observableOf(false)),
      oneAuthorizationMatchesFeature(featureId)
    );
  }

  /**
   * Search for a list of {@link Authorization}s using the "object" search endpoint and providing optional object url,
   * {@link EPerson} uuid and/or {@link Feature} id
   * @param objectUrl                   URL to the object to search {@link Authorization}s for.
   *                                    If not provided, the repository's {@link Site} will be used.
   * @param ePersonUuid                 UUID of the {@link EPerson} to search {@link Authorization}s for.
   *                                    If not provided, the UUID of the currently authenticated {@link EPerson} will be used.
   * @param featureId                   ID of the {@link Feature} to search {@link Authorization}s for
   * @param options                     {@link FindListOptions} to provide pagination and/or additional arguments
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  searchByObject(featureId?: FeatureID, objectUrl?: string, ePersonUuid?: string, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Authorization>[]): Observable<RemoteData<PaginatedList<Authorization>>> {
    const objectUrl$ = observableOf(objectUrl).pipe(
      switchMap((url) => {
        if (hasNoValue(url)) {
          return this.siteService.find().pipe(
            map((site) => site.self)
          );
        } else {
          return observableOf(url);
        }
      }),
    );

    const out$ = objectUrl$.pipe(
      map((url: string) => new AuthorizationSearchParams(url, ePersonUuid, featureId)),
      switchMap((params: AuthorizationSearchParams) => {
        return this.searchBy(this.searchByObjectPath, this.createSearchOptions(params.objectUrl, options, params.ePersonUuid, params.featureId), useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
      })
    );

    this.addDependency(out$, objectUrl$);

    return out$;
  }

  /**
   * Create {@link FindListOptions} with {@link RequestParam}s containing a "uri", "feature" and/or "eperson" parameter
   * @param objectUrl   Required parameter value to add to {@link RequestParam} "uri"
   * @param options     Optional initial {@link FindListOptions} to add parameters to
   * @param ePersonUuid Optional parameter value to add to {@link RequestParam} "eperson"
   * @param featureId   Optional parameter value to add to {@link RequestParam} "feature"
   */
  private createSearchOptions(objectUrl: string, options: FindListOptions = {}, ePersonUuid?: string, featureId?: FeatureID): FindListOptions {
    let params = [];
    if (isNotEmpty(options.searchParams)) {
      params = [...options.searchParams];
    }
    params.push(new RequestParam('uri', objectUrl));
    if (hasValue(featureId)) {
      params.push(new RequestParam('feature', featureId));
    }
    if (hasValue(ePersonUuid)) {
      params.push(new RequestParam('eperson', ePersonUuid));
    }
    return Object.assign(new FindListOptions(), options, {
      searchParams: [...params],
    });
  }

  /**
   * Make a new FindListRequest with given search method
   *
   * @param searchMethod                The search method for the object
   * @param options                     The [[FindListOptions]] object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>}
   *    Return an observable that emits response from the server
   */
  searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<Authorization>[]): Observable<RemoteData<PaginatedList<Authorization>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
