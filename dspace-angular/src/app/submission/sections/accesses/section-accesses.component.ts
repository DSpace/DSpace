import { SectionAccessesService } from './section-accesses.service';
import { Component, Inject, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';

import { filter, map, mergeMap, take } from 'rxjs/operators';
import { combineLatest, Observable, of, Subscription } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';

import { renderSectionFor } from '../sections-decorator';
import { SectionsType } from '../sections-type';
import { SectionDataObject } from '../models/section-data.model';
import { SectionsService } from '../sections.service';
import { SectionModelComponent } from '../models/section.model';
import {
  DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX,
  DYNAMIC_FORM_CONTROL_TYPE_DATEPICKER,
  DynamicCheckboxModel,
  DynamicDatePickerModel,
  DynamicFormArrayModel,
  DynamicFormControlEvent,
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicSelectModel,
  MATCH_ENABLED,
  OR_OPERATOR
} from '@ng-dynamic-forms/core';

import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import {
  ACCESS_CONDITION_GROUP_CONFIG,
  ACCESS_CONDITION_GROUP_LAYOUT,
  ACCESS_CONDITIONS_FORM_ARRAY_CONFIG,
  ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT,
  ACCESS_FORM_CHECKBOX_CONFIG,
  ACCESS_FORM_CHECKBOX_LAYOUT,
  FORM_ACCESS_CONDITION_END_DATE_CONFIG,
  FORM_ACCESS_CONDITION_END_DATE_LAYOUT,
  FORM_ACCESS_CONDITION_START_DATE_CONFIG,
  FORM_ACCESS_CONDITION_START_DATE_LAYOUT,
  FORM_ACCESS_CONDITION_TYPE_CONFIG,
  FORM_ACCESS_CONDITION_TYPE_LAYOUT
} from './section-accesses.model';
import { hasValue, isNotEmpty, isNotNull } from '../../../shared/empty.util';
import {
  WorkspaceitemSectionAccessesObject
} from '../../../core/submission/models/workspaceitem-section-accesses.model';
import { SubmissionAccessesConfigDataService } from '../../../core/config/submission-accesses-config-data.service';
import { getFirstSucceededRemoteData } from '../../../core/shared/operators';
import { FormComponent } from '../../../shared/form/form.component';
import { FormService } from '../../../shared/form/form.service';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { SectionFormOperationsService } from '../form/section-form-operations.service';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { AccessesConditionOption } from '../../../core/config/models/config-accesses-conditions-options.model';
import {
  SubmissionJsonPatchOperationsService
} from '../../../core/submission/submission-json-patch-operations.service';
import { dateToISOFormat } from '../../../shared/date.util';
import { DynamicFormControlCondition } from '@ng-dynamic-forms/core/lib/model/misc/dynamic-form-control-relation.model';
import { DynamicDateControlValue } from '@ng-dynamic-forms/core/lib/model/dynamic-date-control.model';

/**
 * This component represents a section for managing item's access conditions.
 */
@Component({
  selector: 'ds-section-accesses',
  templateUrl: './section-accesses.component.html',
  styleUrls: ['./section-accesses.component.scss']
})
@renderSectionFor(SectionsType.AccessesCondition)
export class SubmissionSectionAccessesComponent extends SectionModelComponent {

  /**
   * The FormComponent reference
   */
  @ViewChild('formRef') public formRef: FormComponent;

  /**
   * List of available access conditions that could be set to item
   */
  public availableAccessConditionOptions: AccessesConditionOption[];  // List of accessConditions that an user can select

  /**
   * The form id
   * @type {string}
   */
  public formId: string;

  /**
   * The accesses section data
   * @type {WorkspaceitemSectionAccessesObject}
   */
  public accessesData: WorkspaceitemSectionAccessesObject;

  /**
   * The form model
   * @type {DynamicFormControlModel[]}
   */
  public formModel: DynamicFormControlModel[];

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  protected subs: Subscription[] = [];

  /**
   * The [[JsonPatchOperationPathCombiner]] object
   * @type {JsonPatchOperationPathCombiner}
   */
  protected pathCombiner: JsonPatchOperationPathCombiner;

  /**
   * Defines if the access discoverable property can be managed
   */
  public canChangeDiscoverable: boolean;

  /**
   * Initialize instance variables
   *
   * @param {SectionsService} sectionService
   * @param {SectionDataObject} injectedSectionData
   * @param {FormService} formService
   * @param {JsonPatchOperationsBuilder} operationsBuilder
   * @param {SectionFormOperationsService} formOperationsService
   * @param {FormBuilderService} formBuilderService
   * @param {TranslateService} translate
   * @param {SubmissionAccessesConfigDataService} accessesConfigService
   * @param {SectionAccessesService} accessesService
   * @param {SubmissionJsonPatchOperationsService} operationsService
   * @param {string} injectedSubmissionId
   */
  constructor(
    protected sectionService: SectionsService,
    private formBuilderService: FormBuilderService,
    private accessesConfigService: SubmissionAccessesConfigDataService,
    private accessesService: SectionAccessesService,
    protected formOperationsService: SectionFormOperationsService,
    protected operationsBuilder: JsonPatchOperationsBuilder,
    private formService: FormService,
    private translate: TranslateService,
    private operationsService: SubmissionJsonPatchOperationsService,
    @Inject('sectionDataProvider') public injectedSectionData: SectionDataObject,
    @Inject('submissionIdProvider') public injectedSubmissionId: string) {
    super(undefined, injectedSectionData, injectedSubmissionId);
  }

  /**
   * Initialize form model values
   *
   * @param formModel
   *    The form model
   */
  public initModelData(formModel: DynamicFormControlModel[]) {
    this.accessesData.accessConditions.forEach((accessCondition, index) => {
      Array.of('name', 'startDate', 'endDate')
        .filter((key) => accessCondition.hasOwnProperty(key) && isNotEmpty(accessCondition[key]))
        .forEach((key) => {
          const metadataModel: any = this.formBuilderService.findById(key, formModel, index);
          if (metadataModel) {
            if (metadataModel.type === DYNAMIC_FORM_CONTROL_TYPE_DATEPICKER) {
              const date = new Date(accessCondition[key]);
              metadataModel.value = {
                year: date.getUTCFullYear(),
                month: date.getUTCMonth() + 1,
                day: date.getUTCDate()
              };
            } else {
              metadataModel.value = accessCondition[key];
            }
          }
        });
    });
  }

  /**
   * Method called when a form dfChange event is fired.
   * Dispatch form operations based on changes.
   */
  onChange(event: DynamicFormControlEvent) {
    if (event.model.type === DYNAMIC_FORM_CONTROL_TYPE_CHECKBOX) {
      const path = this.formOperationsService.getFieldPathSegmentedFromChangeEvent(event);
      const value = this.formOperationsService.getFieldValueFromChangeEvent(event);
      this.operationsBuilder.replace(this.pathCombiner.getPath(path), value.value, true);
    } else {
      if (event.model.id === FORM_ACCESS_CONDITION_TYPE_CONFIG.id) {
        // Clear previous state when switching through different access conditions

        const startDateControl: FormControl = event.control.parent.get('startDate') as FormControl;
        const endDateControl: FormControl = event.control.parent.get('endDate') as FormControl;

        startDateControl?.markAsUntouched();
        endDateControl?.markAsUntouched();

        startDateControl?.setValue(null);
        endDateControl?.setValue(null);
        event.control.parent.markAsDirty();
      }

      // validate form
      this.formService.validateAllFormFields(this.formRef.formGroup);
      this.formService.isValid(this.formId).pipe(
        take(1),
        filter((isValid) => isValid),
        mergeMap(() => this.formService.getFormData(this.formId)),
        take(1)
      ).subscribe((formData: any) => {
        const accessConditionsToSave = [];
        formData.accessCondition
          .map((accessConditions) => accessConditions.accessConditionGroup)
          .filter((accessCondition) => isNotEmpty(accessCondition))
          .forEach((accessCondition) => {
            let accessConditionOpt;

            this.availableAccessConditionOptions
              .filter((element) => isNotNull(accessCondition.name) && element.name === accessCondition.name[0].value)
              .forEach((element) => accessConditionOpt = element);

            if (accessConditionOpt) {
              const currentAccessCondition = Object.assign({}, accessCondition);
              currentAccessCondition.name = this.retrieveValueFromField(accessCondition.name);

              /* When start and end date fields are deactivated, their values may be still present in formData,
              therefore it is necessary to delete them if they're not allowed by the current access condition option. */
              if (!accessConditionOpt.hasStartDate) {
                delete currentAccessCondition.startDate;
              } else if (accessCondition.startDate) {
                const startDate = this.retrieveValueFromField(accessCondition.startDate);
                currentAccessCondition.startDate = dateToISOFormat(startDate);
              }
              if (!accessConditionOpt.hasEndDate) {
                delete currentAccessCondition.endDate;
              } else if (accessCondition.endDate) {
                const endDate = this.retrieveValueFromField(accessCondition.endDate);
                currentAccessCondition.endDate = dateToISOFormat(endDate);
              }
              accessConditionsToSave.push(currentAccessCondition);
            }
          });

        this.operationsBuilder.add(this.pathCombiner.getPath('accessConditions'), accessConditionsToSave, true);
      });
    }
  }

  /**
   * Method called when a form removeArrayItem event is fired.
   * Dispatch remove form operations based on changes.
   */
  onRemove(event: DynamicFormControlEvent) {
    const fieldIndex = this.formOperationsService.getArrayIndexFromEvent(event);
    const fieldPath = 'accessConditions/' + fieldIndex;

    this.operationsBuilder.remove(this.pathCombiner.getPath(fieldPath));
  }

  /**
   * Unsubscribe from all subscriptions
   */
  onSectionDestroy() {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

  /**
   * Initialize all instance variables and retrieve collection default access conditions
   */
  protected onSectionInit(): void {

    this.pathCombiner = new JsonPatchOperationPathCombiner('sections', this.sectionData.id);
    this.formId = this.formService.getUniqueId(this.sectionData.id);
    const config$ = this.accessesConfigService.findByHref(this.sectionData.config, true, false).pipe(
      getFirstSucceededRemoteData(),
      map((config) => config.payload),
    );

    const accessData$ = this.accessesService.getAccessesData(this.submissionId, this.sectionData.id);

    combineLatest([config$, accessData$]).subscribe(([config, accessData]) => {
      this.availableAccessConditionOptions = isNotEmpty(config.accessConditionOptions) ? config.accessConditionOptions : [];
      this.canChangeDiscoverable = !!config.canChangeDiscoverable;
      this.accessesData = accessData;
      this.formModel = this.buildFileEditForm();
    });


  }

  /**
   * Get section status
   *
   * @return Observable<boolean>
   *     the section status
   */
  protected getSectionStatus(): Observable<boolean> {
    return of(true);
  }

  /**
   * Initialize form model
   */
  protected buildFileEditForm() {

    const formModel: DynamicFormControlModel[] = [];
    if (this.canChangeDiscoverable) {
      const discoverableCheckboxConfig = Object.assign({}, ACCESS_FORM_CHECKBOX_CONFIG, {
        label: this.translate.instant('submission.sections.accesses.form.discoverable-label'),
        hint: this.translate.instant('submission.sections.accesses.form.discoverable-description'),
        value: this.accessesData.discoverable
      });
      formModel.push(
        new DynamicCheckboxModel(discoverableCheckboxConfig, ACCESS_FORM_CHECKBOX_LAYOUT)
      );
    }

    const accessConditionTypeModelConfig = Object.assign({}, FORM_ACCESS_CONDITION_TYPE_CONFIG);
    const accessConditionsArrayConfig = Object.assign({}, ACCESS_CONDITIONS_FORM_ARRAY_CONFIG);
    const accessConditionTypeOptions = [];

    for (const accessCondition of this.availableAccessConditionOptions) {
      accessConditionTypeOptions.push(
        {
          label: accessCondition.name,
          value: accessCondition.name
        }
      );
    }
    accessConditionTypeModelConfig.options = accessConditionTypeOptions;

    // Dynamically assign of relation in config. For startDate and endDate.
    const startDateCondition: DynamicFormControlCondition[] = [];
    const endDateCondition: DynamicFormControlCondition[] = [];
    let maxStartDate: DynamicDateControlValue;
    let maxEndDate: DynamicDateControlValue;
    this.availableAccessConditionOptions.forEach((condition) => {

      if (condition.hasStartDate) {
        startDateCondition.push({ id: 'name', value: condition.name });
        if (condition.maxStartDate) {
          const min = new Date(condition.maxStartDate);
          maxStartDate = {
            year: min.getUTCFullYear(),
            month: min.getUTCMonth() + 1,
            day: min.getUTCDate()
          };
        }
      }
      if (condition.hasEndDate) {
        endDateCondition.push({ id: 'name', value: condition.name });
        if (condition.maxEndDate) {
          const max = new Date(condition.maxEndDate);
          maxEndDate = {
            year: max.getUTCFullYear(),
            month: max.getUTCMonth() + 1,
            day: max.getUTCDate()
          };
        }
      }
    });
    const confStart = { relations: [{ match: MATCH_ENABLED, operator: OR_OPERATOR, when: startDateCondition }] };
    const confEnd = { relations: [{ match: MATCH_ENABLED, operator: OR_OPERATOR, when: endDateCondition }] };
    const hasStartDate = startDateCondition.length > 0;
    const hasEndDate = endDateCondition.length > 0;

    accessConditionsArrayConfig.groupFactory = () => {
      const type = new DynamicSelectModel(accessConditionTypeModelConfig, FORM_ACCESS_CONDITION_TYPE_LAYOUT);
      const startDateConfig = Object.assign({}, FORM_ACCESS_CONDITION_START_DATE_CONFIG, confStart);
      if (maxStartDate) {
        startDateConfig.max = maxStartDate;
      }

      const endDateConfig = Object.assign({}, FORM_ACCESS_CONDITION_END_DATE_CONFIG, confEnd);
      if (maxEndDate) {
        endDateConfig.max = maxEndDate;
      }

      const startDate = new DynamicDatePickerModel(startDateConfig, FORM_ACCESS_CONDITION_START_DATE_LAYOUT);
      const endDate = new DynamicDatePickerModel(endDateConfig, FORM_ACCESS_CONDITION_END_DATE_LAYOUT);
      const accessConditionGroupConfig = Object.assign({}, ACCESS_CONDITION_GROUP_CONFIG);
      accessConditionGroupConfig.group = [type];
      if (hasStartDate) {
        accessConditionGroupConfig.group.push(startDate);
      }
      if (hasEndDate) {
        accessConditionGroupConfig.group.push(endDate);
      }
      return [new DynamicFormGroupModel(accessConditionGroupConfig, ACCESS_CONDITION_GROUP_LAYOUT)];
    };

    // Number of access conditions blocks in form
    accessConditionsArrayConfig.initialCount = isNotEmpty(this.accessesData.accessConditions) ? this.accessesData.accessConditions.length : 1;
    formModel.push(
      new DynamicFormArrayModel(accessConditionsArrayConfig, ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT)
    );

    this.initModelData(formModel);
    return formModel;
  }

  protected retrieveValueFromField(field: any) {
    const temp = Array.isArray(field) ? field[0] : field;
    return (temp) ? temp.value : undefined;
  }

}
