import { Injectable } from '@angular/core';
import { ConfigDataService } from './config-data.service';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { SUBMISSION_UPLOADS_TYPE } from './models/config-type';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ConfigObject } from './models/config.model';
import { SubmissionUploadsModel } from './models/config-submission-uploads.model';
import { RemoteData } from '../data/remote-data';
import { Observable } from 'rxjs';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { dataService } from '../data/base/data-service.decorator';

/**
 * Provides methods to retrieve, from REST server, bitstream access conditions configurations applicable during the submission process.
 */
@Injectable()
@dataService(SUBMISSION_UPLOADS_TYPE)
export class SubmissionUploadsConfigDataService extends ConfigDataService {
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('submissionuploads', requestService, rdbService, objectCache, halService);
  }

  findByHref(href: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow): Observable<RemoteData<SubmissionUploadsModel>> {
    return super.findByHref(href, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow as FollowLinkConfig<ConfigObject>[]) as Observable<RemoteData<SubmissionUploadsModel>>;
  }
}
