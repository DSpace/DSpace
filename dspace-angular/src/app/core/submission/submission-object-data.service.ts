import { Injectable } from '@angular/core';
import { of as observableOf, Observable } from 'rxjs';
import { FollowLinkConfig } from '../../shared/utils/follow-link-config.model';
import { SubmissionService } from '../../submission/submission.service';
import { RemoteData } from '../data/remote-data';
import { SubmissionObject } from './models/submission-object.model';
import { SubmissionScopeType } from './submission-scope-type';
import { WorkflowItemDataService } from './workflowitem-data.service';
import { WorkspaceitemDataService } from './workspaceitem-data.service';
import { map } from 'rxjs/operators';
import { HALEndpointService } from '../shared/hal-endpoint.service';
import { environment } from '../../../environments/environment';
import { RequestEntryState } from '../data/request-entry-state.model';
import { IdentifiableDataService } from '../data/base/identifiable-data.service';

/**
 * A service to retrieve submission objects (WorkspaceItem/WorkflowItem)
 * without knowing their type
 */
@Injectable({
  providedIn: 'root'
})
export class SubmissionObjectDataService {
  constructor(
    private workspaceitemDataService: WorkspaceitemDataService,
    private workflowItemDataService: WorkflowItemDataService,
    private submissionService: SubmissionService,
    private halService: HALEndpointService
  ) {
  }

  /**
   * Create the HREF for a specific object based on its identifier
   * @param id The identifier for the object
   */
  getHrefByID(id): Observable<string> {
    const dataService: IdentifiableDataService<SubmissionObject> = this.submissionService.getSubmissionScope() === SubmissionScopeType.WorkspaceItem ? this.workspaceitemDataService : this.workflowItemDataService;

    return this.halService.getEndpoint(dataService.getLinkPath()).pipe(
      map((endpoint: string) => dataService.getIDHref(endpoint, encodeURIComponent(id))));
  }

  /**
   * Retrieve a submission object based on its ID.
   *
   * @param id                          The identifier of a submission object
   * @param useCachedVersionIfAvailable If this is true, the request will only be sent if there's
   *                                    no valid cached version. Defaults to true
   * @param reRequestOnStale            Whether or not the request should automatically be re-
   *                                    requested after the response becomes stale
   * @param linksToFollow               List of {@link FollowLinkConfig} that indicate which
   *                                    {@link HALLink}s should be automatically resolved
   */
  findById(id: string, useCachedVersionIfAvailable = true, reRequestOnStale = true, ...linksToFollow: FollowLinkConfig<SubmissionObject>[]): Observable<RemoteData<SubmissionObject>> {
    switch (this.submissionService.getSubmissionScope()) {
      case SubmissionScopeType.WorkspaceItem:
        return this.workspaceitemDataService.findById(id, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
      case SubmissionScopeType.WorkflowItem:
        return this.workflowItemDataService.findById(id, useCachedVersionIfAvailable, reRequestOnStale, ...linksToFollow);
      default:
        const now = new Date().getTime();
        return observableOf(new RemoteData(
          now,
          environment.cache.msToLive.default,
          now,
          RequestEntryState.Error,
          'The request couldn\'t be sent. Unable to determine the type of submission object',
          undefined,
          400
        ));
    }
  }
}
