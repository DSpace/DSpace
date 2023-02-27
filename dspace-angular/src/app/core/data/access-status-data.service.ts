import { Injectable } from '@angular/core';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RequestService } from './request.service';
import { AccessStatusObject } from 'src/app/shared/object-list/access-status-badge/access-status.model';
import { ACCESS_STATUS } from 'src/app/shared/object-list/access-status-badge/access-status.resource-type';
import { Observable } from 'rxjs';
import { RemoteData } from './remote-data';
import { Item } from '../shared/item.model';
import { BaseDataService } from './base/base-data.service';
import { dataService } from './base/data-service.decorator';

/**
 * Data service responsible for retrieving the access status of Items
 */
@Injectable()
@dataService(ACCESS_STATUS)
export class AccessStatusDataService extends BaseDataService<AccessStatusObject> {

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('accessStatus', requestService, rdbService, objectCache, halService);
  }

  /**
   * Returns {@link RemoteData} of {@link AccessStatusObject} that is the access status of the given item
   * @param item Item we want the access status of
   */
  findAccessStatusFor(item: Item): Observable<RemoteData<AccessStatusObject>> {
    return this.findByHref(item._links.accessStatus.href);
  }
}
