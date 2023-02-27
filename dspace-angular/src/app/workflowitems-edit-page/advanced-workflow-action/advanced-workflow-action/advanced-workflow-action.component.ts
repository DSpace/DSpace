import { Component, OnInit } from '@angular/core';
import { WorkflowAction } from '../../../core/tasks/models/workflow-action-object.model';
import { WorkflowActionDataService } from '../../../core/data/workflow-action-data.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { WorkflowItemActionPageComponent } from '../../workflow-item-action-page.component';
import { WorkflowItemDataService } from '../../../core/submission/workflowitem-data.service';
import { RouteService } from '../../../core/services/route.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { getFirstSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { ClaimedTaskDataService } from '../../../core/tasks/claimed-task-data.service';
import { map } from 'rxjs/operators';
import { ProcessTaskResponse } from '../../../core/tasks/models/process-task-response';
import { RequestService } from '../../../core/data/request.service';
import { Location } from '@angular/common';

/**
 * Abstract component for rendering an advanced claimed task's workflow page
 * To create a child-component for a new option:
 * - Set the "{@link getType}()" of the component
 * - Implement the {@link createBody}, should always contain at least the ADVANCED_WORKFLOW_TASK_OPTION_*
 */
@Component({
  selector: 'ds-advanced-workflow-action',
  template: '',
})
export abstract class AdvancedWorkflowActionComponent extends WorkflowItemActionPageComponent implements OnInit {

  workflowAction$: Observable<WorkflowAction>;

  constructor(
    protected route: ActivatedRoute,
    protected workflowItemService: WorkflowItemDataService,
    protected router: Router,
    protected routeService: RouteService,
    protected notificationsService: NotificationsService,
    protected translationService: TranslateService,
    protected workflowActionService: WorkflowActionDataService,
    protected claimedTaskDataService: ClaimedTaskDataService,
    protected requestService: RequestService,
    protected location: Location,
  ) {
    super(route, workflowItemService, router, routeService, notificationsService, translationService, requestService, location);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.workflowAction$ = this.workflowActionService.findById(this.route.snapshot.queryParams.workflow).pipe(
      getFirstSucceededRemoteDataPayload(),
    );
  }

  /**
   * Performs the action and shows a notification based on the outcome of the action
   */
  performAction(): void {
    this.sendRequest(this.route.snapshot.queryParams.claimedTask).subscribe((successful: boolean) => {
      if (successful) {
        const title = this.translationService.get('workflow-item.' + this.type + '.notification.success.title');
        const content = this.translationService.get('workflow-item.' + this.type + '.notification.success.content');
        this.notificationsService.success(title, content);
        this.previousPage();
      } else {
        const title = this.translationService.get('workflow-item.' + this.type + '.notification.error.title');
        const content = this.translationService.get('workflow-item.' + this.type + '.notification.error.content');
        this.notificationsService.error(title, content);
      }
    });
  }

  /**
   * Submits the task with the given {@link createBody}.
   *
   * @param id The task id
   */
  sendRequest(id: string): Observable<boolean> {
    return this.claimedTaskDataService.submitTask(id, this.createBody()).pipe(
      map((processTaskResponse: ProcessTaskResponse) => processTaskResponse.hasSucceeded),
    );
  }

  /**
   * The body that needs to be passed to the {@link ClaimedTaskDataService}.submitTask().
   */
  abstract createBody(): any;

}
