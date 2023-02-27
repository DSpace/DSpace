import { VersionHistory } from '../shared/version-history.model';
import { Injectable } from '@angular/core';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { HttpHeaders } from '@angular/common/http';
import { PostRequest } from './request.models';
import { Observable, of } from 'rxjs';
import { PaginatedSearchOptions } from '../../shared/search/models/paginated-search-options.model';
import { RemoteData } from './remote-data';
import { PaginatedList } from './paginated-list.model';
import { Version } from '../shared/version.model';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { VERSION_HISTORY } from '../shared/version-history.resource-type';
import { followLink, FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { VersionDataService } from './version-data.service';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { getAllSucceededRemoteData, getFirstCompletedRemoteData, getFirstSucceededRemoteDataPayload, getRemoteDataPayload } from '../shared/operators';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { hasValueOperator } from '../../shared/empty.util';
import { Item } from '../shared/item.model';
import { FindListOptions } from './find-list-options.model';
import { sendRequest } from '../shared/request.operators';
import { RestRequest } from './rest-request.model';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { dataService } from './base/data-service.decorator';

/**
 * Service responsible for handling requests related to the VersionHistory object
 */
@Injectable()
@dataService(VERSION_HISTORY)
export class VersionHistoryDataService extends IdentifiableDataService<VersionHistory> {
  protected versionsEndpoint = 'versions';

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected versionDataService: VersionDataService,
  ) {
    super('versionhistories', requestService, rdbService, objectCache, halService);
  }

  /**
   * Get the endpoint for browsing versions
   */
  getBrowseEndpoint(options: FindListOptions = {}, linkPath?: string): Observable<string> {
    return this.halService.getEndpoint(this.linkPath);
  }

  /**
   * Get the versions endpoint for a version history
   * @param versionHistoryId
   */
  getVersionsEndpoint(versionHistoryId: string): Observable<string> {
    return this.getBrowseEndpoint().pipe(
      switchMap((href: string) => this.halService.getEndpoint(this.versionsEndpoint, `${href}/${versionHistoryId}`))
    );
  }

  /**
   * Get a version history's versions using paginated search options
   * @param versionHistoryId            The version history's ID
   * @param searchOptions               The search options to use
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  getVersions(versionHistoryId: string, searchOptions?: PaginatedSearchOptions, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Version>[]): Observable<RemoteData<PaginatedList<Version>>> {
    const hrefObs = this.getVersionsEndpoint(versionHistoryId).pipe(
      map((href) => searchOptions ? searchOptions.toRestUrl(href) : href)
    );

    return this.versionDataService.findListByHref(hrefObs, undefined, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Create a new version for an item
   * @param itemHref the item for which create a new version
   * @param summary the summary of the new version
   */
  createVersion(itemHref: string, summary: string): Observable<RemoteData<Version>> {
    const requestOptions: HttpOptions = Object.create({});
    let requestHeaders = new HttpHeaders();
    requestHeaders = requestHeaders.append('Content-Type', 'text/uri-list');
    requestOptions.headers = requestHeaders;

    return this.halService.getEndpoint(this.versionsEndpoint).pipe(
      take(1),
      map((endpointUrl: string) => (summary?.length > 0) ? `${endpointUrl}?summary=${summary}` : `${endpointUrl}`),
      map((endpointURL: string) => new PostRequest(this.requestService.generateRequestId(), endpointURL, itemHref, requestOptions)),
      sendRequest(this.requestService),
      switchMap((restRequest: RestRequest) => this.rdbService.buildFromRequestUUID(restRequest.uuid)),
      getFirstCompletedRemoteData()
    ) as Observable<RemoteData<Version>>;
  }

  /**
   * Get the latest version in a version history
   * @param versionHistory
   */
  getLatestVersionFromHistory$(versionHistory: VersionHistory): Observable<Version> {

    // Pagination options to fetch a single version on the first page (this is the latest version in the history)
    const latestVersionOptions = Object.assign(new PaginationComponentOptions(), {
      id: 'item-newest-version-options',
      currentPage: 1,
      pageSize: 1
    });

    const latestVersionSearch = new PaginatedSearchOptions({pagination: latestVersionOptions});

    return this.getVersions(versionHistory.id, latestVersionSearch, false, true, followLink('item')).pipe(
      getAllSucceededRemoteData(),
      getRemoteDataPayload(),
      hasValueOperator(),
      filter((versions) => versions.page.length > 0),
      map((versions) => versions.page[0])
    );

  }

  /**
   * Get the latest version (return null if the specified version is null)
   * @param version
   */
  getLatestVersion$(version: Version): Observable<Version> {
    // retrieve again version, including with versionHistory
    return version.id ? this.versionDataService.findById(version.id, false, true, followLink('versionhistory')).pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((res) => res.versionhistory),
      getFirstSucceededRemoteDataPayload(),
      switchMap((versionHistoryRD) => this.getLatestVersionFromHistory$(versionHistoryRD)),
    ) : of(null);
  }

  /**
   * Check if the given version is the latest (return null if `version` is null)
   * @param version
   * @returns `true` if the specified version is the latest one, `false` otherwise, or `null` if the specified version is null
   */
  isLatest$(version: Version): Observable<boolean> {
    return version ? this.getLatestVersion$(version).pipe(
      take(1),
      switchMap((latestVersion) => of(version.version === latestVersion.version))
    ) : of(null);
  }

  /**
   * Check if a worskpace item exists in the version history (return null if there is no version history)
   * @param versionHref the href of the version
   * @returns `true` if a workspace item exists, `false` otherwise, or `null` if a version history does not exist
   */
  hasDraftVersion$(versionHref: string): Observable<boolean> {
    return this.versionDataService.findByHref(versionHref, true, true, followLink('versionhistory')).pipe(
      getFirstCompletedRemoteData(),
      switchMap((res) => {
        if (res.hasSucceeded && !res.hasNoContent) {
          return of(res).pipe(
            getFirstSucceededRemoteDataPayload(),
            switchMap((version) => this.versionDataService.getHistoryFromVersion(version)),
            map((versionHistory) => versionHistory ? versionHistory.draftVersion : false),
          );
        } else {
          return of(false);
        }
      }),
    );
  }

  /**
   * Get the item of the latest version in a version history
   * @param versionHistory
   */
  getLatestVersionItemFromHistory$(versionHistory: VersionHistory): Observable<Item> {
    return this.getLatestVersionFromHistory$(versionHistory).pipe(
      switchMap((newLatestVersion) => newLatestVersion.item),
      getFirstSucceededRemoteDataPayload(),
    );
  }

  /**
   * Get the item of the latest version from any version in the version history
   * @param version
   */
  getVersionHistoryFromVersion$(version: Version): Observable<VersionHistory> {
    return this.versionDataService.getHistoryIdFromVersion(version).pipe(
      take(1),
      switchMap((res) => this.findById(res)),
      getFirstSucceededRemoteDataPayload(),
    );
  }

  /**
   * Invalidate the cache of the version history
   * @param versionHistoryID
   */
  invalidateVersionHistoryCache(versionHistoryID: string) {
    this.requestService.setStaleByHrefSubstring('versioning/versionhistories/' + versionHistoryID);
  }
}
