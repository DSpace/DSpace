import { Component, Injector } from '@angular/core';
import { ClaimedTaskActionsAbstractComponent } from '../abstract/claimed-task-actions-abstract.component';
import { rendersWorkflowTaskOption } from '../switcher/claimed-task-actions-decorator';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import { RemoteData } from '../../../../core/data/remote-data';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import { PoolTaskDataService } from '../../../../core/tasks/pool-task-data.service';
import { take } from 'rxjs/operators';

export const WORKFLOW_TASK_OPTION_RETURN_TO_POOL = 'return_to_pool';

@rendersWorkflowTaskOption(WORKFLOW_TASK_OPTION_RETURN_TO_POOL)
@Component({
  selector: 'ds-claimed-task-actions-return-to-pool',
  styleUrls: ['./claimed-task-actions-return-to-pool.component.scss'],
  templateUrl: './claimed-task-actions-return-to-pool.component.html',
})
/**
 * Component for displaying and processing the return to pool action on a workflow task item
 */
export class ClaimedTaskActionsReturnToPoolComponent extends ClaimedTaskActionsAbstractComponent {
  /**
   * This component represents the return to pool option
   */
  option = WORKFLOW_TASK_OPTION_RETURN_TO_POOL;

  constructor(protected injector: Injector,
              protected router: Router,
              protected notificationsService: NotificationsService,
              protected translate: TranslateService,
              protected searchService: SearchService,
              protected requestService: RequestService,
              private poolTaskService: PoolTaskDataService) {
    super(injector, router, notificationsService, translate, searchService, requestService);
  }

  reloadObjectExecution(): Observable<RemoteData<DSpaceObject> | DSpaceObject> {
    return this.poolTaskService.findByItem(this.itemUuid).pipe(take(1));
  }

  actionExecution(): Observable<any> {
    return this.objectDataService.returnToPoolTask(this.object.id);
  }

}
