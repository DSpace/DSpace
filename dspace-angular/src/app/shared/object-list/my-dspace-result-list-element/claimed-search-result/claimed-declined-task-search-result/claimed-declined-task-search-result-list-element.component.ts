import { Component, Inject, OnInit } from '@angular/core';
import { listableObjectComponent } from '../../../../object-collection/shared/listable-object/listable-object.decorator';
import { ClaimedDeclinedTaskTaskSearchResult } from 'src/app/shared/object-collection/shared/claimed-declined-task-task-search-result.model';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { LinkService } from '../../../../../core/cache/builders/link.service';
import { TruncatableService } from '../../../../truncatable/truncatable.service';
import { MyDspaceItemStatusType } from '../../../../object-collection/shared/mydspace-item-status/my-dspace-item-status-type';
import { Observable } from 'rxjs';
import { RemoteData } from '../../../../../core/data/remote-data';
import { WorkflowItem } from '../../../../../core/submission/models/workflowitem.model';
import { followLink } from '../../../../utils/follow-link-config.model';
import { SearchResultListElementComponent } from '../../../search-result-list-element/search-result-list-element.component';
import { ClaimedTaskSearchResult } from '../../../../object-collection/shared/claimed-task-search-result.model';
import { ClaimedTask } from '../../../../../core/tasks/models/claimed-task-object.model';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { APP_CONFIG, AppConfig } from '../../../../../../config/app-config.interface';

/**
 * This component renders claimed task declined task object for the search result in the list view.
 */
@Component({
  selector: 'ds-claimed-declined-task-search-result-list-element',
  styleUrls: ['../../../search-result-list-element/search-result-list-element.component.scss'],
  templateUrl: './claimed-declined-task-search-result-list-element.component.html'
})
@listableObjectComponent(ClaimedDeclinedTaskTaskSearchResult, ViewMode.ListElement)
export class ClaimedDeclinedTaskSearchResultListElementComponent extends SearchResultListElementComponent<ClaimedTaskSearchResult, ClaimedTask> implements OnInit {

  /**
   * A boolean representing if to show submitter information
   */
  public showSubmitter = true;

  /**
   * Represent item's status
   */
  public status = MyDspaceItemStatusType.DECLINED_TASk;

  /**
   * The workflowitem object that belonging to the result object
   */
  public workflowitemRD$: Observable<RemoteData<WorkflowItem>>;

  public constructor(
    protected linkService: LinkService,
    protected truncatableService: TruncatableService,
    protected dsoNameService: DSONameService,
    @Inject(APP_CONFIG) protected appConfig: AppConfig,
  ) {
    super(truncatableService, dsoNameService, appConfig);
  }

  /**
   * Initialize all instance variables
   */
  ngOnInit() {
    super.ngOnInit();
    this.linkService.resolveLinks(this.dso,
      followLink('workflowitem',
        { useCachedVersionIfAvailable: false },
        followLink('item'),
        followLink('submitter')
      ),
      followLink('action'));
    this.workflowitemRD$ = this.dso.workflowitem as Observable<RemoteData<WorkflowItem>>;
  }

}
