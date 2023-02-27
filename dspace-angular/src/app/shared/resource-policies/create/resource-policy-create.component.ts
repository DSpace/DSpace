import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { BehaviorSubject, Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { ResourcePolicyDataService } from '../../../core/resource-policy/resource-policy-data.service';
import { NotificationsService } from '../../notifications/notifications.service';
import { RemoteData } from '../../../core/data/remote-data';
import { ResourcePolicy } from '../../../core/resource-policy/models/resource-policy.model';
import { ResourcePolicyEvent } from '../form/resource-policy-form.component';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { ITEM_EDIT_AUTHORIZATIONS_PATH } from '../../../item-page/edit-item-page/edit-item-page.routing-paths';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';

@Component({
  selector: 'ds-resource-policy-create',
  templateUrl: './resource-policy-create.component.html'
})
export class ResourcePolicyCreateComponent implements OnInit {

  /**
   * The name of the resource target of the policy
   */
  public targetResourceName: string;

  /**
   * A boolean representing if a submission creation operation is pending
   * @type {BehaviorSubject<boolean>}
   */
  private processing$ = new BehaviorSubject<boolean>(false);

  /**
   * The uuid of the resource target of the policy
   */
  private targetResourceUUID: string;

  /**
   * Initialize instance variables
   *
   * @param {DSONameService} dsoNameService
   * @param {NotificationsService} notificationsService
   * @param {ResourcePolicyDataService} resourcePolicyService
   * @param {ActivatedRoute} route
   * @param {Router} router
   * @param {TranslateService} translate
   */
  constructor(
    private dsoNameService: DSONameService,
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
      this.targetResourceUUID = (data.resourcePolicyTarget as RemoteData<DSpaceObject>).payload.id;
      this.targetResourceName = this.dsoNameService.getName((data.resourcePolicyTarget as RemoteData<DSpaceObject>).payload);
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
  redirectToAuthorizationsPage(): void {
    this.router.navigate([`../../${ITEM_EDIT_AUTHORIZATIONS_PATH}`], { relativeTo: this.route });
  }

  /**
   * Create a new resource policy
   *
   * @param event The {{ResourcePolicyEvent}} emitted
   */
  createResourcePolicy(event: ResourcePolicyEvent): void {
    this.processing$.next(true);
    let response$;
    if (event.target.type === 'eperson') {
      response$ = this.resourcePolicyService.create(event.object, this.targetResourceUUID, event.target.uuid);
    } else {
      response$ = this.resourcePolicyService.create(event.object, this.targetResourceUUID, null, event.target.uuid);
    }
    response$.pipe(
      getFirstCompletedRemoteData()
    ).subscribe((responseRD: RemoteData<ResourcePolicy>) => {
      this.processing$.next(false);
      if (responseRD.hasSucceeded) {
        this.notificationsService.success(null, this.translate.get('resource-policies.create.page.success.content'));
        this.redirectToAuthorizationsPage();
      } else {
        this.notificationsService.error(null, this.translate.get('resource-policies.create.page.failure.content'));
      }
    });
  }
}
