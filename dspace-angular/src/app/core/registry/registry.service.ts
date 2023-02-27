import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { RemoteData } from '../data/remote-data';
import { PaginatedList } from '../data/paginated-list.model';
import { hasValue, hasValueOperator, isNotEmptyOperator } from '../../shared/empty.util';
import { getFirstSucceededRemoteDataPayload } from '../shared/operators';
import { createSelector, select, Store } from '@ngrx/store';
import { AppState } from '../../app.reducer';
import { MetadataRegistryState } from '../../admin/admin-registries/metadata-registry/metadata-registry.reducers';
import {
  MetadataRegistryCancelFieldAction,
  MetadataRegistryCancelSchemaAction,
  MetadataRegistryDeselectAllFieldAction,
  MetadataRegistryDeselectAllSchemaAction,
  MetadataRegistryDeselectFieldAction,
  MetadataRegistryDeselectSchemaAction,
  MetadataRegistryEditFieldAction,
  MetadataRegistryEditSchemaAction,
  MetadataRegistrySelectFieldAction,
  MetadataRegistrySelectSchemaAction
} from '../../admin/admin-registries/metadata-registry/metadata-registry.actions';
import { map, mergeMap, tap } from 'rxjs/operators';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { MetadataSchema } from '../metadata/metadata-schema.model';
import { MetadataField } from '../metadata/metadata-field.model';
import { MetadataSchemaDataService } from '../data/metadata-schema-data.service';
import { MetadataFieldDataService } from '../data/metadata-field-data.service';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RequestParam } from '../cache/models/request-param.model';
import { NoContent } from '../shared/NoContent.model';
import { FindListOptions } from '../data/find-list-options.model';

const metadataRegistryStateSelector = (state: AppState) => state.metadataRegistry;
const editMetadataSchemaSelector = createSelector(metadataRegistryStateSelector, (metadataState: MetadataRegistryState) => metadataState.editSchema);
const selectedMetadataSchemasSelector = createSelector(metadataRegistryStateSelector, (metadataState: MetadataRegistryState) => metadataState.selectedSchemas);
const editMetadataFieldSelector = createSelector(metadataRegistryStateSelector, (metadataState: MetadataRegistryState) => metadataState.editField);
const selectedMetadataFieldsSelector = createSelector(metadataRegistryStateSelector, (metadataState: MetadataRegistryState) => metadataState.selectedFields);

/**
 * Service for registry related CRUD actions such as metadata schema, metadata field and bitstream format
 */
@Injectable()
export class RegistryService {

  constructor(private store: Store<AppState>,
              private notificationsService: NotificationsService,
              private translateService: TranslateService,
              private metadataSchemaService: MetadataSchemaDataService,
              private metadataFieldService: MetadataFieldDataService) {

  }

  /**
   * Retrieves all metadata schemas
   * @param options           The options used to retrieve the schemas
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  public getMetadataSchemas(options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataSchema>[]): Observable<RemoteData<PaginatedList<MetadataSchema>>> {
    return this.metadataSchemaService.findAll(options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Retrieves a metadata schema by its prefix
   * @param prefix                      The prefux of the schema to find
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  public getMetadataSchemaByPrefix(prefix: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataSchema>[]): Observable<RemoteData<MetadataSchema>> {
    // Temporary options to get ALL metadataschemas until there's a rest api endpoint for fetching a specific schema
    const options: FindListOptions = Object.assign(new FindListOptions(), {
      elementsPerPage: 10000
    });
    return this.getMetadataSchemas(options).pipe(
      getFirstSucceededRemoteDataPayload(),
      map((schemas: PaginatedList<MetadataSchema>) => schemas.page),
      isNotEmptyOperator(),
      map((schemas: MetadataSchema[]) => schemas.filter((schema) => schema.prefix === prefix)[0]),
      mergeMap((schema: MetadataSchema) => this.metadataSchemaService.findById(`${schema.id}`, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow))
    );
  }

  /**
   * retrieves all metadata fields that belong to a certain metadata schema
   * @param schema                      The schema to filter by
   * @param options                     The options info used to retrieve the fields
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  public getMetadataFieldsBySchema(schema: MetadataSchema, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataField>[]): Observable<RemoteData<PaginatedList<MetadataField>>> {
    return this.metadataFieldService.findBySchema(schema, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  public editMetadataSchema(schema: MetadataSchema) {
    this.store.dispatch(new MetadataRegistryEditSchemaAction(schema));
  }

  /**
   * Method to cancel editing a metadata schema, dispatches a cancel schema action
   */
  public cancelEditMetadataSchema() {
    this.store.dispatch(new MetadataRegistryCancelSchemaAction());
  }

  /**
   * Method to retrieve the metadata schema that are currently being edited
   */
  public getActiveMetadataSchema(): Observable<MetadataSchema> {
    return this.store.pipe(select(editMetadataSchemaSelector));
  }

  /**
   * Method to select a metadata schema, dispatches a select schema action
   * @param schema The schema that's being selected
   */
  public selectMetadataSchema(schema: MetadataSchema) {
    this.store.dispatch(new MetadataRegistrySelectSchemaAction(schema));
  }

  /**
   * Method to deselect a metadata schema, dispatches a deselect schema action
   * @param schema The schema that's it being deselected
   */
  public deselectMetadataSchema(schema: MetadataSchema) {
    this.store.dispatch(new MetadataRegistryDeselectSchemaAction(schema));
  }

  /**
   * Method to deselect all currently selected metadata schema, dispatches a deselect all schema action
   */
  public deselectAllMetadataSchema() {
    this.store.dispatch(new MetadataRegistryDeselectAllSchemaAction());
  }

  /**
   * Method to retrieve the metadata schemas that are currently selected
   */
  public getSelectedMetadataSchemas(): Observable<MetadataSchema[]> {
    return this.store.pipe(select(selectedMetadataSchemasSelector));
  }

  /**
   * Method to start editing a metadata field, dispatches an edit field action
   * @param field The field that's being edited
   */
  public editMetadataField(field: MetadataField) {
    this.store.dispatch(new MetadataRegistryEditFieldAction(field));
  }

  /**
   * Method to cancel editing a metadata field, dispatches a cancel field action
   */
  public cancelEditMetadataField() {
    this.store.dispatch(new MetadataRegistryCancelFieldAction());
  }

  /**
   * Method to retrieve the metadata field that are currently being edited
   */
  public getActiveMetadataField(): Observable<MetadataField> {
    return this.store.pipe(select(editMetadataFieldSelector));
  }

  /**
   * Method to select a metadata field, dispatches a select field action
   * @param field The field that's being selected
   */
  public selectMetadataField(field: MetadataField) {
    this.store.dispatch(new MetadataRegistrySelectFieldAction(field));
  }

  /**
   * Method to deselect a metadata field, dispatches a deselect field action
   * @param field The field that's it being deselected
   */
  public deselectMetadataField(field: MetadataField) {
    this.store.dispatch(new MetadataRegistryDeselectFieldAction(field));
  }

  /**
   * Method to deselect all currently selected metadata fields, dispatches a deselect all field action
   */
  public deselectAllMetadataField() {
    this.store.dispatch(new MetadataRegistryDeselectAllFieldAction());
  }

  /**
   * Method to retrieve the metadata fields that are currently selected
   */
  public getSelectedMetadataFields(): Observable<MetadataField[]> {
    return this.store.pipe(select(selectedMetadataFieldsSelector));
  }

  /**
   * Create or Update a MetadataSchema
   *  If the MetadataSchema contains an id, it is assumed the schema already exists and is updated instead
   *  Since creating or updating is nearly identical, the only real difference is the request (and slight difference in endpoint):
   *  - On creation, a CreateRequest is used
   *  - On update, a PutRequest is used
   * @param schema    The MetadataSchema to create or update
   */
  public createOrUpdateMetadataSchema(schema: MetadataSchema): Observable<MetadataSchema> {
    const isUpdate = hasValue(schema.id);
    return this.metadataSchemaService.createOrUpdateMetadataSchema(schema).pipe(
      getFirstSucceededRemoteDataPayload(),
      hasValueOperator(),
      tap(() => {
        this.showNotifications(true, isUpdate, false, { prefix: schema.prefix });
      })
    );
  }

  /**
   * Method to delete a metadata schema
   * @param id The id of the metadata schema to delete
   */
  public deleteMetadataSchema(id: number): Observable<RemoteData<NoContent>> {
    return this.metadataSchemaService.delete(`${id}`);
  }

  /**
   * Method that clears a cached metadata schema request and returns its REST url
   */
  public clearMetadataSchemaRequests(): Observable<string> {
    return this.metadataSchemaService.clearRequests();
  }

  /**
   * Create a MetadataField
   *
   * @param field    The MetadataField to create
   * @param schema   The MetadataSchema to create the field in
   */
  public createMetadataField(field: MetadataField, schema: MetadataSchema): Observable<MetadataField> {
    if (!field.qualifier) {
      field.qualifier = null;
    }
    return this.metadataFieldService.create(field, new RequestParam('schemaId', schema.id)).pipe(
      getFirstSucceededRemoteDataPayload(),
      hasValueOperator(),
      tap(() => {
        this.showNotifications(true, false, true, { field: field.toString() });
      })
    );
  }

  /**
   * Update a MetadataField
   *
   * @param field    The MetadataField to update
   */
  public updateMetadataField(field: MetadataField): Observable<MetadataField> {
    if (!field.qualifier) {
      field.qualifier = null;
    }
    return this.metadataFieldService.put(field).pipe(
      getFirstSucceededRemoteDataPayload(),
      hasValueOperator(),
      tap(() => {
        this.showNotifications(true, true, true, { field: field.toString() });
      })
    );
  }

  /**
   * Method to delete a metadata field
   * @param id The id of the metadata field to delete
   */
  public deleteMetadataField(id: number): Observable<RemoteData<NoContent>> {
    return this.metadataFieldService.delete(`${id}`);
  }

  /**
   * Method that clears a cached metadata field request and returns its REST url
   */
  public clearMetadataFieldRequests(): void {
    this.metadataFieldService.clearRequests();
  }

  private showNotifications(success: boolean, edited: boolean, isField: boolean, options: any) {
    const prefix = 'admin.registries.schema.notification';
    const suffix = success ? 'success' : 'failure';
    const editedString = edited ? 'edited' : 'created';
    const messages = observableCombineLatest(
      this.translateService.get(success ? `${prefix}.${suffix}` : `${prefix}.${suffix}`),
      this.translateService.get(`${prefix}${isField ? '.field' : ''}.${editedString}`, options)
    );
    messages.subscribe(([head, content]) => {
      if (success) {
        this.notificationsService.success(head, content);
      } else {
        this.notificationsService.error(head, content);
      }
    });
  }

  /**
   * Retrieve a filtered paginated list of metadata fields
   * @param query {string}              The query to use for the metadata field name, can be part of
   *                                    the fully qualified field, should start with the start of
   *                                    the schema, element or qualifier (e.g. “dc.ti”,
   *                                    “contributor”, “auth”, “contributor.ot”)
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @returns an observable that emits a remote data object with a page of metadata fields that match the query
   */
  queryMetadataFields(query: string, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<MetadataField>[]): Observable<RemoteData<PaginatedList<MetadataField>>> {
    return this.metadataFieldService.searchByFieldNameParams(null, null, null, query, null, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }
}
