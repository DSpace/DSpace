import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { Observable, forkJoin } from 'rxjs';
import { map, switchMap, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { WorkflowItem } from '../core/submission/models/workflowitem.model';
import { Item } from '../core/shared/item.model';
import { ActivatedRoute, Data, Router, Params } from '@angular/router';
import { WorkflowItemDataService } from '../core/submission/workflowitem-data.service';
import { RouteService } from '../core/services/route.service';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { RemoteData } from '../core/data/remote-data';
import { getAllSucceededRemoteData, getRemoteDataPayload } from '../core/shared/operators';
import { isEmpty } from '../shared/empty.util';
import { RequestService } from '../core/data/request.service';

/**
 * Abstract component representing a page to perform an action on a workflow item
 */
@Component({
  selector: 'ds-workflowitem-action-page',
  template: ''
})
export abstract class WorkflowItemActionPageComponent implements OnInit {
  public type;
  public wfi$: Observable<WorkflowItem>;
  public item$: Observable<Item>;
  protected previousQueryParameters?: Params;

  constructor(protected route: ActivatedRoute,
              protected workflowItemService: WorkflowItemDataService,
              protected router: Router,
              protected routeService: RouteService,
              protected notificationsService: NotificationsService,
              protected translationService: TranslateService,
              protected requestService: RequestService,
              protected location: Location,
  ) {
  }

  /**
   * Sets up the type, workflow item and its item object
   */
  ngOnInit() {
    this.type = this.getType();
    this.wfi$ = this.route.data.pipe(map((data: Data) => data.wfi as RemoteData<WorkflowItem>), getRemoteDataPayload());
    this.item$ = this.wfi$.pipe(switchMap((wfi: WorkflowItem) => (wfi.item as Observable<RemoteData<Item>>).pipe(getAllSucceededRemoteData(), getRemoteDataPayload())));
    this.previousQueryParameters = (this.location.getState() as { [key: string]: any }).previousQueryParams;
  }

  /**
   * Performs the action and shows a notification based on the outcome of the action
   */
  performAction() {
    forkJoin([this.wfi$, this.requestService.removeByHrefSubstring('/discover')]).pipe(
      take(1),
      switchMap(([wfi]) => this.sendRequest(wfi.id))
    ).subscribe((successful: boolean) => {
      if (successful) {
        const title = this.translationService.get('workflow-item.' + this.type + '.notification.success.title');
        const content = this.translationService.get('workflow-item.' + this.type + '.notification.success.content');
        this.notificationsService.success(title, content);
      } else {
        const title = this.translationService.get('workflow-item.' + this.type + '.notification.error.title');
        const content = this.translationService.get('workflow-item.' + this.type + '.notification.error.content');
        this.notificationsService.error(title, content);
      }
      this.previousPage();
    });
  }

  /**
   * Navigates to the previous url
   * If there's not previous url, it continues to the mydspace page instead
   */
  previousPage() {
    this.routeService.getPreviousUrl().pipe(take(1))
      .subscribe((url) => {
          let params: Params = {};
          if (isEmpty(url)) {
            url = '/mydspace';
            params = this.previousQueryParameters;
          }
          if (url.split('?').length > 1) {
            for (const param of url.split('?')[1].split('&')) {
              params[param.split('=')[0]] = decodeURIComponent(param.split('=')[1]);
            }
          }
          void this.router.navigate([url.split('?')[0]], { queryParams: params });
        }
      );
  }

  /**
   * Performs the action of this workflow item action page
   * @param id The id of the WorkflowItem
   */
  abstract sendRequest(id: string): Observable<boolean>;

  /**
   * Returns the type of page
   */
  abstract getType(): string;
}
