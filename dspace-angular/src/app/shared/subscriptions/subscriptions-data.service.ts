import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, switchMap, take } from 'rxjs/operators';


import { NotificationsService } from '../notifications/notifications.service';
import { RemoteDataBuildService } from '../../core/cache/builders/remote-data-build.service';
import { RequestParam } from '../../core/cache/models/request-param.model';
import { ObjectCacheService } from '../../core/cache/object-cache.service';
import { DSOChangeAnalyzer } from '../../core/data/dso-change-analyzer.service';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { CreateRequest, PutRequest } from '../../core/data/request.models';
import { FindListOptions } from '../../core/data/find-list-options.model';
import { RestRequest } from '../../core/data/rest-request.model';

import { RequestService } from '../../core/data/request.service';
import { HALEndpointService } from '../../core/shared/hal-endpoint.service';
import { Subscription } from './models/subscription.model';
import { dataService } from '../../core/data/base/data-service.decorator';
import { SUBSCRIPTION } from './models/subscription.resource-type';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { NoContent } from '../../core/shared/NoContent.model';
import { isNotEmpty, isNotEmptyOperator } from '../empty.util';

import { getFirstCompletedRemoteData } from '../../core/shared/operators';
import { sendRequest } from 'src/app/core/shared/request.operators';
import { IdentifiableDataService } from '../../core/data/base/identifiable-data.service';
import { DeleteDataImpl } from '../../core/data/base/delete-data';
import { SearchDataImpl } from '../../core/data/base/search-data';
import { FindAllData } from '../../core/data/base/find-all-data';
import { followLink } from '../utils/follow-link-config.model';

/**
 * Provides methods to retrieve subscription resources from the REST API related CRUD actions.
 */
@Injectable({
  providedIn: 'root'
})
@dataService(SUBSCRIPTION)
export class SubscriptionsDataService extends IdentifiableDataService<Subscription> {
  protected findByEpersonLinkPath = 'findByEPerson';

  private deleteData: DeleteDataImpl<Subscription>;
  private findAllData: FindAllData<Subscription>;
  private searchData: SearchDataImpl<Subscription>;

  constructor(
    protected comparator: DSOChangeAnalyzer<Subscription>,
    protected http: HttpClient,
    protected notificationsService: NotificationsService,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected store: Store<any>,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected nameService: DSONameService,
  ) {
    super('subscriptions', requestService, rdbService, objectCache, halService);

    this.searchData = new SearchDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, this.responseMsToLive);
    this.deleteData = new DeleteDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive, this.constructIdEndpoint);
  }
  /**
   * Get subscriptions for a given item or community or collection & eperson.
   *
   * @param eperson The eperson to search for
   * @param uuid The uuid of the dsobjcet to search for
   */
  getSubscriptionsByPersonDSO(eperson: string, uuid: string): Observable<RemoteData<PaginatedList<Subscription>>> {

    const optionsWithObject = Object.assign(new FindListOptions(), {
      searchParams: [
        new RequestParam('resource', uuid),
        new RequestParam('eperson_id', eperson)
      ]
    });

    return this.searchData.searchBy('findByEPersonAndDso', optionsWithObject, false, true);
  }

  /**
   * Create a subscription for a given item or community or collection.
   *
   * @param subscription The subscription to create
   * @param ePerson The ePerson to create for
   * @param uuid The uuid of the dsobjcet to create for
   */
  createSubscription(subscription: Subscription, ePerson: string, uuid: string): Observable<RemoteData<Subscription>> {

    return this.halService.getEndpoint(this.linkPath).pipe(
      isNotEmptyOperator(),
      take(1),
      map((endpointUrl: string) => `${endpointUrl}?resource=${uuid}&eperson_id=${ePerson}`),
      map((endpointURL: string) => new CreateRequest(this.requestService.generateRequestId(), endpointURL, JSON.stringify(subscription))),
      sendRequest(this.requestService),
      switchMap((restRequest: RestRequest) => this.rdbService.buildFromRequestUUID(restRequest.uuid)),
      getFirstCompletedRemoteData(),
    ) as Observable<RemoteData<Subscription>>;
  }

  /**
   * Update a subscription for a given item or community or collection.
   *
   * @param subscription The subscription to update
   * @param ePerson The ePerson to update for
   * @param uuid The uuid of the dsobjcet to update for
   */
  updateSubscription(subscription, ePerson: string, uuid: string) {

    return this.halService.getEndpoint(this.linkPath).pipe(
      isNotEmptyOperator(),
      take(1),
      map((endpointUrl: string) => `${endpointUrl}/${subscription.id}?resource=${uuid}&eperson_id=${ePerson}`),
      map((endpointURL: string) => new PutRequest(this.requestService.generateRequestId(), endpointURL, JSON.stringify(subscription))),
      sendRequest(this.requestService),
      switchMap((restRequest: RestRequest) => this.rdbService.buildFromRequestUUID(restRequest.uuid)),
      getFirstCompletedRemoteData(),
    ) as Observable<RemoteData<Subscription>>;
  }


  /**
   * Deletes the subscription with a give id
   *
   * @param id  the id of Subscription to delete
   */
  deleteSubscription(id: string): Observable<RemoteData<NoContent>> {
    return this.halService.getEndpoint(this.linkPath).pipe(
      filter((href: string) => isNotEmpty(href)),
      distinctUntilChanged(),
      switchMap((endpointUrl) => this.deleteData.delete(id)),
      getFirstCompletedRemoteData(),
    );
  }

  /**
   * Retrieves the list of subscription with {@link dSpaceObject} and {@link ePerson}
   *
   * @param options                     options for the find all request
   */
  findAllSubscriptions(options?): Observable<RemoteData<PaginatedList<Subscription>>> {
    return this.findAllData.findAll(options, true, true, followLink('resource'), followLink('eperson'));
  }


  /**
   * Retrieves the list of subscription with {@link dSpaceObject} and {@link ePerson}
   *
   * @param ePersonId  The eperson id
   * @param options    The options for the find all request
   */
  findByEPerson(ePersonId: string, options?: FindListOptions): Observable<RemoteData<PaginatedList<Subscription>>> {
    const optionsWithObject = Object.assign(new FindListOptions(), options, {
      searchParams: [
        new RequestParam('uuid', ePersonId)
      ]
    });

    // return this.searchData.searchBy(this.findByEpersonLinkPath, optionsWithObject, true, true, followLink('dSpaceObject'), followLink('ePerson'));

    return this.getEndpoint().pipe(
      map(href => `${href}/search/${this.findByEpersonLinkPath}`),
      switchMap(href => this.findListByHref(href, optionsWithObject, false, true, followLink('resource'), followLink('eperson')))
    );


  }

}
