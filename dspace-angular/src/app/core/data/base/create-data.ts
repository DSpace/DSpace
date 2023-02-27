/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
import { CacheableObject } from '../../cache/cacheable-object.model';
import { BaseDataService } from './base-data.service';
import { RequestParam } from '../../cache/models/request-param.model';
import { Observable } from 'rxjs';
import { RemoteData } from '../remote-data';
import { hasValue, isNotEmptyOperator } from '../../../shared/empty.util';
import { distinctUntilChanged, map, take, takeWhile } from 'rxjs/operators';
import { DSpaceSerializer } from '../../dspace-rest/dspace.serializer';
import { getClassForType } from '../../cache/builders/build-decorators';
import { CreateRequest } from '../request.models';
import { NotificationOptions } from '../../../shared/notifications/models/notification-options.model';
import { RequestService } from '../request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { HALEndpointService } from '../../shared/hal-endpoint.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { ObjectCacheService } from '../../cache/object-cache.service';

/**
 * Interface for a data service that can create objects.
 */
export interface CreateData<T extends CacheableObject> {
  /**
   * Create a new DSpaceObject on the server, and store the response
   * in the object cache
   *
   * @param object  The object to create
   * @param params  Array with additional params to combine with query string
   */
  create(object: T, ...params: RequestParam[]): Observable<RemoteData<T>>;
}

/**
 * A DataService feature to create objects.
 *
 * Concrete data services can use this feature by implementing {@link CreateData}
 * and delegating its method to an inner instance of this class.
 */
export class CreateDataImpl<T extends CacheableObject> extends BaseDataService<T> implements CreateData<T> {
  constructor(
    protected linkPath: string,
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
    protected responseMsToLive: number,
  ) {
    super(linkPath, requestService, rdbService, objectCache, halService, responseMsToLive);
  }

  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  create(object: T, ...params: RequestParam[]): Observable<RemoteData<T>> {
    const endpoint$ = this.getEndpoint().pipe(
      isNotEmptyOperator(),
      distinctUntilChanged(),
      map((endpoint: string) => this.buildHrefWithParams(endpoint, params)),
    );
    return this.createOnEndpoint(object, endpoint$);
  }

  /**
   * Send a POST request to create a new resource to a specific endpoint.
   * Use this method if the endpoint needs to be adjusted. In most cases {@link create} should be sufficient.
   * @param object     the object to create
   * @param endpoint$  the endpoint to send the POST request to
   */
  createOnEndpoint(object: T, endpoint$: Observable<string>): Observable<RemoteData<T>> {
    const requestId = this.requestService.generateRequestId();
    const serializedObject = new DSpaceSerializer(getClassForType(object.type)).serialize(object);

    endpoint$.pipe(
      take(1),
    ).subscribe((endpoint: string) => {
      const request = new CreateRequest(requestId, endpoint, JSON.stringify(serializedObject));
      if (hasValue(this.responseMsToLive)) {
        request.responseMsToLive = this.responseMsToLive;
      }
      this.requestService.send(request);
    });

    const result$ = this.rdbService.buildFromRequestUUID<T>(requestId);

    // TODO a dataservice is not the best place to show a notification,
    // this should move up to the components that use this method
    result$.pipe(
      takeWhile((rd: RemoteData<T>) => rd.isLoading, true)
    ).subscribe((rd: RemoteData<T>) => {
      if (rd.hasFailed) {
        this.notificationsService.error('Server Error:', rd.errorMessage, new NotificationOptions(-1));
      }
    });

    return result$;
  }
}
