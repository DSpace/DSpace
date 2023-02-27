import { Component, EventEmitter, HostListener, OnDestroy, OnInit, Output, ChangeDetectorRef } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import {
  DynamicFormControlModel,
  DynamicFormLayout,
  DynamicInputModel,
  DynamicTextAreaModel
} from '@ng-dynamic-forms/core';
import { TranslateService } from '@ngx-translate/core';
import {
  ObservedValueOf,
  combineLatest as observableCombineLatest,
  Observable,
  of as observableOf,
  Subscription,
} from 'rxjs';
import { catchError, map, switchMap, take, filter, debounceTime } from 'rxjs/operators';
import { getCollectionEditRolesRoute } from '../../../collection-page/collection-page-routing-paths';
import { getCommunityEditRolesRoute } from '../../../community-page/community-page-routing-paths';
import { DSpaceObjectDataService } from '../../../core/data/dspace-object-data.service';
import { AuthorizationDataService } from '../../../core/data/feature-authorization/authorization-data.service';
import { FeatureID } from '../../../core/data/feature-authorization/feature-id';
import { PaginatedList } from '../../../core/data/paginated-list.model';
import { RemoteData } from '../../../core/data/remote-data';
import { RequestService } from '../../../core/data/request.service';
import { EPersonDataService } from '../../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { Group } from '../../../core/eperson/models/group.model';
import { Collection } from '../../../core/shared/collection.model';
import { Community } from '../../../core/shared/community.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import {
  getRemoteDataPayload,
  getFirstSucceededRemoteData,
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteDataPayload
} from '../../../core/shared/operators';
import { AlertType } from '../../../shared/alert/aletr-type';
import { ConfirmationModalComponent } from '../../../shared/confirmation-modal/confirmation-modal.component';
import { hasValue, isNotEmpty, hasValueOperator } from '../../../shared/empty.util';
import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { NoContent } from '../../../core/shared/NoContent.model';
import { Operation } from 'fast-json-patch';
import { ValidateGroupExists } from './validators/group-exists.validator';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'ds-group-form',
  templateUrl: './group-form.component.html'
})
/**
 * A form used for creating and editing groups
 */
export class GroupFormComponent implements OnInit, OnDestroy {

  messagePrefix = 'admin.access-control.groups.form';

  /**
   * A unique id used for ds-form
   */
  formId = 'group-form';

  /**
   * Dynamic models for the inputs of form
   */
  groupName: DynamicInputModel;
  groupCommunity: DynamicInputModel;
  groupDescription: DynamicTextAreaModel;

  /**
   * A list of all dynamic input models
   */
  formModel: DynamicFormControlModel[];

  /**
   * Layout used for structuring the form inputs
   */
  formLayout: DynamicFormLayout = {
    groupName: {
      grid: {
        host: 'row'
      }
    },
    groupDescription: {
      grid: {
        host: 'row'
      }
    },
  };

  /**
   * A FormGroup that combines all inputs
   */
  formGroup: FormGroup;

  /**
   * An EventEmitter that's fired whenever the form is being submitted
   */
  @Output() submitForm: EventEmitter<any> = new EventEmitter();

  /**
   * An EventEmitter that's fired whenever the form is cancelled
   */
  @Output() cancelForm: EventEmitter<any> = new EventEmitter();

  /**
   * List of subscriptions
   */
  subs: Subscription[] = [];

  /**
   * Group currently being edited
   */
  groupBeingEdited: Group;

  /**
   * Observable whether or not the logged in user is allowed to delete the Group & doesn't have a linked object (community / collection linked to workspace group
   */
  canEdit$: Observable<boolean>;

  /**
   * The AlertType enumeration
   * @type {AlertType}
   */
  public AlertTypeEnum = AlertType;

  /**
   * Subscription to email field value change
   */
  groupNameValueChangeSubscribe: Subscription;


  constructor(public groupDataService: GroupDataService,
    private ePersonDataService: EPersonDataService,
    private dSpaceObjectDataService: DSpaceObjectDataService,
    private formBuilderService: FormBuilderService,
    private translateService: TranslateService,
    private notificationsService: NotificationsService,
    private route: ActivatedRoute,
    protected router: Router,
    private authorizationService: AuthorizationDataService,
    private modalService: NgbModal,
    public requestService: RequestService,
    protected changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit() {
    this.initialisePage();
  }

  initialisePage() {
    this.subs.push(this.route.params.subscribe((params) => {
      if (params.groupId !== 'newGroup') {
        this.setActiveGroup(params.groupId);
      }
    }));
    this.canEdit$ = this.groupDataService.getActiveGroup().pipe(
      hasValueOperator(),
      switchMap((group: Group) => {
        return observableCombineLatest(
          this.authorizationService.isAuthorized(FeatureID.CanDelete, isNotEmpty(group) ? group.self : undefined),
          this.hasLinkedDSO(group),
          (isAuthorized: ObservedValueOf<Observable<boolean>>, hasLinkedDSO: ObservedValueOf<Observable<boolean>>) => {
            return isAuthorized && !hasLinkedDSO;
          });
      })
    );
    observableCombineLatest(
      this.translateService.get(`${this.messagePrefix}.groupName`),
      this.translateService.get(`${this.messagePrefix}.groupCommunity`),
      this.translateService.get(`${this.messagePrefix}.groupDescription`)
    ).subscribe(([groupName, groupCommunity, groupDescription]) => {
      this.groupName = new DynamicInputModel({
        id: 'groupName',
        label: groupName,
        name: 'groupName',
        validators: {
          required: null,
        },
        required: true,
      });
      this.groupCommunity = new DynamicInputModel({
        id: 'groupCommunity',
        label: groupCommunity,
        name: 'groupCommunity',
        required: false,
        readOnly: true,
      });
      this.groupDescription = new DynamicTextAreaModel({
        id: 'groupDescription',
        label: groupDescription,
        name: 'groupDescription',
        required: false,
        spellCheck: environment.form.spellCheck,
      });
      this.formModel = [
        this.groupName,
        this.groupDescription,
      ];
      this.formGroup = this.formBuilderService.createFormGroup(this.formModel);

      if (!!this.formGroup.controls.groupName) {
        this.formGroup.controls.groupName.setAsyncValidators(ValidateGroupExists.createValidator(this.groupDataService));
        this.groupNameValueChangeSubscribe = this.groupName.valueChanges.pipe(debounceTime(300)).subscribe(() => {
          this.changeDetectorRef.detectChanges();
        });
      }

      this.subs.push(
        observableCombineLatest(
          this.groupDataService.getActiveGroup(),
          this.canEdit$,
          this.groupDataService.getActiveGroup()
            .pipe(filter((activeGroup) => hasValue(activeGroup)),switchMap((activeGroup) => this.getLinkedDSO(activeGroup).pipe(getFirstSucceededRemoteDataPayload())))
        ).subscribe(([activeGroup, canEdit, linkedObject]) => {

          if (activeGroup != null) {

            // Disable group name exists validator
            this.formGroup.controls.groupName.clearAsyncValidators();

            this.groupBeingEdited = activeGroup;

            if (linkedObject?.name) {
              this.formBuilderService.insertFormGroupControl(1, this.formGroup, this.formModel, this.groupCommunity);
              this.formGroup.patchValue({
                groupName: activeGroup.name,
                groupCommunity: linkedObject?.name ?? '',
                groupDescription: activeGroup.firstMetadataValue('dc.description'),
              });
            } else {
              this.formModel = [
                this.groupName,
                this.groupDescription,
              ];
              this.formGroup.patchValue({
                groupName: activeGroup.name,
                groupDescription: activeGroup.firstMetadataValue('dc.description'),
              });
            }
            setTimeout(() => {
              if (!canEdit || activeGroup.permanent) {
                this.formGroup.disable();
              }
            }, 200);
          }
        })
      );
    });
  }

  /**
   * Stop editing the currently selected group
   */
  onCancel() {
    this.groupDataService.cancelEditGroup();
    this.cancelForm.emit();
    this.router.navigate([this.groupDataService.getGroupRegistryRouterLink()]);
  }

  /**
   * Submit the form
   * When the eperson has an id attached -> Edit the eperson
   * When the eperson has no id attached -> Create new eperson
   * Emit the updated/created eperson using the EventEmitter submitForm
   */
  onSubmit() {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe(
      (group: Group) => {
        const values = {
          name: this.groupName.value,
          metadata: {
            'dc.description': [
              {
                value: this.groupDescription.value
              }
            ]
          },
        };
        if (group === null) {
          this.createNewGroup(values);
        } else {
          this.editGroup(group);
        }
      }
    );
  }

  /**
   * Creates new Group based on given values from form
   * @param values
   */
  createNewGroup(values) {
    const groupToCreate = Object.assign(new Group(), values);
    this.groupDataService.create(groupToCreate).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((rd: RemoteData<Group>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get(this.messagePrefix + '.notification.created.success', { name: groupToCreate.name }));
        this.submitForm.emit(groupToCreate);
        if (isNotEmpty(rd.payload)) {
          const groupSelfLink = rd.payload._links.self.href;
          this.setActiveGroupWithLink(groupSelfLink);
          this.groupDataService.clearGroupsRequests();
          this.router.navigateByUrl(this.groupDataService.getGroupEditPageRouterLinkWithID(rd.payload.uuid));
        }
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.created.failure', { name: groupToCreate.name }));
        this.showNotificationIfNameInUse(groupToCreate, 'created');
        this.cancelForm.emit();
      }
    });
  }

  /**
   * Checks for the given group if there is already a group in the system with that group name and shows error if that
   * is the case
   * @param group                 group to check
   * @param notificationSection   whether in create or edit
   */
  private showNotificationIfNameInUse(group: Group, notificationSection: string) {
    // Relevant message for group name in use
    this.subs.push(this.groupDataService.searchGroups(group.name, {
      currentPage: 1,
      elementsPerPage: 0
    }).pipe(getFirstSucceededRemoteData(), getRemoteDataPayload())
      .subscribe((list: PaginatedList<Group>) => {
        if (list.totalElements > 0) {
          this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.' + notificationSection + '.failure.groupNameInUse', {
            name: group.name
          }));
        }
      }));
  }

  /**
   * Edit existing Group based on given values from form and old Group
   * @param group   Group to edit and old values contained within
   */
  editGroup(group: Group) {
    let operations: Operation[] = [];

    if (hasValue(this.groupDescription.value)) {
      operations = [...operations, {
        op: 'add',
        path: '/metadata/dc.description',
        value: this.groupDescription.value
      }];
    }

    if (hasValue(this.groupName.value)) {
      operations = [...operations, {
        op: 'replace',
        path: '/name',
        value: this.groupName.value
      }];
    }

    this.groupDataService.patch(group, operations).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((rd: RemoteData<Group>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get(this.messagePrefix + '.notification.edited.success', { name: rd.payload.name }));
        this.submitForm.emit(rd.payload);
      } else {
        this.notificationsService.error(this.translateService.get(this.messagePrefix + '.notification.edited.failure', { name: group.name }));
        this.cancelForm.emit();
      }
    });
  }

  /**
   * Start editing the selected group
   * @param groupId   ID of group to set as active
   */
  setActiveGroup(groupId: string) {
    this.groupDataService.cancelEditGroup();
    this.groupDataService.findById(groupId)
      .pipe(
        getFirstSucceededRemoteData(),
        getRemoteDataPayload())
      .subscribe((group: Group) => {
        this.groupDataService.editGroup(group);
      });
  }

  /**
   * Start editing the selected group
   * @param groupSelfLink   SelfLink of group to set as active
   */
  setActiveGroupWithLink(groupSelfLink: string) {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((activeGroup: Group) => {
      if (activeGroup === null) {
        this.groupDataService.cancelEditGroup();
        this.groupDataService.findByHref(groupSelfLink, false, false, followLink('subgroups'), followLink('epersons'), followLink('object'))
          .pipe(
            getFirstSucceededRemoteData(),
            getRemoteDataPayload())
          .subscribe((group: Group) => {
            this.groupDataService.editGroup(group);
          });
      }
    });
  }

  /**
   * Deletes the Group from the Repository. The Group will be the only that this form is showing.
   * It'll either show a success or error message depending on whether the delete was successful or not.
   */
  delete() {
    this.groupDataService.getActiveGroup().pipe(take(1)).subscribe((group: Group) => {
      const modalRef = this.modalService.open(ConfirmationModalComponent);
      modalRef.componentInstance.dso = group;
      modalRef.componentInstance.headerLabel = this.messagePrefix + '.delete-group.modal.header';
      modalRef.componentInstance.infoLabel = this.messagePrefix + '.delete-group.modal.info';
      modalRef.componentInstance.cancelLabel = this.messagePrefix + '.delete-group.modal.cancel';
      modalRef.componentInstance.confirmLabel = this.messagePrefix + '.delete-group.modal.confirm';
      modalRef.componentInstance.brandColor = 'danger';
      modalRef.componentInstance.confirmIcon = 'fas fa-trash';
      modalRef.componentInstance.response.pipe(take(1)).subscribe((confirm: boolean) => {
        if (confirm) {
          if (hasValue(group.id)) {
            this.groupDataService.delete(group.id).pipe(getFirstCompletedRemoteData())
              .subscribe((rd: RemoteData<NoContent>) => {
                if (rd.hasSucceeded) {
                  this.notificationsService.success(this.translateService.get(this.messagePrefix + '.notification.deleted.success', { name: group.name }));
                  this.onCancel();
                } else {
                  this.notificationsService.error(
                    this.translateService.get(this.messagePrefix + '.notification.deleted.failure.title', { name: group.name }),
                    this.translateService.get(this.messagePrefix + '.notification.deleted.failure.content', { cause: rd.errorMessage }));
                }
              });
          }
        }
      });
    });
  }

  /**
   * Cancel the current edit when component is destroyed & unsub all subscriptions
   */
  @HostListener('window:beforeunload')
  ngOnDestroy(): void {
    this.groupDataService.cancelEditGroup();
    this.subs.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());

    if ( hasValue(this.groupNameValueChangeSubscribe) ) {
      this.groupNameValueChangeSubscribe.unsubscribe();
    }

  }

  /**
   * Check if group has a linked object (community or collection linked to a workflow group)
   * @param group
   */
  hasLinkedDSO(group: Group): Observable<boolean> {
    if (hasValue(group) && hasValue(group._links.object.href)) {
      return this.getLinkedDSO(group).pipe(
        map((rd: RemoteData<DSpaceObject>) => {
          return hasValue(rd) && hasValue(rd.payload);
        }),
        catchError(() => observableOf(false)),
      );
    }
  }

  /**
   * Get group's linked object if it has one (community or collection linked to a workflow group)
   * @param group
   */
  getLinkedDSO(group: Group): Observable<RemoteData<DSpaceObject>> {
    if (hasValue(group) && hasValue(group._links.object.href)) {
      if (group.object === undefined) {
        return this.dSpaceObjectDataService.findByHref(group._links.object.href);
      }
      return group.object;
    }
  }

  /**
   * Get the route to the edit roles tab of the group's linked object (community or collection linked to a workflow group) if it has one
   * @param group
   */
  getLinkedEditRolesRoute(group: Group): Observable<string> {
    if (hasValue(group) && hasValue(group._links.object.href)) {
      return this.getLinkedDSO(group).pipe(
        map((rd: RemoteData<DSpaceObject>) => {
          if (hasValue(rd) && hasValue(rd.payload)) {
            const dso = rd.payload;
            switch ((dso as any).type) {
              case Community.type.value:
                return getCommunityEditRolesRoute(rd.payload.id);
              case Collection.type.value:
                return getCollectionEditRolesRoute(rd.payload.id);
            }
          }
        })
      );
    }
  }
}
