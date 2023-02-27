/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { BaseDataService } from './base-data.service';
import { Observable } from 'rxjs';
import { RemoteData } from '../remote-data';
import { DSpaceSerializer } from '../../dspace-rest/dspace.serializer';
import { GenericConstructor } from '../../shared/generic-constructor';
import { PutRequest } from '../request.models';
import { hasValue } from '../../../shared/empty.util';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';

/**
 * Interface for a data service that can send PUT requests.
 */
export interface PutData<T extends CacheableObject> {
  /**
   * Send a PUT request for the specified object
   *
   * @param object The object to send a put request for.
   */
  put(object: T): Observable<RemoteData<T>>;
}

/**
 * A DataService feature to send PUT requests.
 *
 * Concrete data services can use this feature by implementing {@link PutData}
 * and delegating its method to an inner instance of this class.
 */
export class PutDataImpl<T extends CacheableObject> extends BaseDataService<T> implements PutData<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected responseMsToLive: number,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive);
  }

  /**
   * Send a PUT request for the specified object
   *
   * @param object The object to send a put request for.
   */
  put(object: T): Observable<RemoteData<T>> {
    const requestId = this.requestService.generateRequestId();
    const serializedObject = new DSpaceSerializer(object.constructor as GenericConstructor<{}>).serialize(object);
    const request = new PutRequest(requestId, object._links.self.href, serializedObject);

    if (hasValue(this.responseMsToLive)) {
      request.responseMsToLive = this.responseMsToLive;
    }

    this.requestService.send(request);

    return this.rdbService.buildFromRequestUUID(requestId);
  }
}
