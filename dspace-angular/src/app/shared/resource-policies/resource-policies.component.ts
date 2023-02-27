import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { BehaviorSubject, from as observableFrom, Observable, Subscription } from 'rxjs';
import { concatMap, distinctUntilChanged, filter, map, reduce, scan, take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

import { getAllSucceededRemoteData } from '../../core/shared/operators';
import { ResourcePolicyDataService } from '../../core/resource-policy/resource-policy-data.service';
import { ResourcePolicy } from '../../core/resource-policy/models/resource-policy.model';
import { DSONameService } from '../../core/breadcrumbs/dso-name.service';
import { GroupDataService } from '../../core/eperson/group-data.service';
import { hasValue, isEmpty, isNotEmpty } from '../empty.util';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { RequestService } from '../../core/data/request.service';
import { NotificationsService } from '../notifications/notifications.service';
import { followLink } from '../utils/follow-link-config.model';
import { ResourcePolicyCheckboxEntry } from './entry/resource-policy-entry.component';

@Component({
  selector: 'ds-resource-policies',
  styleUrls: ['./resource-policies.component.scss'],
  templateUrl: './resource-policies.component.html',
})
/**
 * Component that shows the policies for given resource
 */
export class ResourcePoliciesComponent implements OnInit, OnDestroy {

  /**
   * The resource UUID
   * @type {string}
   */
  @Input() public resourceUUID: string;

  /**
   * The resource type (e.g. 'item', 'bundle' etc) used as key to build automatically translation label
   * @type {string}
   */
  @Input() public resourceType: string;

  /**
   * The resource name
   * @type {string}
   */
  @Input() public resourceName: string;

  /**
   * A boolean representing if component is active
   * @type {boolean}
   */
  private isActive: boolean;

  /**
   * A boolean representing if a submission delete operation is pending
   * @type {BehaviorSubject<boolean>}
   */
  private processingDelete$ = new BehaviorSubject<boolean>(false);

  /**
   * The list of policies for given resource
   * @type {BehaviorSubject<ResourcePolicyCheckboxEntry[]>}
   */
  private resourcePoliciesEntries$: BehaviorSubject<ResourcePolicyCheckboxEntry[]> =
    new BehaviorSubject<ResourcePolicyCheckboxEntry[]>([]);

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  /**
   * Initialize instance variables
   *
   * @param {ChangeDetectorRef} cdr
   * @param {DSONameService} dsoNameService
   * @param {EPersonDataService} ePersonService
   * @param {GroupDataService} groupService
   * @param {NotificationsService} notificationsService
   * @param {RequestService} requestService
   * @param {ResourcePolicyDataService} resourcePolicyService
   * @param {ActivatedRoute} route
   * @param {Router} router
   * @param {TranslateService} translate
   */
  constructor(
    private cdr: ChangeDetectorRef,
    private dsoNameService: DSONameService,
    private ePersonService: EPersonDataService,
    private groupService: GroupDataService,
    private notificationsService: NotificationsService,
    private requestService: RequestService,
    private resourcePolicyService: ResourcePolicyDataService,
    private route: ActivatedRoute,
    private router: Router,
    private translate: TranslateService
  ) {
  }

  /**
   * Initialize the component, setting up the resource's policies
   */
  ngOnInit(): void {
    this.isActive = true;
    this.initResourcePolicyList();
  }

  /**
   * Check if there are any selected resource's policies to be deleted
   *
   * @return {Observable<boolean>}
   */
  canDelete(): Observable<boolean> {
    return observableFrom(this.resourcePoliciesEntries$.value).pipe(
      filter((entry: ResourcePolicyCheckboxEntry) => entry.checked),
      reduce((acc: any, value: any) => [...acc, value], []),
      map((entries: ResourcePolicyCheckboxEntry[]) => isNotEmpty(entries)),
      distinctUntilChanged()
    );
  }

  /**
   * Delete the selected resource's policies
   */
  deleteSelectedResourcePolicies(): void {
    this.processingDelete$.next(true);
    const policiesToDelete: ResourcePolicyCheckboxEntry[] = this.resourcePoliciesEntries$.value
      .filter((entry: ResourcePolicyCheckboxEntry) => entry.checked);
    this.subs.push(
      observableFrom(policiesToDelete).pipe(
        concatMap((entry: ResourcePolicyCheckboxEntry) => this.resourcePolicyService.delete(entry.policy.id)),
        scan((acc: any, value: any) => [...acc, value], []),
        filter((results: boolean[]) => results.length === policiesToDelete.length),
        take(1),
      ).subscribe((results: boolean[]) => {
        const failureResults = results.filter((result: boolean) => !result);
        if (isEmpty(failureResults)) {
          this.notificationsService.success(null, this.translate.get('resource-policies.delete.success.content'));
        } else {
          this.notificationsService.error(null, this.translate.get('resource-policies.delete.failure.content'));
        }
        this.processingDelete$.next(false);
      })
    );
  }

  /**
   * Return all resource's policies
   *
   * @return an observable that emits all resource's policies
   */
  getResourcePolicies(): Observable<ResourcePolicyCheckboxEntry[]> {
    return this.resourcePoliciesEntries$.asObservable();
  }

  /**
   * Initialize the resource's policies list
   */
  initResourcePolicyList() {
    this.subs.push(this.resourcePolicyService.searchByResource(
      this.resourceUUID, null, false, true,
      followLink('eperson'), followLink('group')
    ).pipe(
      filter(() => this.isActive),
      getAllSucceededRemoteData()
    ).subscribe((result) => {
      const entries = result.payload.page.map((policy: ResourcePolicy) => ({
        id: policy.id,
        policy: policy,
        checked: false
      }));
      this.resourcePoliciesEntries$.next(entries);
      // TODO detectChanges still needed?
      this.cdr.detectChanges();
    }));
  }

  /**
   * Return a boolean representing if a delete operation is pending
   *
   * @return {Observable<boolean>}
   */
  isProcessingDelete(): Observable<boolean> {
    return this.processingDelete$.asObservable();
  }

  /**
   * Redirect to resource policy creation page
   */
  redirectToResourcePolicyCreatePage(): void {
    this.router.navigate([`./create`], {
      relativeTo: this.route,
      queryParams: {
        policyTargetId: this.resourceUUID,
        targetType: this.resourceType
      }
    });
  }

  /**
   * Select/unselect all checkbox in the list
   */
  selectAllCheckbox(event: any): void {
    const checked = event.target.checked;
    this.resourcePoliciesEntries$.value.forEach((entry: ResourcePolicyCheckboxEntry) => entry.checked = checked);
  }

  /**
   * Select/unselect checkbox
   */
  selectCheckbox(policyEntry: ResourcePolicyCheckboxEntry, checked: boolean) {
    policyEntry.checked = checked;
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.isActive = false;
    this.resourcePoliciesEntries$ = null;
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

}
