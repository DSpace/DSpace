import { Component, Inject } from '@angular/core';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { ClaimedApprovedTaskSearchResult } from '../../../../object-collection/shared/claimed-approved-task-search-result.model';
import { listableObjectComponent } from '../../../../object-collection/shared/listable-object/listable-object.decorator';
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
 * This component renders claimed task approved object for the search result in the list view.
 */
@Component({
  selector: 'ds-claimed-approved-search-result-list-element',
  styleUrls: ['../../../search-result-list-element/search-result-list-element.component.scss'],
  templateUrl: './claimed-approved-search-result-list-element.component.html'
})
@listableObjectComponent(ClaimedApprovedTaskSearchResult, ViewMode.ListElement)
export class ClaimedApprovedSearchResultListElementComponent extends SearchResultListElementComponent<ClaimedTaskSearchResult, ClaimedTask> {

  /**
   * A boolean representing if to show submitter information
   */
  public showSubmitter = true;

  /**
   * Represent item's status
   */
  public status = MyDspaceItemStatusType.APPROVED;

  /**
   * The workflowitem object that belonging to the result object
   */
  public workflowitemRD$: Observable<RemoteData<WorkflowItem>>;

  public constructor(
    protected linkService: LinkService,
    protected truncatableService: TruncatableService,
    protected dsoNameService: DSONameService,
    @Inject(APP_CONFIG) protected appConfig: AppConfig
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
      followLink('action')
    );
    this.workflowitemRD$ = this.dso.workflowitem as Observable<RemoteData<WorkflowItem>>;
  }

}
