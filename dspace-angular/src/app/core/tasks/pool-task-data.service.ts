import { Injectable } from '@angular/core';

import { Observable } from 'rxjs';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { RequestService } from '../data/request.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { PoolTask } from './models/pool-task-object.model';
import { POOL_TASK } from './models/pool-task-object.resource-type';
import { TasksService } from './tasks.service';
import { RemoteData } from '../data/remote-data';
import { RequestParam } from '../cache/models/request-param.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { FindListOptions } from '../data/find-list-options.model';
import { dataService } from '../data/base/data-service.decorator';

/**
 * The service handling all REST requests for PoolTask
 */
@Injectable()
@dataService(POOL_TASK)
export class PoolTaskDataService extends TasksService<PoolTask> {
  /**
   * Initialize instance variables
   *
   * @param {RequestService} requestService
   * @param {RemoteDataBuildService} rdbService
   * @param {ObjectCacheService} objectCache
   * @param {HALEndpointService} halService
   */
  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
  ) {
    super('pooltasks', requestService, rdbService, objectCache, halService, 1000);
  }

  /**
   * Search a pool task by item uuid.
   * @param uuid
   *   The item uuid
   * @return {Observable<RemoteData<ClaimedTask>>}
   *    The server response
   */
  public findByItem(uuid: string): Observable<RemoteData<PoolTask>> {
    const options = new FindListOptions();
    options.searchParams = [
      new RequestParam('uuid', uuid)
    ];
    return this.searchTask('findByItem', options).pipe(getFirstCompletedRemoteData());
  }

  /**
   * Get the Href of the pool task
   *
   * @param poolTaskId
   *   the poolTask id
   * @return {Observable<string>>}
   *    the Href
   */
  public getPoolTaskEndpointById(poolTaskId): Observable<string> {
    return this.getEndpointById(poolTaskId);
  }

}
