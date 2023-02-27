import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { BehaviorSubject, Observable, of, combineLatest as observableCombineLatest, } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { ResourcePolicyDataService } from '../../../core/resource-policy/resource-policy-data.service';
import { NotificationsService } from '../../notifications/notifications.service';
import { RemoteData } from '../../../core/data/remote-data';
import { ResourcePolicy } from '../../../core/resource-policy/models/resource-policy.model';
import { ResourcePolicyEvent } from '../form/resource-policy-form.component';
import { RESOURCE_POLICY } from '../../../core/resource-policy/models/resource-policy.resource-type';
import { ITEM_EDIT_AUTHORIZATIONS_PATH } from '../../../item-page/edit-item-page/edit-item-page.routing-paths';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';

@Component({
  selector: 'ds-resource-policy-edit',
  templateUrl: './resource-policy-edit.component.html'
})
export class ResourcePolicyEditComponent implements OnInit {

  /**
   * The resource policy object to edit
   */
  public resourcePolicy: ResourcePolicy;

  /**
   * A boolean representing if a submission editing operation is pending
   * @type {BehaviorSubject<boolean>}
   */
  private processing$ = new BehaviorSubject<boolean>(false);

  /**
   * Initialize instance variables
   *
   * @param {NotificationsService} notificationsService
   * @param {ResourcePolicyDataService} resourcePolicyService
   * @param {ActivatedRoute} route
   * @param {Router} router
   * @param {TranslateService} translate
   */
  constructor(
    private notificationsService: NotificationsService,
    private resourcePolicyService: ResourcePolicyDataService,
    private route: ActivatedRoute,
    private router: Router,
    private translate: TranslateService) {
  }

  /**
   * Initialize the component
   */
  ngOnInit(): void {
    this.route.data.pipe(
      map((data) => data),
      take(1)
    ).subscribe((data: any) => {
      this.resourcePolicy = (data.resourcePolicy as RemoteData<ResourcePolicy>).payload;
    });
  }

  /**
   * Return a boolean representing if an operation is pending
   *
   * @return {Observable<boolean>}
   */
  isProcessing(): Observable<boolean> {
    return this.processing$.asObservable();
  }

  /**
   * Redirect to the authorizations page
   */
  redirectToAuthorizationsPage() {
    this.router.navigate([`../../${ITEM_EDIT_AUTHORIZATIONS_PATH}`], { relativeTo: this.route });
  }

  /**
   * Update a resource policy
   *
   * @param event The {{ResourcePolicyEvent}} emitted
   */
  updateResourcePolicy(event: ResourcePolicyEvent) {
    this.processing$.next(true);
    const updatedObject = Object.assign({}, event.object, {
      id: this.resourcePolicy.id,
      type: RESOURCE_POLICY.value,
      _links: this.resourcePolicy._links
    });

    const updateTargetSucceeded$ = event.updateTarget ? this.resourcePolicyService.updateTarget(
      this.resourcePolicy.id, this.resourcePolicy._links.self.href, event.target.uuid, event.target.type
    ).pipe(
      getFirstCompletedRemoteData(),
      map((responseRD) => responseRD && responseRD.hasSucceeded)
    ) : of(true);

    const updateResourcePolicySucceeded$ = this.resourcePolicyService.update(updatedObject).pipe(
      getFirstCompletedRemoteData(),
      map((responseRD) => responseRD && responseRD.hasSucceeded)
    );

    observableCombineLatest([updateTargetSucceeded$, updateResourcePolicySucceeded$]).subscribe(
      ([updateTargetSucceeded, updateResourcePolicySucceeded]) => {
        this.processing$.next(false);
        if (updateTargetSucceeded && updateResourcePolicySucceeded) {
          this.notificationsService.success(null, this.translate.get('resource-policies.edit.page.success.content'));
          this.redirectToAuthorizationsPage();
        } else if (updateResourcePolicySucceeded) { // everything except target has been updated
          this.notificationsService.error(null, this.translate.get('resource-policies.edit.page.target-failure.content'));
        } else if (updateTargetSucceeded) { // only target has been updated
          this.notificationsService.error(null, this.translate.get('resource-policies.edit.page.other-failure.content'));
        } else { // nothing has been updated
          this.notificationsService.error(null, this.translate.get('resource-policies.edit.page.failure.content'));
        }
      }
    );
  }
}
