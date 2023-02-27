import { Root } from './root.model';
import { Injectable } from '@angular/core';
import { ROOT } from './root.resource-type';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Observable, of as observableOf } from 'rxjs';
import { RemoteData } from './remote-data';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { DspaceRestService } from '../dspace-rest/dspace-rest.service';
import { RawRestResponse } from '../dspace-rest/raw-rest-response.model';
import { catchError, map } from 'rxjs/operators';
import { BaseDataService } from './base/base-data.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { dataService } from './base/data-service.decorator';

/**
 * A service to retrieve the {@link Root} object from the REST API.
 */
@Injectable()
@dataService(ROOT)
export class RootDataService extends BaseDataService<Root> {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected restService: DspaceRestService,
  ) {
    super('', requestService, rdbService, objectCache, halService, 6 * 60 * 60 * 1000);
  }

  /**
   * Check if root endpoint is available
   */
  checkServerAvailability(): Observable<boolean> {
    return this.restService.get(this.halService.getRootHref()).pipe(
      catchError((err ) => {
        console.error(err);
        return observableOf(false);
      }),
      map((res: RawRestResponse) => res.statusCode === 200)
    );
  }

  /**
   * Find the {@link Root} object of the REST API
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findRoot(useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Root>[]): Observable<RemoteData<Root>> {
    return this.findByHref(this.halService.getRootHref(), useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Set to sale the root endpoint cache hit
   */
  invalidateRootCache() {
    this.requestService.setStaleByHrefSubstring(this.halService.getRootHref());
  }
}
