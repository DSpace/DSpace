import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Feedback } from './models/feedback.model';
import { FEEDBACK } from './models/feedback.resource-type';
import { RequestService } from '../data/request.service';
import { RemoteDataBuildService } from '../cache/builders/remote-data-build.service';
import { Store } from '@ngrx/store';
import { ObjectCacheService } from '../cache/object-cache.service';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { getFirstSucceededRemoteData, getRemoteDataPayload } from '../shared/operators';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';
import { RemoteData } from '../data/remote-data';
import { RequestParam } from '../cache/models/request-param.model';
import { CreateData, CreateDataImpl } from '../data/base/create-data';
import { dataService } from '../data/base/data-service.decorator';

/**
 * Service for checking and managing the feedback
 */
@Injectable()
@dataService(FEEDBACK)
export class FeedbackDataService extends IdentifiableDataService<Feedback> implements CreateData<Feedback> {
  private createData: CreateDataImpl<Feedback>;

  constructor(
    protected requestService: RequestService,
    protected rdbService: RemoteDataBuildService,
    protected store: Store<any>,
    protected objectCache: ObjectCacheService,
    protected halService: HALEndpointService,
    protected notificationsService: NotificationsService,
  ) {
    super('feedbacks', requestService, rdbService, objectCache, halService);

    this.createData = new CreateDataImpl(this.linkPath, requestService, rdbService, objectCache, halService, notificationsService, this.responseMsToLive);
  }

  /**
   * Get feedback from its id
   * @param uuid string the id of the feedback
   */
  getFeedback(uuid: string): Observable<Feedback> {
    return this.findById(uuid).pipe(
      getFirstSucceededRemoteData(),
      getRemoteDataPayload(),
    );
  }


  /**
   * Create a new object on the server, and store the response in the object cache
   *
   * @param object    The object to create
   * @param params    Array with additional params to combine with query string
   */
  public create(object: Feedback, ...params: RequestParam[]): Observable<RemoteData<Feedback>> {
    return this.createData.create(object, ...params);
  }
}
