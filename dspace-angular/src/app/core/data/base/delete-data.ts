/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { Observable } from 'rxjs';
import { RemoteData } from '../remote-data';
import { NoContent } from '../../shared/NoContent.model';
import { switchMap } from 'rxjs/operators';
import { DeleteRequest } from '../request.models';
import { hasNoValue, hasValue } from '../../../shared/empty.util';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { ConstructIdEndpoint, IdentifiableDataService } from './identifiable-data.service';

export interface DeleteData<T extends CacheableObject> {
  /**
   * Delete an existing object on the server
   * @param   objectId The id of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   */
  delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>>;

  /**
   * Delete an existing object on the server
   * @param   href The self link of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   *          Only emits once all request related to the DSO has been invalidated.
   */
  deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>>;
}

export class DeleteDataImpl<T extends CacheableObject> extends IdentifiableDataService<T> implements DeleteData<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected responseMsToLive: number,
    protected constructIdEndpoint: ConstructIdEndpoint,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive, constructIdEndpoint);
    if (hasNoValue(constructIdEndpoint)) {
      throw new Error(`DeleteDataImpl initialized without a constructIdEndpoint method (linkPath: ${linkPath})`);
    }
  }

  delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.getIDHrefObs(objectId).pipe(
      switchMap((href: string) => this.deleteByHref(href, copyVirtualMetadata)),
    );
  }

  deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    const requestId = this.requestService.generateRequestId();

    if (copyVirtualMetadata) {
      copyVirtualMetadata.forEach((id) =>
        href += (href.includes('?') ? '&' : '?')
          + 'copyVirtualMetadata='
          + id,
      );
    }

    const request = new DeleteRequest(requestId, href);
    if (hasValue(this.responseMsToLive)) {
      request.responseMsToLive = this.responseMsToLive;
    }
    this.requestService.send(request);

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => this.invalidateByHref(href));
  }
}
