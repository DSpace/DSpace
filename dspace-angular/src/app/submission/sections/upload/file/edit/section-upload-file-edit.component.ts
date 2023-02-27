import { ChangeDetectorRef, Component, OnInit, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';

import {
  DYNAMIC_FORM_CONTROL_TYPE_DATEPICKER,
  DynamicDatePickerModel,
  DynamicFormArrayModel,
  DynamicFormControlEvent,
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicSelectModel,
  MATCH_ENABLED,
  OR_OPERATOR
} from '@ng-dynamic-forms/core';

import {
  WorkspaceitemSectionUploadFileObject
} from '../../../../../core/submission/models/workspaceitem-section-upload-file.model';
import { FormBuilderService } from '../../../../../shared/form/builder/form-builder.service';
import {
  BITSTREAM_ACCESS_CONDITION_GROUP_CONFIG,
  BITSTREAM_ACCESS_CONDITION_GROUP_LAYOUT,
  BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_CONFIG,
  BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT,
  BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_CONFIG,
  BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_LAYOUT,
  BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_CONFIG,
  BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_LAYOUT,
  BITSTREAM_FORM_ACCESS_CONDITION_TYPE_CONFIG,
  BITSTREAM_FORM_ACCESS_CONDITION_TYPE_LAYOUT,
  BITSTREAM_METADATA_FORM_GROUP_CONFIG,
  BITSTREAM_METADATA_FORM_GROUP_LAYOUT
} from './section-upload-file-edit.model';
import { POLICY_DEFAULT_WITH_LIST } from '../../section-upload.component';
import { hasNoValue, hasValue, isNotEmpty, isNotNull } from '../../../../../shared/empty.util';
import { SubmissionFormsModel } from '../../../../../core/config/models/config-submission-forms.model';
import { FormFieldModel } from '../../../../../shared/form/builder/models/form-field.model';
import { AccessConditionOption } from '../../../../../core/config/models/config-access-condition-option.model';
import { SubmissionService } from '../../../../submission.service';
import { FormService } from '../../../../../shared/form/form.service';
import { FormComponent } from '../../../../../shared/form/form.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { filter, mergeMap, take } from 'rxjs/operators';
import { dateToISOFormat } from '../../../../../shared/date.util';
import { SubmissionObject } from '../../../../../core/submission/models/submission-object.model';
import {
  WorkspaceitemSectionUploadObject
} from '../../../../../core/submission/models/workspaceitem-section-upload.model';
import { JsonPatchOperationsBuilder } from '../../../../../core/json-patch/builder/json-patch-operations-builder';
import {
  SubmissionJsonPatchOperationsService
} from '../../../../../core/submission/submission-json-patch-operations.service';
import {
  JsonPatchOperationPathCombiner
} from '../../../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { SectionUploadService } from '../../section-upload.service';
import { Subscription } from 'rxjs';
import { DynamicFormControlCondition } from '@ng-dynamic-forms/core/lib/model/misc/dynamic-form-control-relation.model';
import { DynamicDateControlValue } from '@ng-dynamic-forms/core/lib/model/dynamic-date-control.model';

/**
 * This component represents the edit form for bitstream
 */
@Component({
  selector: 'ds-submission-section-upload-file-edit',
  styleUrls: ['./section-upload-file-edit.component.scss'],
  templateUrl: './section-upload-file-edit.component.html',
})
export class SubmissionSectionUploadFileEditComponent implements OnInit {

  /**
   * The FormComponent reference
   */
  @ViewChild('formRef') public formRef: FormComponent;

  /**
   * The list of available access condition
   * @type {Array}
   */
  public availableAccessConditionOptions: any[];

  /**
   * The submission id
   * @type {string}
   */
  public collectionId: string;

  /**
   * Define if collection access conditions policy type :
   * POLICY_DEFAULT_NO_LIST : is not possible to define additional access group/s for the single file
   * POLICY_DEFAULT_WITH_LIST : is possible to define additional access group/s for the single file
   * @type {number}
   */
  public collectionPolicyType: number;

  /**
   * The configuration for the bitstream's metadata form
   * @type {SubmissionFormsModel}
   */
  public configMetadataForm: SubmissionFormsModel;

  /**
   * The bitstream's metadata data
   * @type {WorkspaceitemSectionUploadFileObject}
   */
  public fileData: WorkspaceitemSectionUploadFileObject;

  /**
   * The bitstream id
   * @type {string}
   */
  public fileId: string;

  /**
   * The bitstream array key
   * @type {string}
   */
  public fileIndex: string;

  /**
   * The form id
   * @type {string}
   */
  public formId: string;

  /**
   * The section id
   * @type {string}
   */
  public sectionId: string;

  /**
   * The submission id
   * @type {string}
   */
  public submissionId: string;

  /**
   * The list of all available metadata
   */
  formMetadata: string[] = [];

  /**
   * The form model
   * @type {DynamicFormControlModel[]}
   */
  formModel: DynamicFormControlModel[];

  /**
   * When `true` form controls are deactivated
   */
  isSaving = false;

  /**
   * The [JsonPatchOperationPathCombiner] object
   * @type {JsonPatchOperationPathCombiner}
   */
  protected pathCombiner: JsonPatchOperationPathCombiner;

  protected subscriptions: Subscription[] = [];

  /**
   * Initialize instance variables
   *
   * @param activeModal
   * @param {ChangeDetectorRef} cdr
   * @param {FormBuilderService} formBuilderService
   * @param {FormService} formService
   * @param {SubmissionService} submissionService
   * @param {JsonPatchOperationsBuilder} operationsBuilder
   * @param {SubmissionJsonPatchOperationsService} operationsService
   * @param {SectionUploadService} uploadService
   */
  constructor(
    protected activeModal: NgbActiveModal,
    private cdr: ChangeDetectorRef,
    private formBuilderService: FormBuilderService,
    private formService: FormService,
    private submissionService: SubmissionService,
    private operationsBuilder: JsonPatchOperationsBuilder,
    private operationsService: SubmissionJsonPatchOperationsService,
    private uploadService: SectionUploadService,
  ) {
  }

  /**
   * Initialize form model values
   *
   * @param formModel
   *    The form model
   */
  public initModelData(formModel: DynamicFormControlModel[]) {
    this.fileData.accessConditions.forEach((accessCondition, index) => {
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
   * Dispatch form model update when changing an access condition
   *
   * @param event
   *    The event emitted
   */
  onChange(event: DynamicFormControlEvent) {
    if (event.model.id === 'name') {
      this.setOptions(event.model, event.control);
    }
  }

  onModalClose() {
    this.activeModal.dismiss();
  }

  onSubmit() {
    this.isSaving = true;
    this.saveBitstreamData();
  }

  /**
   * Update `startDate`, 'groupUUID' and 'endDate' model
   *
   * @param model
   *    The [[DynamicFormControlModel]] object
   * @param control
   *    The [[FormControl]] object
   */
  public setOptions(model: DynamicFormControlModel, control: FormControl) {
    let accessCondition: AccessConditionOption = null;
    this.availableAccessConditionOptions.filter((element) => element.name === control.value)
      .forEach((element) => accessCondition = element );
    if (isNotEmpty(accessCondition)) {
      const startDateControl: FormControl = control.parent.get('startDate') as FormControl;
      const endDateControl: FormControl = control.parent.get('endDate') as FormControl;

      // Clear previous state
      startDateControl?.markAsUntouched();
      endDateControl?.markAsUntouched();

      startDateControl?.setValue(null);
      control.parent.markAsDirty();
      endDateControl?.setValue(null);
    }
  }

  /**
   * Dispatch form model init
   */
  ngOnInit() {
    if (this.fileData && this.formId) {
      this.formModel = this.buildFileEditForm();
      this.cdr.detectChanges();
    }
  }

  ngOnDestroy(): void {
    this.unsubscribeAll();
  }

  protected retrieveValueFromField(field: any) {
    const temp = Array.isArray(field) ? field[0] : field;
    return (temp) ? temp.value : undefined;
  }

  /**
   * Initialize form model
   */
  protected buildFileEditForm() {
    const configDescr: FormFieldModel = Object.assign({}, this.configMetadataForm.rows[0].fields[0]);
    configDescr.repeatable = false;
    const configForm = Object.assign({}, this.configMetadataForm, {
      fields: Object.assign([], this.configMetadataForm.rows[0].fields[0], [
        this.configMetadataForm.rows[0].fields[0],
        configDescr
      ])
    });
    const formModel: DynamicFormControlModel[] = [];
    const metadataGroupModelConfig = Object.assign({}, BITSTREAM_METADATA_FORM_GROUP_CONFIG);
    metadataGroupModelConfig.group = this.formBuilderService.modelFromConfiguration(
      this.submissionId,
      configForm,
      this.collectionId,
      this.fileData.metadata,
      this.submissionService.getSubmissionScope()
    );
    formModel.push(new DynamicFormGroupModel(metadataGroupModelConfig, BITSTREAM_METADATA_FORM_GROUP_LAYOUT));
    const accessConditionTypeModelConfig = Object.assign({}, BITSTREAM_FORM_ACCESS_CONDITION_TYPE_CONFIG);
    const accessConditionsArrayConfig = Object.assign({}, BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_CONFIG);
    const accessConditionTypeOptions = [];

    if (this.collectionPolicyType === POLICY_DEFAULT_WITH_LIST) {
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
        const type = new DynamicSelectModel(accessConditionTypeModelConfig, BITSTREAM_FORM_ACCESS_CONDITION_TYPE_LAYOUT);
        const startDateConfig = Object.assign({}, BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_CONFIG, confStart);
        if (maxStartDate) {
          startDateConfig.max = maxStartDate;
        }

        const endDateConfig = Object.assign({}, BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_CONFIG, confEnd);
        if (maxEndDate) {
          endDateConfig.max = maxEndDate;
        }

        const startDate = new DynamicDatePickerModel(startDateConfig, BITSTREAM_FORM_ACCESS_CONDITION_START_DATE_LAYOUT);
        const endDate = new DynamicDatePickerModel(endDateConfig, BITSTREAM_FORM_ACCESS_CONDITION_END_DATE_LAYOUT);
        const accessConditionGroupConfig = Object.assign({}, BITSTREAM_ACCESS_CONDITION_GROUP_CONFIG);
        accessConditionGroupConfig.group = [type];
        if (hasStartDate) {
          accessConditionGroupConfig.group.push(startDate);
        }
        if (hasEndDate) {
          accessConditionGroupConfig.group.push(endDate);
        }
        return [new DynamicFormGroupModel(accessConditionGroupConfig, BITSTREAM_ACCESS_CONDITION_GROUP_LAYOUT)];
      };

      // Number of access conditions blocks in form
      accessConditionsArrayConfig.initialCount = isNotEmpty(this.fileData.accessConditions) ? this.fileData.accessConditions.length : 1;
      formModel.push(
        new DynamicFormArrayModel(accessConditionsArrayConfig, BITSTREAM_ACCESS_CONDITIONS_FORM_ARRAY_LAYOUT)
      );

    }
    this.initModelData(formModel);
    return formModel;
  }

  /**
   * Save bitstream metadata
   */
  saveBitstreamData() {
    // validate form
    this.formService.validateAllFormFields(this.formRef.formGroup);
    const saveBitstreamDataSubscription = this.formService.isValid(this.formId).pipe(
      take(1),
      filter((isValid) => isValid),
      mergeMap(() => this.formService.getFormData(this.formId)),
      take(1),
      mergeMap((formData: any) => {
        // collect bitstream metadata
        Object.keys((formData.metadata))
          .filter((key) => isNotEmpty(formData.metadata[key]))
          .forEach((key) => {
            const metadataKey = key.replace(/_/g, '.');
            const path = `metadata/${metadataKey}`;
            this.operationsBuilder.add(this.pathCombiner.getPath(path), formData.metadata[key], true);
          });
        Object.keys((this.fileData.metadata))
          .filter((key) => isNotEmpty(this.fileData.metadata[key]))
          .filter((key) => hasNoValue(formData.metadata[key]))
          .filter((key) => this.formMetadata.includes(key))
          .forEach((key) => {
            const metadataKey = key.replace(/_/g, '.');
            const path = `metadata/${metadataKey}`;
            this.operationsBuilder.remove(this.pathCombiner.getPath(path));
          });
        const accessConditionsToSave = [];
        formData.accessConditions
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

        if (isNotEmpty(accessConditionsToSave)) {
          this.operationsBuilder.add(this.pathCombiner.getPath('accessConditions'), accessConditionsToSave, true);
        }

        // dispatch a PATCH request to save metadata
        return this.operationsService.jsonPatchByResourceID(
          this.submissionService.getSubmissionObjectLinkName(),
          this.submissionId,
          this.pathCombiner.rootElement,
          this.pathCombiner.subRootElement);
      })
    ).subscribe((result: SubmissionObject[]) => {
      if (result[0].sections[this.sectionId]) {
        const uploadSection = (result[0].sections[this.sectionId] as WorkspaceitemSectionUploadObject);
        Object.keys(uploadSection.files)
          .filter((key) => uploadSection.files[key].uuid === this.fileId)
          .forEach((key) => this.uploadService.updateFileData(
            this.submissionId, this.sectionId, this.fileId, uploadSection.files[key])
          );
      }
      this.isSaving = false;
      this.activeModal.close();
    });
    this.subscriptions.push(saveBitstreamDataSubscription);
  }

  private unsubscribeAll() {
    this.subscriptions.filter((sub) => hasValue(sub)).forEach((sub) => sub.unsubscribe());
  }

}
