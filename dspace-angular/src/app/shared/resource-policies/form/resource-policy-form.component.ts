import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';

import {
  Observable,
  of as observableOf,
  combineLatest as observableCombineLatest,
  Subscription,
  BehaviorSubject
} from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import {
  DynamicDatePickerModel,
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicSelectModel
} from '@ng-dynamic-forms/core';

import { ResourcePolicy } from '../../../core/resource-policy/models/resource-policy.model';
import { DsDynamicInputModel } from '../../form/builder/ds-dynamic-form-ui/models/ds-dynamic-input.model';
import {
  RESOURCE_POLICY_FORM_ACTION_TYPE_CONFIG,
  RESOURCE_POLICY_FORM_DATE_GROUP_CONFIG,
  RESOURCE_POLICY_FORM_DATE_GROUP_LAYOUT,
  RESOURCE_POLICY_FORM_DESCRIPTION_CONFIG,
  RESOURCE_POLICY_FORM_END_DATE_CONFIG,
  RESOURCE_POLICY_FORM_END_DATE_LAYOUT,
  RESOURCE_POLICY_FORM_NAME_CONFIG,
  RESOURCE_POLICY_FORM_POLICY_TYPE_CONFIG,
  RESOURCE_POLICY_FORM_START_DATE_CONFIG,
  RESOURCE_POLICY_FORM_START_DATE_LAYOUT
} from './resource-policy-form.model';
import { DsDynamicTextAreaModel } from '../../form/builder/ds-dynamic-form-ui/models/ds-dynamic-textarea.model';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { DSONameService } from '../../../core/breadcrumbs/dso-name.service';
import { hasValue, isEmpty, isNotEmpty, hasValueOperator } from '../../empty.util';
import { FormService } from '../../form/form.service';
import { RESOURCE_POLICY } from '../../../core/resource-policy/models/resource-policy.resource-type';
import { RemoteData } from '../../../core/data/remote-data';
import { dateToISOFormat, stringToNgbDateStruct } from '../../date.util';
import { EPersonDataService } from '../../../core/eperson/eperson-data.service';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { getFirstSucceededRemoteData } from '../../../core/shared/operators';
import { RequestService } from '../../../core/data/request.service';
import { NgbModal, NgbNavChangeEvent } from '@ng-bootstrap/ng-bootstrap';

export interface ResourcePolicyEvent {
  object: ResourcePolicy;
  target: {
    type: string,
    uuid: string
  };
  updateTarget: boolean;
}

@Component({
  selector: 'ds-resource-policy-form',
  templateUrl: './resource-policy-form.component.html',
})
/**
 * Component that show form for adding/editing a resource policy
 */
export class ResourcePolicyFormComponent implements OnInit, OnDestroy {

  /**
   * If given contains the resource policy to edit
   * @type {ResourcePolicy}
   */
  @Input() resourcePolicy: ResourcePolicy;

  /**
   * A boolean representing if form submit operation is processing
   * @type {boolean}
   */
  @Input() isProcessing: Observable<boolean> = observableOf(false);

  /**
   * An event fired when form is canceled.
   * Event's payload is empty.
   */
  @Output() reset: EventEmitter<any> = new EventEmitter<any>();

  /**
   * An event fired when form is submitted.
   * Event's payload equals to a new ResourcePolicy.
   */
  @Output() submit: EventEmitter<ResourcePolicyEvent> = new EventEmitter<ResourcePolicyEvent>();

  @ViewChild('content') content: ElementRef;

  /**
   * The form id
   * @type {string}
   */
  public formId: string;

  /**
   * The form model
   * @type {DynamicFormControlModel[]}
   */
  public formModel: DynamicFormControlModel[];

  /**
   * The eperson or group that will be granted the permission
   * @type {DSpaceObject}
   */
  public resourcePolicyGrant: DSpaceObject;

  /**
   * The type of the object that will be grant of the permission. It could be 'eperson' or 'group'
   * @type {string}
   */
  public resourcePolicyGrantType: string;

  /**
   * The name of the eperson or group that will be granted the permission
   * @type {BehaviorSubject<string>}
   */
  public resourcePolicyTargetName$: BehaviorSubject<string> = new BehaviorSubject('');

  /**
   * A boolean representing if component is active
   * @type {boolean}
   */
  private isActive: boolean;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  navActiveId: string;

  resourcePolicyTargetUpdated = false;

  /**
   * Initialize instance variables
   *
   * @param {DSONameService} dsoNameService
   * @param {EPersonDataService} ePersonService
   * @param {FormService} formService
   * @param {GroupDataService} groupService
   * @param {RequestService} requestService
   * @param modalService
   */
  constructor(
    private dsoNameService: DSONameService,
    private ePersonService: EPersonDataService,
    private formService: FormService,
    private groupService: GroupDataService,
    private requestService: RequestService,
    private modalService: NgbModal,
  ) {
  }

  /**
   * Initialize the component, setting up the form model
   */
  ngOnInit(): void {
    this.isActive = true;
    this.formId = this.formService.getUniqueId('resource-policy-form');
    this.formModel = this.buildResourcePolicyForm();

    if (this.isBeingEdited()) {
      const epersonRD$ = this.ePersonService.findByHref(this.resourcePolicy._links.eperson.href, false).pipe(
        getFirstSucceededRemoteData()
      );
      const groupRD$ = this.groupService.findByHref(this.resourcePolicy._links.group.href, false).pipe(
        getFirstSucceededRemoteData()
      );
      const dsoRD$: Observable<RemoteData<DSpaceObject>> = observableCombineLatest([epersonRD$, groupRD$]).pipe(
        map((rdArr: RemoteData<DSpaceObject>[]) => {
          return rdArr.find((rd: RemoteData<DSpaceObject>) => isNotEmpty(rd.payload));
        }),
        hasValueOperator(),
      );
      this.subs.push(
        dsoRD$.pipe(
          filter(() => this.isActive),
        ).subscribe((dsoRD: RemoteData<DSpaceObject>) => {
          this.resourcePolicyGrant = dsoRD.payload;
          this.navActiveId = String(dsoRD.payload.type);
          this.resourcePolicyTargetName$.next(this.getResourcePolicyTargetName());
        })
      );
    }
  }

  /**
   * Method to check if the form status is valid or not
   *
   * @return Observable that emits the form status
   */
  isFormValid(): Observable<boolean> {
    return this.formService.isValid(this.formId).pipe(
      map((isValid: boolean) => isValid && isNotEmpty(this.resourcePolicyGrant))
    );
  }

  /**
   * Initialize the form model
   *
   * @return the form models
   */
  private buildResourcePolicyForm(): DynamicFormControlModel[] {
    const formModel: DynamicFormControlModel[] = [];

    formModel.push(
      new DsDynamicInputModel(RESOURCE_POLICY_FORM_NAME_CONFIG),
      new DsDynamicTextAreaModel(RESOURCE_POLICY_FORM_DESCRIPTION_CONFIG),
      new DynamicSelectModel(RESOURCE_POLICY_FORM_POLICY_TYPE_CONFIG),
      new DynamicSelectModel(RESOURCE_POLICY_FORM_ACTION_TYPE_CONFIG)
    );

    const startDateModel = new DynamicDatePickerModel(
      RESOURCE_POLICY_FORM_START_DATE_CONFIG,
      RESOURCE_POLICY_FORM_START_DATE_LAYOUT
    );
    const endDateModel = new DynamicDatePickerModel(
      RESOURCE_POLICY_FORM_END_DATE_CONFIG,
      RESOURCE_POLICY_FORM_END_DATE_LAYOUT
    );
    const dateGroupConfig = Object.assign({}, RESOURCE_POLICY_FORM_DATE_GROUP_CONFIG, { group: [] });
    dateGroupConfig.group.push(startDateModel, endDateModel);
    formModel.push(new DynamicFormGroupModel(dateGroupConfig, RESOURCE_POLICY_FORM_DATE_GROUP_LAYOUT));

    this.initModelsValue(formModel);
    return formModel;
  }

  /**
   * Setting up the form models value
   *
   * @return the form models
   */
  initModelsValue(formModel: DynamicFormControlModel[]): DynamicFormControlModel[] {
    if (this.resourcePolicy) {
      formModel.forEach((model: any) => {
        if (model.id === 'date') {
          if (hasValue(this.resourcePolicy.startDate)) {
            model.get(0).value = stringToNgbDateStruct(this.resourcePolicy.startDate);
          }
          if (hasValue(this.resourcePolicy.endDate)) {
            model.get(1).value = stringToNgbDateStruct(this.resourcePolicy.endDate);
          }
        } else {
          if (this.resourcePolicy.hasOwnProperty(model.id) && this.resourcePolicy[model.id]) {
            model.value = this.resourcePolicy[model.id];
          }
        }
      });
    }

    return formModel;
  }

  /**
   * Return a boolean representing If is possible to set policy grant
   *
   * @return true if is possible, false otherwise
   */
  isBeingEdited(): boolean {
    return !isEmpty(this.resourcePolicy);
  }

  /**
   * Return the name of the eperson or group that will be granted the permission
   *
   * @return the object name
   */
  getResourcePolicyTargetName(): string {
    return isNotEmpty(this.resourcePolicyGrant) ? this.dsoNameService.getName(this.resourcePolicyGrant) : '';
  }

  /**
   * Update reference to the eperson or group that will be granted the permission
   */
  updateObjectSelected(object: DSpaceObject, isEPerson: boolean): void {
    this.resourcePolicyTargetUpdated = true;
    this.resourcePolicyGrant = object;
    this.resourcePolicyGrantType = isEPerson ? 'eperson' : 'group';
    this.resourcePolicyTargetName$.next(this.getResourcePolicyTargetName());
  }

  /**
   * Method called on reset
   * Emit a new reset Event
   */
  onReset(): void {
    this.reset.emit();
  }

  /**
   * Method called on submit.
   * Emit a new submit Event whether the form is valid
   */
  onSubmit(): void {
    this.formService.getFormData(this.formId).pipe(take(1))
      .subscribe((data) => {
        const eventPayload: ResourcePolicyEvent = Object.create({});
        eventPayload.object = this.createResourcePolicyByFormData(data);
        eventPayload.target = {
          type: this.resourcePolicyGrantType,
          uuid: this.resourcePolicyGrant.id
        };
        eventPayload.updateTarget = this.resourcePolicyTargetUpdated;
        this.submit.emit(eventPayload);
      });
  }

  /**
   * Create e new ResourcePolicy by form data
   *
   * @return the new ResourcePolicy object
   */
  createResourcePolicyByFormData(data): ResourcePolicy {
    const resourcePolicy = new ResourcePolicy();
    resourcePolicy.name = (data.name) ? data.name[0].value : null;
    resourcePolicy.description = (data.description) ? data.description[0].value : null;
    resourcePolicy.policyType = (data.policyType) ? data.policyType[0].value : null;
    resourcePolicy.action = (data.action) ? data.action[0].value : null;
    resourcePolicy.startDate = (data.date && data.date.start) ? dateToISOFormat(data.date.start[0].value) : null;
    resourcePolicy.endDate = (data.date && data.date.end) ? dateToISOFormat(data.date.end[0].value) : null;
    resourcePolicy.type = RESOURCE_POLICY;

    return resourcePolicy;
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.isActive = false;
    this.formModel = null;
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

  onNavChange(changeEvent: NgbNavChangeEvent) {
    // if a policy is being edited it should not be possible to switch between group and eperson
    if (this.isBeingEdited())  {
      changeEvent.preventDefault();
      this.modalService.open(this.content);
    }
  }
}
