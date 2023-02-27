import { Injectable } from '@angular/core';
import { createSelector, select, Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map, tap } from 'rxjs/operators';
import { BitstreamFormatsRegistryDeselectAction, BitstreamFormatsRegistryDeselectAllAction, BitstreamFormatsRegistrySelectAction } from '../../admin/admin-registries/bitstream-formats/bitstream-format.actions';
import { BitstreamFormatRegistryState } from '../../admin/admin-registries/bitstream-formats/bitstream-format.reducers';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { coreSelector } from '../core.selectors';
import { BitstreamFormat } from '../shared/bitstream-format.model';
import { BITSTREAM_FORMAT } from '../shared/bitstream-format.resource-type';
import { Bitstream } from '../shared/bitstream.model';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { RemoteData } from './remote-data';
import { PostRequest, PutRequest } from './request.models';
import { RequestService } from './request.service';
import { sendRequest } from '../shared/request.operators';
import { CoreState } from '../core-state.model';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { DeleteData, DeleteDataImpl } from './base/delete-data';
import { FindAllData, FindAllDataImpl } from './base/find-all-data';
import { FollowLinkConfig } from 'src/app/shared/utils/follow-link-config.model';
import { FindListOptions } from './find-list-options.model';
import { PaginatedList } from './paginated-list.model';
import { NoContent } from '../shared/NoContent.model';
import { dataService } from './base/data-service.decorator';

const bitstreamFormatsStateSelector = createSelector(
  coreSelector,
  (state: CoreState) => state.bitstreamFormats,
);
const selectedBitstreamFormatSelector = createSelector(
  bitstreamFormatsStateSelector,
  (bitstreamFormatRegistryState: BitstreamFormatRegistryState) => bitstreamFormatRegistryState.selectedBitstreamFormats,
);

/**
 * A service responsible for fetching/sending data from/to the REST API on the bitstreamformats endpoint
 */
@Injectable()
@dataService(BITSTREAM_FORMAT)
export class BitstreamFormatDataService extends IdentifiableDataService<BitstreamFormat> implements FindAllData<BitstreamFormat>, DeleteData<BitstreamFormat> {

  protected linkPath = 'bitstreamformats';

  private findAllData: FindAllDataImpl<BitstreamFormat>;
  private deleteData: DeleteDataImpl<BitstreamFormat>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected store: Store<CoreState>,
  ) {
    super('bitstreamformats', requestService, rdbService, objectCache, halService);

    this.findAllData = new FindAllDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Get the endpoint to update an existing bitstream format
   * @param formatId
   */
  public getUpdateEndpoint(formatId: string): Observable<string> {
    return this.getBrowseEndpoint().pipe(
      map((endpoint: string) => this.getIDHref(endpoint, formatId)),
    );
  }

  /**
   * Get the endpoint to create a new bitstream format
   */
  public getCreateEndpoint(): Observable<string> {
    return this.getBrowseEndpoint();
  }

  /**
   * Update an existing bitstreamFormat
   * @param bitstreamFormat
   */
  updateBitstreamFormat(bitstreamFormat: BitstreamFormat): Observable<RemoteData<BitstreamFormat>> {
    const requestId = this.requestService.generateRequestId();

    this.getUpdateEndpoint(bitstreamFormat.id).pipe(
      distinctUntilChanged(),
      map((endpointURL: string) =>
        new PutRequest(requestId, endpointURL, bitstreamFormat)),
      sendRequest(this.requestService)).subscribe();

    return this.rdbService.buildFromRequestUUID(requestId);

  }

  /**
   * Create a new BitstreamFormat
   * @param {BitstreamFormat} bitstreamFormat
   */
  public createBitstreamFormat(bitstreamFormat: BitstreamFormat): Observable<RemoteData<BitstreamFormat>> {
    const requestId = this.requestService.generateRequestId();

    this.getCreateEndpoint().pipe(
      map((endpointURL: string) => {
        return new PostRequest(requestId, endpointURL, bitstreamFormat);
      }),
      sendRequest(this.requestService)
    ).subscribe();

    return this.rdbService.buildFromRequestUUID(requestId);
  }

  /**
   * Clears the cache of the list of BitstreamFormats
   */
  public clearBitStreamFormatRequests(): Observable<string> {
    return this.getBrowseEndpoint().pipe(
      tap((href: string) => this.requestService.removeByHrefSubstring(href))
    );
  }

  /**
   * Gets all the selected BitstreamFormats from the store
   */
  public getSelectedBitstreamFormats(): Observable<BitstreamFormat[]> {
    return this.store.pipe(select(selectedBitstreamFormatSelector));
  }

  /**
   * Adds a BistreamFormat to the selected BitstreamFormats in the store
   * @param bitstreamFormat
   */
  public selectBitstreamFormat(bitstreamFormat: BitstreamFormat) {
    this.store.dispatch(new BitstreamFormatsRegistrySelectAction(bitstreamFormat));
  }

  /**
   * Removes a BistreamFormat from the list of selected BitstreamFormats in the store
   * @param bitstreamFormat
   */
  public deselectBitstreamFormat(bitstreamFormat: BitstreamFormat) {
    this.store.dispatch(new BitstreamFormatsRegistryDeselectAction(bitstreamFormat));
  }

  /**
   * Removes all BitstreamFormats from the list of selected BitstreamFormats in the store
   */
  public deselectAllBitstreamFormats() {
    this.store.dispatch(new BitstreamFormatsRegistryDeselectAllAction());
  }

  findByBitstream(bitstream: Bitstream): Observable<RemoteData<BitstreamFormat>> {
    return this.findByHref(bitstream._links.format.href);
  }

  /**
   * Returns {@link RemoteData} of all object with a list of {@link FollowLinkConfig}, to indicate which embedded
   * info should be added to the objects
   *
   * @param options                     Find list options object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return {Observable<RemoteData<PaginatedList<T>>>}
   *    Return an observable that emits object list
   */
  findAll(options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<BitstreamFormat>[]): Observable<RemoteData<PaginatedList<BitstreamFormat>>> {
    return this.findAllData.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Delete an existing object on the server
   * @param   objectId The id of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   */
  delete(objectId: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.delete(objectId, copyVirtualMetadata);
  }

  /**
   * Delete an existing object on the server
   * @param   href The self link of the object to be removed
   * @param   copyVirtualMetadata (optional parameter) the identifiers of the relationship types for which the virtual
   *                            metadata should be saved as real metadata
   * @return  A RemoteData observable with an empty payload, but still representing the state of the request: statusCode,
   *          errorMessage, timeCompleted, etc
   *          Only emits once all request related to the DSO has been invalidated.
   */
  deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.deleteByHref(href, copyVirtualMetadata);
  }
}
