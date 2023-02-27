import { Component, Injector } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import {
  AdvancedClaimedTaskActionsAbstractComponent
} from '../abstract/advanced-claimed-task-actions-abstract.component';
import {
  ADVANCED_WORKFLOW_ACTION_RATING,
  ADVANCED_WORKFLOW_TASK_OPTION_RATING,
} from '../../../../workflowitems-edit-page/advanced-workflow-action/advanced-workflow-action-rating/advanced-workflow-action-rating.component';
import { rendersWorkflowTaskOption } from '../switcher/claimed-task-actions-decorator';

/**
 * Advanced Workflow button that redirect to the {@link AdvancedWorkflowActionRatingComponent}
 */
@rendersWorkflowTaskOption(ADVANCED_WORKFLOW_TASK_OPTION_RATING)
@Component({
  selector: 'ds-advanced-claimed-task-action-rating-reviewer',
  templateUrl: './advanced-claimed-task-action-rating.component.html',
  styleUrls: ['./advanced-claimed-task-action-rating.component.scss']
})
export class AdvancedClaimedTaskActionRatingComponent extends AdvancedClaimedTaskActionsAbstractComponent {

  /**
   * This component represents the advanced select option
   */
  option = ADVANCED_WORKFLOW_TASK_OPTION_RATING;

  workflowType = ADVANCED_WORKFLOW_ACTION_RATING;

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
