import { Injectable } from '@angular/core';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from '../data/request.service';
import { SubmissionCcLicenceUrl } from './models/submission-cc-license-url.model';
import { SUBMISSION_CC_LICENSE_URL } from './models/submission-cc-licence-link.resource-type';
import { Field, Option, SubmissionCcLicence } from './models/submission-cc-license.model';
import { Observable } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { getRemoteDataPayload, getFirstSucceededRemoteData } from '../shared/operators';
import { BaseDataService } from '../data/base/base-data.service';
import { SearchData, SearchDataImpl } from '../data/base/search-data';
import { FindListOptions } from '../data/find-list-options.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { dataService } from '../data/base/data-service.decorator';

@Injectable()
@dataService(SUBMISSION_CC_LICENSE_URL)
export class SubmissionCcLicenseUrlDataService extends BaseDataService<SubmissionCcLicenceUrl> implements SearchData<SubmissionCcLicenceUrl> {
  private searchData: SearchDataImpl<SubmissionCcLicenceUrl>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('submissioncclicenseUrls-search', requestService, rdbService, objectCache, halService);

    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive, (href, searchMethod) => `${href}/${searchMethod}`);
  }

  /**
   * Get the link to the Creative Commons license corresponding to the given type and options.
   * @param ccLicense   the Creative Commons license type
   * @param options     the selected options of the license fields
   */
  getCcLicenseLink(ccLicense: SubmissionCcLicence, options: Map<Field, Option>): Observable<string> {

    return this.searchData.getSearchByHref(
      'rightsByQuestions',{
        searchParams: [
          {
            fieldName: 'license',
            fieldValue: ccLicense.id
          },
          ...ccLicense.fields.map(
            (field) => {
              return {
                fieldName: `answer_${field.id}`,
                fieldValue: options.get(field).id,
              };
            }),
        ]
      }
    ).pipe(
      switchMap((href) => this.findByHref(href)),
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      map((response) => response.url),
    );
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
  public searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<SubmissionCcLicenceUrl>[]): Observable<RemoteData<PaginatedList<SubmissionCcLicenceUrl>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
