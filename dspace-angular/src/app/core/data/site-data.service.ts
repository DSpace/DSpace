import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { getFirstSucceededRemoteData } from '../shared/operators';
import { Site } from '../shared/site.model';
import { SITE } from '../shared/site.resource-type';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import { RequestService } from './request.service';
import { BaseDataService } from './base/base-data.service';
import { FindAllData, FindAllDataImpl } from './base/find-all-data';
import { FollowLinkConfig } from 'src/app/shared/utils/follow-link-config.model';
import { FindListOptions } from './find-list-options.model';
import { ObjectCacheService } from '../cache/object-cache.service';
import { dataService } from './base/data-service.decorator';

/**
 * Service responsible for handling requests related to the Site object
 */
@Injectable()
@dataService(SITE)
export class SiteDataService extends BaseDataService<Site> implements FindAllData<Site> {
  private findAllData: FindAllData<Site>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('sites', requestService, rdbService, objectCache, halService);

    this.findAllData = new FindAllDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
  }

  /**
   * Retrieve the Site Object
   */
  find(): Observable<Site> {
    return this.findAll().pipe(
      getFirstSucceededRemoteData(),
      map((remoteData: RemoteData<PaginatedList<Site>>) => remoteData.payload),
      map((list: PaginatedList<Site>) => list.page[0]),
    );
  }

  /**
   * Returns {@link RemoteData} of all object with a list of {@link FollowLinkConfig}, to indicate which embedded
   * info should be added to the objects
   *
   * @param options                     Find list options object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>>}
   *    Return an observable that emits object list
   */
  public findAll(options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<Site>[]): Observable<RemoteData<PaginatedList<Site>>> {
    return this.findAllData.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
