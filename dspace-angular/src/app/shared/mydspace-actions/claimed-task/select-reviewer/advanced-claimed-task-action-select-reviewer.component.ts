import { Component, Injector } from '@angular/core';
import { rendersWorkflowTaskOption } from '../switcher/claimed-task-actions-decorator';
import {
  AdvancedClaimedTaskActionsAbstractComponent
} from '../abstract/advanced-claimed-task-actions-abstract.component';
import { Router, ActivatedRoute } from '@angular/router';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import {
  ADVANCED_WORKFLOW_ACTION_SELECT_REVIEWER,
  ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER
} from '../../../../workflowitems-edit-page/advanced-workflow-action/advanced-workflow-action-select-reviewer/advanced-workflow-action-select-reviewer.component';

/**
 * Advanced Workflow button that redirect to the {@link AdvancedWorkflowActionSelectReviewerComponent}
 */
@rendersWorkflowTaskOption(ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER)
@Component({
  selector: 'ds-advanced-claimed-task-action-select-reviewer',
  templateUrl: './advanced-claimed-task-action-select-reviewer.component.html',
  styleUrls: ['./advanced-claimed-task-action-select-reviewer.component.scss']
})
export class AdvancedClaimedTaskActionSelectReviewerComponent extends AdvancedClaimedTaskActionsAbstractComponent {

  /**
   * This component represents the advanced select option
   */
  option = ADVANCED_WORKFLOW_TASK_OPTION_SELECT_REVIEWER;

  workflowType = ADVANCED_WORKFLOW_ACTION_SELECT_REVIEWER;

  constructor(
    protected injector: Injector,
    protected router: Router,
    protected notificationsService: NotificationsService,
    protected translate: TranslateService,
    protected searchService: SearchService,
    protected requestService: RequestService,
    protected route: ActivatedRoute,
  ) {
    super(injector, router, notificationsService, translate, searchService, requestService, route);
  }

}
