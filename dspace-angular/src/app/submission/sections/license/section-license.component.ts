import { ChangeDetectorRef, Component, Inject, ViewChild } from '@angular/core';
import {
  DynamicCheckboxModel,
  DynamicFormControlEvent,
  DynamicFormControlModel,
  DynamicFormLayout
} from '@ng-dynamic-forms/core';

import { Observable, Subscription } from 'rxjs';
import { distinctUntilChanged, filter, find, map, mergeMap, startWith, take } from 'rxjs/operators';
import { CollectionDataService } from '../../../core/data/collection-data.service';
import { RemoteData } from '../../../core/data/remote-data';
import { JsonPatchOperationPathCombiner } from '../../../core/json-patch/builder/json-patch-operation-path-combiner';
import { JsonPatchOperationsBuilder } from '../../../core/json-patch/builder/json-patch-operations-builder';
import { Collection } from '../../../core/shared/collection.model';
import { License } from '../../../core/shared/license.model';
import { WorkspaceitemSectionLicenseObject } from '../../../core/submission/models/workspaceitem-section-license.model';
import { hasValue, isNotEmpty, isNotNull, isNotUndefined } from '../../../shared/empty.util';
import { FormBuilderService } from '../../../shared/form/builder/form-builder.service';
import { FormComponent } from '../../../shared/form/form.component';
import { FormService } from '../../../shared/form/form.service';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { SubmissionService } from '../../submission.service';
import { SectionFormOperationsService } from '../form/section-form-operations.service';
import { SectionDataObject } from '../models/section-data.model';

import { SectionModelComponent } from '../models/section.model';
import { renderSectionFor } from '../sections-decorator';
import { SectionsType } from '../sections-type';
import { SectionsService } from '../sections.service';
import { SECTION_LICENSE_FORM_LAYOUT, SECTION_LICENSE_FORM_MODEL } from './section-license.model';
import { TranslateService } from '@ngx-translate/core';

/**
 * This component represents a section that contains the submission license form.
 */
@Component({
  selector: 'ds-submission-section-license',
  styleUrls: ['./section-license.component.scss'],
  templateUrl: './section-license.component.html',
})
@renderSectionFor(SectionsType.License)
export class SubmissionSectionLicenseComponent extends SectionModelComponent {

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
   * The [[DynamicFormLayout]] object
   * @type {DynamicFormLayout}
   */
  public formLayout: DynamicFormLayout = SECTION_LICENSE_FORM_LAYOUT;

  /**
   * A boolean representing if to show form submit and cancel buttons
   * @type {boolean}
   */
  public displaySubmit = false;

  /**
   * The submission license text
   * @type {Array}
   */
  public licenseText$: Observable<string>;

  /**
   * The [[JsonPatchOperationPathCombiner]] object
   * @type {JsonPatchOperationPathCombiner}
   */
  protected pathCombiner: JsonPatchOperationPathCombiner;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  protected subs: Subscription[] = [];

  /**
   * The FormComponent reference
   */
  @ViewChild('formRef') private formRef: FormComponent;

  /**
   * Initialize instance variables
   *
   * @param {ChangeDetectorRef} changeDetectorRef
   * @param {CollectionDataService} collectionDataService
   * @param {FormBuilderService} formBuilderService
   * @param {SectionFormOperationsService} formOperationsService
   * @param {FormService} formService
   * @param {JsonPatchOperationsBuilder} operationsBuilder
   * @param {SectionsService} sectionService
   * @param {SubmissionService} submissionService
   * @param {TranslateService} translateService
   * @param {string} injectedCollectionId
   * @param {SectionDataObject} injectedSectionData
   * @param {string} injectedSubmissionId
   */
  constructor(protected changeDetectorRef: ChangeDetectorRef,
              protected collectionDataService: CollectionDataService,
              protected formBuilderService: FormBuilderService,
              protected formOperationsService: SectionFormOperationsService,
              protected formService: FormService,
              protected operationsBuilder: JsonPatchOperationsBuilder,
              protected sectionService: SectionsService,
              protected submissionService: SubmissionService,
              protected translateService: TranslateService,
              @Inject('collectionIdProvider') public injectedCollectionId: string,
              @Inject('sectionDataProvider') public injectedSectionData: SectionDataObject,
              @Inject('submissionIdProvider') public injectedSubmissionId: string) {
    super(injectedCollectionId, injectedSectionData, injectedSubmissionId);
  }

  /**
   * Initialize all instance variables and retrieve submission license
   */
  onSectionInit() {
    this.pathCombiner = new JsonPatchOperationPathCombiner('sections', this.sectionData.id);
    this.formId = this.formService.getUniqueId(this.sectionData.id);
    this.formModel = this.formBuilderService.fromJSON(SECTION_LICENSE_FORM_MODEL);
    const model = this.formBuilderService.findById('granted', this.formModel);

    // Translate checkbox label
    model.label = this.translateService.instant(model.label);

    // Retrieve license accepted status
    (model as DynamicCheckboxModel).value = (this.sectionData.data as WorkspaceitemSectionLicenseObject).granted;

    this.licenseText$ = this.collectionDataService.findById(this.collectionId, true, true, followLink('license')).pipe(
      filter((collectionData: RemoteData<Collection>) => isNotUndefined((collectionData.payload))),
      mergeMap((collectionData: RemoteData<Collection>) => (collectionData.payload as any).license),
      find((licenseData: RemoteData<License>) => isNotUndefined((licenseData.payload))),
      map((licenseData: RemoteData<License>) => licenseData.payload.text),
      startWith(''));

    this.subs.push(
      // Disable checkbox whether it's in workflow or item scope
      this.sectionService.isSectionReadOnly(
        this.submissionId,
        this.sectionData.id,
        this.submissionService.getSubmissionScope()).pipe(
        take(1),
        filter((isReadOnly) => isReadOnly))
        .subscribe(() => {
          model.disabled = true;
        }),

      this.sectionService.getSectionErrors(this.submissionId, this.sectionData.id).pipe(
        filter((errors) => isNotEmpty(errors)),
        distinctUntilChanged())
        .subscribe((errors) => {
          // parse errors
          const newErrors = errors.map((error) => {
            // When the error path is only on the section,
            // replace it with the path to the form field to display error also on the form
            if (error.path === '/sections/license') {
              // check whether license is not accepted
              if (!(model as DynamicCheckboxModel).checked) {
                return Object.assign({}, error, { path: '/sections/license/granted' });
              } else {
                return null;
              }
            } else {
              return error;
            }
          }).filter((error) => isNotNull(error));

          if (isNotEmpty(newErrors)) {
            this.sectionService.checkSectionErrors(this.submissionId, this.sectionData.id, this.formId, newErrors);
            this.sectionData.errors = errors;
          } else {
            // Remove any section's errors
            this.sectionService.dispatchRemoveSectionErrors(this.submissionId, this.sectionData.id);
          }
          this.changeDetectorRef.detectChanges();
        })
    );
  }

  /**
   * Get section status
   *
   * @return Observable<boolean>
   *     the section status
   */
  protected getSectionStatus(): Observable<boolean> {
    const model = this.formBuilderService.findById('granted', this.formModel);
    return (model as DynamicCheckboxModel).valueChanges.pipe(
      map((value) => value === true),
      startWith((model as DynamicCheckboxModel).value));
  }

  /**
   * Method called when a form dfChange event is fired.
   * Dispatch form operations based on changes.
   */
  onChange(event: DynamicFormControlEvent) {
    const path = this.formOperationsService.getFieldPathSegmentedFromChangeEvent(event);
    const value = this.formOperationsService.getFieldValueFromChangeEvent(event);
    if (value) {
      this.operationsBuilder.add(this.pathCombiner.getPath(path), value.value.toString(), false, true);
      // Remove any section's errors
      this.sectionService.dispatchRemoveSectionErrors(this.submissionId, this.sectionData.id);
    } else {
      this.operationsBuilder.remove(this.pathCombiner.getPath(path));
    }
  }

  /**
   * Unsubscribe from all subscriptions
   */
  onSectionDestroy() {
    this.subs
      .filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

}
