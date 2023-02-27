import { Injectable } from '@angular/core';
import { Version } from '../shared/version.model';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { EMPTY, Observable } from 'rxjs';
import { VERSION } from '../shared/version.resource-type';
import { VersionHistory } from '../shared/version-history.model';
import { followLink } from '../../shared/utils/follow-link-config.model';
import { getFirstSucceededRemoteDataPayload } from '../shared/operators';
import { map, switchMap } from 'rxjs/operators';
import { isNotEmpty } from '../../shared/empty.util';
import { RemoteData } from './remote-data';
import { PatchData, PatchDataImpl } from './base/patch-data';
import { RestRequestMethod } from './rest-request-method';
import { DefaultChangeAnalyzer } from './default-change-analyzer.service';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { dataService } from './base/data-service.decorator';
import { Operation } from 'fast-json-patch';

/**
 * Service responsible for handling requests related to the Version object
 */
@Injectable()
@dataService(VERSION)
export class VersionDataService extends IdentifiableDataService<Version> implements PatchData<Version> {
  private patchData: PatchData<Version>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: DefaultChangeAnalyzer<Version>,
  ) {
    super('versions', requestService, rdbService, objectCache, halService);

    this.patchData = new PatchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, comparator, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Get the version history for the given version
   * @param version
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   */
  getHistoryFromVersion(version: Version, useCachedVersionIfAvailable = false, reRequestOnStale = true): Observable<VersionHistory> {
    return isNotEmpty(version) ? this.findById(version.id, useCachedVersionIfAvailable, reRequestOnStale, followLink('versionhistory')).pipe(
      getFirstSucceededRemoteDataPayload(),
      switchMap((res: Version) => res.versionhistory),
      getFirstSucceededRemoteDataPayload(),
    ) : EMPTY;
  }

  /**
   * Get the ID of the version history for the given version
   * @param version
   */
  getHistoryIdFromVersion(version: Version): Observable<string> {
    return this.getHistoryFromVersion(version).pipe(
      map((versionHistory: VersionHistory) => versionHistory.id),
    );
  }

  /**
   * Send a patch request for a specified object
   * @param {T} object The object to send a patch request for
   * @param {Operation[]} operations The patch operations to be performed
   */
  public patch(object: Version, operations: []): Observable<RemoteData<Version>> {
    return this.patchData.patch(object, operations);
  }

  /**
   * Add a new patch to the object cache
   * The patch is derived from the differences between the given object and its version in the object cache
   * @param {DSpaceObject} object The given object
   */
  public update(object: Version): Observable<RemoteData<Version>> {
    return this.patchData.update(object);
  }

  /**
   * Commit current object changes to the server
   * @param method The RestRequestMethod for which de server sync buffer should be committed
   */
  public commitUpdates(method?: RestRequestMethod): void {
    this.patchData.commitUpdates(method);
  }

  /**
   * Return a list of operations representing the difference between an object and its latest value in the cache.
   * @param object  the object to resolve to a list of patch operations
   */
  public createPatchFromCache(object: Version): Observable<Operation[]> {
    return this.patchData.createPatchFromCache(object);
  }

}
