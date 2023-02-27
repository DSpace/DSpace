import { HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { filter, map, switchMap, take } from 'rxjs/operators';
import { hasValue, isNotEmpty, isNotEmptyOperator } from '../../shared/empty.util';
import { NotificationOptions } from '../../shared/notifications/models/notification-options.model';
import { INotification } from '../../shared/notifications/models/notification.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { RequestParam } from '../cache/models/request-param.model';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HttpOptions } from '../dspace-rest/dspace-rest.service';
import { DSpaceSerializer } from '../dspace-rest/dspace.serializer';
import { Collection } from '../shared/collection.model';
import { COLLECTION } from '../shared/collection.resource-type';
import { ContentSource } from '../shared/content-source.model';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { Item } from '../shared/item.model';
import { getFirstCompletedRemoteData } from '../shared/operators';
import { ComColDataService } from './comcol-data.service';
import { CommunityDataService } from './community-data.service';
import { DSOChangeAnalyzer } from './dso-change-analyzer.service';
import { PaginatedList } from './paginated-list.model';
import { RemoteData } from './remote-data';
import {
  ContentSourceRequest,
  UpdateContentSourceRequest
} from './request.models';
import { RequestService } from './request.service';
import { BitstreamDataService } from './bitstream-data.service';
import { RestRequest } from './rest-request.model';
import { FindListOptions } from './find-list-options.model';
import { Community } from '../shared/community.model';
import { dataService } from './base/data-service.decorator';

@Injectable()
@dataService(COLLECTION)
export class CollectionDataService extends ComColDataService<Collection> {
  protected errorTitle = 'collection.source.update.notifications.error.title';
  protected contentSourceError = 'collection.source.update.notifications.error.content';

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected comparator: DSOChangeAnalyzer<Community>,
    protected notificationsService: NotificationsService,
    protected bitstreamDataService: BitstreamDataService,
    protected communityDataService: CommunityDataService,
    protected translate: TranslateService,
  ) {
    super('collections', requestService, rdbService, objectCache, halService, comparator, notificationsService, bitstreamDataService);
  }

  /**
   * Get all collections the user is authorized to submit to
   *
   * @param query                       limit the returned collection to those with metadata values
   *                                    matching the query terms.
   * @param options                     The [[FindListOptions]] object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   * @return Observable<RemoteData<PaginatedList<Collection>>>
   *    collection list
   */
  getAuthorizedCollection(query: string, options: FindListOptions = {}, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<Collection>[]): Observable<RemoteData<PaginatedList<Collection>>> {
    const searchHref = 'findSubmitAuthorized';
    options = Object.assign({}, options, {
      searchParams: [new RequestParam('query', query)]
    });

    return this.searchBy(searchHref, options, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow).pipe(
      filter((collections: RemoteData<PaginatedList<Collection>>) => !collections.isResponsePending));
  }

  /**
   * Get all collections the user is authorized to submit to
   *
   * @param query limit the returned collection to those with metadata values matching the query terms.
   * @param entityType The entity type used to limit the returned collection
   * @param options The [[FindListOptions]] object
   * @param reRequestOnStale  Whether or not the request should automatically be re-requested after
   *                          the response becomes stale
   * @param linksToFollow The array of [[FollowLinkConfig]]
   * @return Observable<RemoteData<PaginatedList<Collection>>>
   *    collection list
   */
  getAuthorizedCollectionByEntityType(
    query: string,
    entityType: string,
    options: FindListOptions = {},
    reRequestOnStale = true,
    ...linksToFollow: FollowLinkConfig<Collection>[]): Observable<RemoteData<PaginatedList<Collection>>> {
    const searchHref = 'findSubmitAuthorizedByEntityType';
    options = Object.assign({}, options, {
      searchParams: [
        new RequestParam('query', query),
        new RequestParam('entityType', entityType)
      ]
    });

    return this.searchBy(searchHref, options, true, reRequestOnStale, ...linksToFollow).pipe(
      filter((collections: RemoteData<PaginatedList<Collection>>) => !collections.isResponsePending));
  }

  /**
   * Get all collections the user is authorized to submit to, by community
   *
   * @param communityId The community id
   * @param query limit the returned collection to those with metadata values matching the query terms.
   * @param options The [[FindListOptions]] object
   * @param reRequestOnStale Whether or not the request should automatically be re-
   *                         requested after the response becomes stale
   * @return Observable<RemoteData<PaginatedList<Collection>>>
   *    collection list
   */
  getAuthorizedCollectionByCommunity(communityId: string, query: string, options: FindListOptions = {}, reRequestOnStale = true,): Observable<RemoteData<PaginatedList<Collection>>> {
    const searchHref = 'findSubmitAuthorizedByCommunity';
    options = Object.assign({}, options, {
      searchParams: [
        new RequestParam('uuid', communityId),
        new RequestParam('query', query)
      ]
    });

    return this.searchBy(searchHref, options, reRequestOnStale).pipe(
      filter((collections: RemoteData<PaginatedList<Collection>>) => !collections.isResponsePending));
  }
  /**
   * Get all collections the user is authorized to submit to, by community and has the metadata
   *
   * @param communityId The community id
   * @param entityType The entity type used to limit the returned collection
   * @param options The [[FindListOptions]] object
   * @param reRequestOnStale  Whether or not the request should automatically be re-requested after
   *                          the response becomes stale
   * @param linksToFollow The array of [[FollowLinkConfig]]
   * @return Observable<RemoteData<PaginatedList<Collection>>>
   *    collection list
   */
  getAuthorizedCollectionByCommunityAndEntityType(
    communityId: string,
    entityType: string,
    options: FindListOptions = {},
    reRequestOnStale = true,
    ...linksToFollow: FollowLinkConfig<Collection>[]): Observable<RemoteData<PaginatedList<Collection>>> {
    const searchHref = 'findSubmitAuthorizedByCommunityAndEntityType';
    const searchParams = [
      new RequestParam('uuid', communityId),
      new RequestParam('entityType', entityType)
    ];

    options = Object.assign({}, options, {
      searchParams: searchParams
    });

    return this.searchBy(searchHref, options, true, reRequestOnStale, ...linksToFollow).pipe(
      filter((collections: RemoteData<PaginatedList<Collection>>) => !collections.isResponsePending));
  }

  /**
   * Find whether there is a collection whom user has authorization to submit to
   *
   * @return boolean
   *    true if the user has at least one collection to submit to
   */
  hasAuthorizedCollection(): Observable<boolean> {
    const searchHref = 'findSubmitAuthorized';
    const options = new FindListOptions();
    options.elementsPerPage = 1;

    return this.searchBy(searchHref, options).pipe(
      filter((collections: RemoteData<PaginatedList<Collection>>) => !collections.isResponsePending),
      take(1),
      map((collections: RemoteData<PaginatedList<Collection>>) => collections.payload.totalElements > 0)
    );
  }

  /**
   * Get the endpoint for the collection's content harvester
   * @param collectionId
   */
  getHarvesterEndpoint(collectionId: string): Observable<string> {
    return this.halService.getEndpoint(this.linkPath).pipe(
      switchMap((href: string) => this.halService.getEndpoint('harvester', `${href}/${collectionId}`))
    );
  }

  /**
   * Get the collection's content harvester
   * @param collectionId
   */
  getContentSource(collectionId: string, useCachedVersionIfAvailable = true): Observable<RemoteData<ContentSource>> {
    const href$ = this.getHarvesterEndpoint(collectionId).pipe(
      isNotEmptyOperator(),
      take(1)
    );

    href$.subscribe((href: string) => {
      const request = new ContentSourceRequest(this.requestService.generateRequestId(), href);
      this.requestService.send(request, useCachedVersionIfAvailable);
    });

    return this.rdbService.buildSingle<ContentSource>(href$);
  }

  /**
   * Update the settings of the collection's content harvester
   * @param collectionId
   * @param contentSource
   */
  updateContentSource(collectionId: string, contentSource: ContentSource): Observable<ContentSource | INotification> {
    const requestId = this.requestService.generateRequestId();
    const serializedContentSource = new DSpaceSerializer(ContentSource).serialize(contentSource);
    const request$ = this.getHarvesterEndpoint(collectionId).pipe(
      take(1),
      map((href: string) => {
        const options: HttpOptions = Object.create({});
        let headers = new HttpHeaders();
        headers = headers.append('Content-Type', 'application/json');
        options.headers = headers;
        return new UpdateContentSourceRequest(requestId, href, JSON.stringify(serializedContentSource), options);
      })
    );

    // Execute the post/put request
    request$.subscribe((request: RestRequest) => this.requestService.send(request));

    // Return updated ContentSource
    return this.rdbService.buildFromRequestUUID<ContentSource>(requestId).pipe(
      getFirstCompletedRemoteData(),
      map((response: RemoteData<ContentSource>) => {
        if (response.hasFailed) {
          if (hasValue(response.errorMessage)) {
            if (response.statusCode === 422) {
              return this.notificationsService.error(this.translate.instant(this.errorTitle), this.translate.instant(this.contentSourceError), new NotificationOptions(-1));
            } else {
              return this.notificationsService.error(this.translate.instant(this.errorTitle), (response as any).errorMessage, new NotificationOptions(-1));
            }
          }
        } else {
          return response;
        }
      }),
      isNotEmptyOperator(),
      map((response: RemoteData<ContentSource> | INotification) => {
        if (isNotEmpty((response as any).payload)) {
          return (response as RemoteData<ContentSource>).payload;
        }
        return response as INotification;
      })
    );
  }

  protected getFindByParentHref(parentUUID: string): Observable<string> {
    return this.halService.getEndpoint('communities').pipe(
      switchMap((communityEndpointHref: string) =>
        this.halService.getEndpoint('collections', `${communityEndpointHref}/${parentUUID}`)),
    );
  }

  /**
   * Returns {@link RemoteData} of {@link Collection} that is the owning collection of the given item
   * @param item  Item we want the owning collection of
   */
  findOwningCollectionFor(item: Item): Observable<RemoteData<Collection>> {
    return this.findByHref(item._links.owningCollection.href);
  }

  /**
   * Get a list of mapped collections for the given item.
   * @param item  Item for which the mapped collections should be retrieved.
   * @param findListOptions Pagination and search options.
   */
  findMappedCollectionsFor(item: Item, findListOptions?: FindListOptions): Observable<RemoteData<PaginatedList<Collection>>> {
    return this.findListByHref(item._links.mappedCollections.href, findListOptions);
  }


  protected getScopeCommunityHref(options: FindListOptions) {
    return this.communityDataService.getEndpoint().pipe(
      map((endpoint: string) => this.communityDataService.getIDHref(endpoint, options.scopeID)),
      filter((href: string) => isNotEmpty(href)),
      take(1),
    );
  }
}
