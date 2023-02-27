import { Component, Injector } from '@angular/core';
import { ClaimedTaskActionsAbstractComponent } from '../abstract/claimed-task-actions-abstract.component';
import { rendersWorkflowTaskOption } from '../switcher/claimed-task-actions-decorator';
import { Router } from '@angular/router';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import { DSpaceObject } from '../../../../core/shared/dspace-object.model';
import {
  ClaimedDeclinedTaskTaskSearchResult
} from '../../../object-collection/shared/claimed-declined-task-task-search-result.model';
import { Observable, of as observableOf } from 'rxjs';
import { RemoteData } from 'src/app/core/data/remote-data';

export const WORKFLOW_TASK_OPTION_DECLINE_TASK = 'submit_decline_task';

@rendersWorkflowTaskOption(WORKFLOW_TASK_OPTION_DECLINE_TASK)
@Component({
  selector: 'ds-claimed-task-actions-decline-task',
  templateUrl: './claimed-task-actions-decline-task.component.html',
  styleUrls: ['./claimed-task-actions-decline-task.component.scss']
})
/**
 * Component for displaying and processing the decline task action on a workflow task item
 */
export class ClaimedTaskActionsDeclineTaskComponent extends ClaimedTaskActionsAbstractComponent {

  option = WORKFLOW_TASK_OPTION_DECLINE_TASK;

  constructor(protected injector: Injector,
              protected router: Router,
              protected notificationsService: NotificationsService,
              protected translate: TranslateService,
              protected searchService: SearchService,
              protected requestService: RequestService) {
    super(injector, router, notificationsService, translate, searchService, requestService);
  }

  reloadObjectExecution(): Observable<RemoteData<DSpaceObject> | DSpaceObject> {
    return observableOf(this.object);
  }

  convertReloadedObject(dso: DSpaceObject): DSpaceObject {
    return Object.assign(new ClaimedDeclinedTaskTaskSearchResult(), dso, {
      indexableObject: dso
    });
  }

}
