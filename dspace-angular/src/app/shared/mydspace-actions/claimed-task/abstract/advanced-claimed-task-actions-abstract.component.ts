import { Component, OnInit, Injector } from '@angular/core';
import { ClaimedTaskActionsAbstractComponent } from './claimed-task-actions-abstract.component';
import { getFirstSucceededRemoteDataPayload } from '../../../../core/shared/operators';
import { WorkflowItem } from '../../../../core/submission/models/workflowitem.model';
import { getAdvancedWorkflowRoute } from '../../../../workflowitems-edit-page/workflowitems-edit-page-routing-paths';
import { Params, Router, ActivatedRoute, NavigationExtras } from '@angular/router';
import { NotificationsService } from '../../../notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';

/**
 * Abstract component for rendering an advanced claimed task's action
 * To create a child-component for a new option:
 * - Set the "{@link option}" and "{@link workflowType}" of the component
 * - Add a @{@link rendersWorkflowTaskOption} annotation to your component providing the same enum value
 */
@Component({
  selector: 'ds-advanced-claimed-task-action-abstract',
  template: ''
})
export abstract class AdvancedClaimedTaskActionsAbstractComponent extends ClaimedTaskActionsAbstractComponent implements OnInit {

  /**
   * The {@link WorkflowAction} id of the advanced workflow that needs to be opened.
   */
  abstract workflowType: string;

  /**
   * Route to the workflow's task page
   */
  workflowTaskPageRoute: string;

  constructor(
    protected injector: Injector,
    protected router: Router,
    protected notificationsService: NotificationsService,
    protected translate: TranslateService,
    protected searchService: SearchService,
    protected requestService: RequestService,
    protected route: ActivatedRoute,
  ) {
    super(injector, router, notificationsService, translate, searchService, requestService);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.initPageRoute();
  }

  /**
   * Initialise the route to the advanced workflow page
   */
  initPageRoute() {
    this.subs.push(this.object.workflowitem.pipe(
      getFirstSucceededRemoteDataPayload()
    ).subscribe((workflowItem: WorkflowItem) => {
      this.workflowTaskPageRoute = getAdvancedWorkflowRoute(workflowItem.id);
    }));
  }

  /**
   * Navigates to the advanced workflow page based on the {@link workflow}.
   */
  openAdvancedClaimedTaskTab(): void {
    const navigationExtras: NavigationExtras = {
      queryParams: this.getQueryParams(),
    };
    if (Object.keys(this.route.snapshot.queryParams).length > 0) {
      navigationExtras.state = {};
      navigationExtras.state.previousQueryParams = this.route.snapshot.queryParams;
    }
    void this.router.navigate([this.workflowTaskPageRoute], navigationExtras);
  }

  /**
   * The {@link Params} that need to be given to the workflow page.
   */
  getQueryParams(): Params {
    return {
      workflow: this.workflowType,
      claimedTask: this.object.id,
    };
  }

}
