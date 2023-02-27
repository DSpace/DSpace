/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { Operation } from 'fast-json-patch';
import { Observable } from 'rxjs';
import { RemoteData } from '../remote-data';
import { find, map, mergeMap } from 'rxjs/operators';
import { hasNoValue, hasValue, isNotEmpty } from '../../../shared/empty.util';
import { PatchRequest } from '../request.models';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../../shared/operators';
import { ChangeAnalyzer } from '../change-analyzer';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { ObjectCacheService } from '../../cache/object-cache.service';
import { RestRequestMethod } from '../rest-request-method';
import { ConstructIdEndpoint, IdentifiableDataService } from './identifiable-data.service';


/**
 * Interface for a data service that can patch and update objects.
 */
export interface PatchData<T extends CacheableObject> {
  /**
   * Send a patch request for a specified object
   * @param {T} object The object to send a patch request for
   * @param {Operation[]} operations The patch operations to be performed
   */
  patch(object: T, operations: Operation[]): Observable<RemoteData<T>>;

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  update(object: T): Observable<RemoteData<T>>;

  /**
   * Commit current object changes to the server
   * @param method The RestRequestMethod for which de server sync buffer should be committed
   */
  commitUpdates(method?: RestRequestMethod): void;

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  createPatchFromCache(object: T): Observable<Operation[]>;
}

/**
 * A DataService feature to patch and update objects.
 *
 * Concrete data services can use this feature by implementing {@link PatchData}
 * and delegating its method to an inner instance of this class.
 *
 * Note that this feature requires the object in question to have an ID.
 * Make sure to use the same {@link ConstructIdEndpoint} as in the parent data service.
 */
export class PatchDataImpl<T extends CacheableObject> extends IdentifiableDataService<T> implements PatchData<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: ChangeAnalyzer<T>,
    protected responseMsToLive: number,
    protected constructIdEndpoint: ConstructIdEndpoint,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive, constructIdEndpoint);
    if (hasNoValue(constructIdEndpoint)) {
      throw new Error(`PatchDataImpl initialized without a constructIdEndpoint method (linkPath: ${linkPath})`);
    }
  }

  /**
   * Send a patch request for a specified object
   * @param {T} object The object to send a patch request for
   * @param {Operation[]} operations The patch operations to be performed
   */
  patch(object: T, operations: Operation[]): Observable<RemoteData<T>> {
    const requestId = this.requestService.generateRequestId();

    const hrefObs = this.halService.getEndpoint(this.linkPath).pipe(
      map((endpoint: string) => this.getIDHref(endpoint, object.uuid)),
    );

    hrefObs.pipe(
      find((href: string) => hasValue(href)),
    ).subscribe((href: string) => {
      const request = new PatchRequest(requestId, href, operations);
      if (hasValue(this.responseMsToLive)) {
        request.responseMsToLive = this.responseMsToLive;
      }
      this.requestService.send(request);
    });

    return this.rdbService.buildFromRequestUUIDAndAwait(requestId, () => this.invalidateByHref(object._links.self.href));
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  update(object: T): Observable<RemoteData<T>> {
    return this.createPatchFromCache(object).pipe(
      mergeMap((operations: Operation[]) => {
        if (isNotEmpty(operations)) {
          this.objectCache.addPatch(object._links.self.href, operations);
        }
        return this.findByHref(object._links.self.href, true, true);
      }),
    );
  }

  /**
   * Commit current object changes to the server
   * @param method The RestRequestMethod for which de server sync buffer should be committed
   */
  commitUpdates(method?: RestRequestMethod): void {
    this.requestService.commit(method);
  }

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  createPatchFromCache(object: T): Observable<Operation[]> {
    const oldVersion$ = this.findByHref(object._links.self.href, true, false);
    return oldVersion$.pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
      map((oldVersion: T) => this.comparator.diff(oldVersion, object)),
    );
  }
}
