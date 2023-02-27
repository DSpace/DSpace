import { Injectable } from '@angular/core';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { USAGE_REPORT } from './models/usage-report.resource-type';
import { UsageReport } from './models/usage-report.model';
import { Observable } from 'rxjs';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../shared/operators';
import { map } from 'rxjs/operators';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { SearchData, SearchDataImpl } from '../data/base/search-data';
import { FindListOptions } from '../data/find-list-options.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { dataService } from '../data/base/data-service.decorator';

/**
 * A service to retrieve {@link UsageReport}s from the REST API
 */
@Injectable()
@dataService(USAGE_REPORT)
export class UsageReportDataService extends IdentifiableDataService<UsageReport> implements SearchData<UsageReport> {
  private searchData: SearchDataImpl<UsageReport>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected halService: HALEndpointService,
    protected objectCache: ObjectCacheService,
  ) {
    super('usagereports', requestService, rdbService, objectCache, halService);

    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
  }

  getStatistic(scope: string, type: string): Observable<UsageReport> {
    return this.findById(`${scope}_${type}`).pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    );
  }

  searchStatistics(uri: string, page: number, size: number): Observable<UsageReport[]> {
    return this.searchBy('object', {
      searchParams: [
        {
          fieldName: `uri`,
          fieldValue: uri,
        },
      ],
      currentPage: page,
      elementsPerPage: size,
    }, true, false).pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      map((list) => list.page),
    );
  }

  public searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<UsageReport>[]): Observable<RemoteData<PaginatedList<UsageReport>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
