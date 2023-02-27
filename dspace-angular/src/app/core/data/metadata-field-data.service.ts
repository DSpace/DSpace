import { Injectable } from '@angular/core';
import { hasValue } from '../../shared/empty.util';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { METADATA_FIELD } from '../metadata/metadata-field.resource-type';
import { MetadataField } from '../metadata/metadata-field.model';
import { MetadataSchema } from '../metadata/metadata-schema.model';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';
import { RequestParam } from '../cache/models/request-param.model';
import { FindListOptions } from './find-list-options.model';
import { SearchData, SearchDataImpl } from './base/search-data';
import { PutData, PutDataImpl } from './base/put-data';
import { CreateData, CreateDataImpl } from './base/create-data';
import { NoContent } from '../shared/NoContent.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { DeleteData, DeleteDataImpl } from './base/delete-data';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { dataService } from './base/data-service.decorator';

/**
 * A service responsible for fetching/sending data from/to the REST API on the metadatafields endpoint
 */
@Injectable()
@dataService(METADATA_FIELD)
export class MetadataFieldDataService extends IdentifiableDataService<MetadataField> implements CreateData<MetadataField>, PutData<MetadataField>, DeleteData<MetadataField>, SearchData<MetadataField> {
  private createData: CreateData<MetadataField>;
  private searchData: SearchData<MetadataField>;
  private putData: PutData<MetadataField>;
  private deleteData: DeleteData<MetadataField>;

  protected searchBySchemaLinkPath = 'bySchema';
  protected searchByFieldNameLinkPath = 'byFieldName';

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
  ) {
    super('metadatafields', requestService, rdbService, objectCache, halService);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.putData = new PutDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }

  /**
   * Find metadata fields belonging to a metadata schema
   * @param schema                      The metadata schema to list fields for
   * @param options                     The options info used to retrieve the fields
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findBySchema(schema: MetadataSchema, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataField>[]) {
    const optionsWithSchema = Object.assign(new FindListOptions(), options, {
      searchParams: [new RequestParam('schema', schema.prefix)],
    });
    return this.searchBy(this.searchBySchemaLinkPath, optionsWithSchema, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Find metadata fields with either the partial metadata field name (e.g. "dc.ti") as query or an exact match to
   * at least the schema, element or qualifier
   * @param schema    optional; an exact match of the prefix of the metadata schema (e.g. "dc", "dcterms", "eperson")
   * @param element   optional; an exact match of the field's element (e.g. "contributor", "title")
   * @param qualifier optional; an exact match of the field's qualifier (e.g. "author", "alternative")
   * @param query     optional (if any of schema, element or qualifier used) - part of the fully qualified field,
   * should start with the start of the schema, element or qualifier (e.g. “dc.ti”, “contributor”, “auth”, “contributor.ot”)
   * @param exactName optional; the exact fully qualified field, should use the syntax schema.element.qualifier or
   * schema.element if no qualifier exists (e.g. "dc.title", "dc.contributor.author"). It will only return one value
   * if there's an exact match
   * @param options   The options info used to retrieve the fields
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's no valid cached version. Defaults to true
   * @param reRequestOnStale  Whether or not the request should automatically be re-requested after the response becomes stale
   * @param linksToFollow List of {@link FollowLinkConfig} that indicate which {@link HALLink}s should be automatically resolved
   */
  searchByFieldNameParams(schema: string, element: string, qualifier: string, query: string, exactName: string, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataField>[]): Observable<RemoteData<PaginatedList<MetadataField>>> {
    const optionParams = Object.assign(new FindListOptions(), options, {
      searchParams: [
        new RequestParam('schema', hasValue(schema) ? schema : ''),
        new RequestParam('element', hasValue(element) ? element : ''),
        new RequestParam('qualifier', hasValue(qualifier) ? qualifier : ''),
        new RequestParam('query', hasValue(query) ? query : ''),
        new RequestParam('exactName', hasValue(exactName) ? exactName : ''),
      ],
    });
    return this.searchBy(this.searchByFieldNameLinkPath, optionParams, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Finds a specific metadata field by name.
   * @param exactFieldName  The exact fully qualified field, should use the syntax schema.element.qualifier or
   * schema.element if no qualifier exists (e.g. "dc.title", "dc.contributor.author"). It will only return one value
   * if there's an exact match, empty list if there is no exact match.
   */
  findByExactFieldName(exactFieldName: string): Observable<RemoteData<PaginatedList<MetadataField>>> {
    return this.searchByFieldNameParams(null, null, null, null, exactFieldName, null);
  }

  /**
   * Clear all metadata field requests
   * Used for refreshing lists after adding/updating/removing a metadata field from a metadata schema
   */
  clearRequests(): void {
    this.getBrowseEndpoint().pipe(take(1)).subscribe((href: string) => {
      this.requestService.setStaleByHrefSubstring(href);
    });

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
  public deleteByHref(href: string, copyVirtualMetadata?: string[]): Observable<RemoteData<NoContent>> {
    return this.deleteData.deleteByHref(href, copyVirtualMetadata);
  }

  /**
   * Send a PUT request for the specified object
   *
   * @param object The object to send a put request for.
   */
  put(object: MetadataField): Observable<RemoteData<MetadataField>> {
    return this.putData.put(object);
  }

  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  create(object: MetadataField, ...params: RequestParam[]): Observable<RemoteData<MetadataField>> {
    return this.createData.create(object, ...params);
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
  public searchBy(searchMethod: string, options?: FindListOptions, useCachedVersionIfAvailable?: boolean, reRequestOnStale?: boolean, ...linksToFollow: FollowLinkConfig<MetadataField>[]): Observable<RemoteData<PaginatedList<MetadataField>>> {
    return this.searchData.searchBy(searchMethod, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

}
