/* eslint-disable max-classes-per-file */
import { Injectable } from '@angular/core';
import { BaseItemDataService } from './item-data.service';
import { Item } from '../shared/item.model';
import { RemoteData } from './remote-data';
import { Observable } from 'rxjs';
import { DSOChangeAnalyzer } from './dso-change-analyzer.service';
import { RequestService } from './request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { BrowseService } from '../browse/browse.service';
import { CollectionDataService } from './collection-data.service';
import { switchMap } from 'rxjs/operators';
import { BundleDataService } from './bundle-data.service';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { IdentifiableDataService } from './base/identifiable-data.service';
import { CreateDataImpl } from './base/create-data';

/**
 * Data service for interacting with Item templates via their Collection
 */
class CollectionItemTemplateDataService extends IdentifiableDataService<Item> {
  private createData: CreateDataImpl<Item>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected collectionService: CollectionDataService,
  ) {
    super('itemtemplates', requestService, rdbService, objectCache, halService, undefined);

    // We only intend to use createOnEndpoint, so this inner data service feature doesn't need an endpoint at all
    this.createData = new CreateDataImpl<Item>(undefined, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
  }

  /**
   * Create an observable for the HREF of a specific object based on its identifier
   *
   * Overridden to ensure that {@link findById} works with Collection IDs and points to the template.
   * @param collectionID  the ID of a Collection
   */
  public getIDHrefObs(collectionID: string): Observable<string> {
    return this.collectionService.getIDHrefObs(collectionID).pipe(
      switchMap((href: string) => this.halService.getEndpoint('itemtemplate', href)),
    );
  }

  /**
   * Create a new item template for a Collection by ID
   * @param item
   * @param collectionID
   */
  public createTemplate(item: Item, collectionID: string): Observable<RemoteData<Item>> {
    return this.createData.createOnEndpoint(item, this.getIDHrefObs(collectionID));
  }
}

/**
 * A service responsible for fetching/sending data from/to the REST API on a collection's itemtemplates endpoint
 */
@Injectable()
export class ItemTemplateDataService extends BaseItemDataService {
  private byCollection: CollectionItemTemplateDataService;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected comparator: DSOChangeAnalyzer<Item>,
    protected browseService: BrowseService,
    protected bundleService: BundleDataService,
    protected collectionService: CollectionDataService,
  ) {
    super('itemtemplates', requestService, rdbService, objectCache, halService, notificationsService, comparator, browseService, bundleService);

    this.byCollection = new CollectionItemTemplateDataService(requestService, rdbService, objectCache, halService, notificationsService, collectionService);
  }

  /**
   * Find an item template by collection ID
   * @param collectionID
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findByCollectionID(collectionID: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Item>[]): Observable<RemoteData<Item>> {
    return this.byCollection.findById(collectionID, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
  }

  /**
   * Create a new item template for a collection by ID
   * @param item
   * @param collectionID
   */
  createByCollectionID(item: Item, collectionID: string): Observable<RemoteData<Item>> {
    return this.byCollection.createTemplate(item, collectionID);
  }

  /**
   * Get the endpoint based on a collection
   * @param collectionID  The ID of the collection to base the endpoint on
   */
  getCollectionEndpoint(collectionID: string): Observable<string> {
    return this.byCollection.getIDHrefObs(collectionID);
  }
}
