import { FindListOptions } from '../../core/data/find-list-options.model';
import { FollowLinkConfig } from '../utils/follow-link-config.model';
import { WorkspaceItem } from '../../core/submission/models/workspaceitem.model';
import { Observable, EMPTY } from 'rxjs';
import { RemoteData } from '../../core/data/remote-data';
import { WorkflowItem } from '../../core/submission/models/workflowitem.model';
import { IdentifiableDataServiceStub } from './identifiable-data-service.stub';

/**
 * Stub class for {@link WorkflowActionDataService}
 */
export class WorkflowActionDataServiceStub extends IdentifiableDataServiceStub<WorkflowItem> {

  public findByItem(_uuid: string, _useCachedVersionIfAvailable = false, _reRequestOnStale = true, _options: FindListOptions = {}, ..._linksToFollow: FollowLinkConfig<WorkspaceItem>[]): Observable<RemoteData<WorkspaceItem>> {
    return EMPTY;
  }

}
