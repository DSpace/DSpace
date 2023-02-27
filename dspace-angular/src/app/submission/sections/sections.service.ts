import { Injectable } from '@angular/core';

import { combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, filter, map, mergeMap, take } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { ScrollToConfigOptions, ScrollToService } from '@nicky-lenaers/ngx-scroll-to';
import findIndex from 'lodash/findIndex';
import findKey from 'lodash/findKey';
import isEqual from 'lodash/isEqual';

import { SubmissionState } from '../submission.reducers';
import { hasValue, isEmpty, isNotEmpty, isNotUndefined } from '../../shared/empty.util';
import {
  DisableSectionAction,
  EnableSectionAction,
  InertSectionErrorsAction,
  RemoveSectionErrorsAction,
  SectionStatusChangeAction,
  SetSectionFormId,
  UpdateSectionDataAction
} from '../objects/submission-objects.actions';
import {
  SubmissionObjectEntry
} from '../objects/submission-objects.reducer';
import {
  submissionObjectFromIdSelector,
  submissionSectionDataFromIdSelector,
  submissionSectionErrorsFromIdSelector,
  submissionSectionFromIdSelector,
  submissionSectionServerErrorsFromIdSelector
} from '../selectors';
import { SubmissionScopeType } from '../../core/submission/submission-scope-type';
import parseSectionErrorPaths, { SectionErrorPath } from '../utils/parseSectionErrorPaths';
import { FormClearErrorsAction } from '../../shared/form/form.actions';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { SubmissionService } from '../submission.service';
import { WorkspaceitemSectionDataType } from '../../core/submission/models/workspaceitem-sections.model';
import { SectionsType } from './sections-type';
import { normalizeSectionData } from '../../core/submission/submission-response-parsing.service';
import { SubmissionFormsModel } from '../../core/config/models/config-submission-forms.model';
import { parseReviver } from '@ng-dynamic-forms/core';
import { FormService } from '../../shared/form/form.service';
import { JsonPatchOperationPathCombiner } from '../../core/json-patch/builder/json-patch-operation-path-combiner';
import { FormError } from '../../shared/form/form.reducer';
import { SubmissionSectionObject } from '../objects/submission-section-object.model';
import { SubmissionSectionError } from '../objects/submission-section-error.model';

/**
 * A service that provides methods used in submission process.
 */
@Injectable()
export class SectionsService {

  /**
   * Initialize service variables
   * @param {FormService} formService
   * @param {NotificationsService} notificationsService
   * @param {ScrollToService} scrollToService
   * @param {SubmissionService} submissionService
   * @param {Store<SubmissionState>} store
   * @param {TranslateService} translate
   */
  constructor(private formService: FormService,
    private notificationsService: NotificationsService,
    private scrollToService: ScrollToService,
    private submissionService: SubmissionService,
    private store: Store<SubmissionState>,
    private translate: TranslateService) {
  }

  /**
   * Compare the list of the current section errors with the previous one,
   * and dispatch actions to add/remove to/from the section state
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The workspaceitem self url
   * @param formId
   *    The [SubmissionDefinitionsModel] that define submission configuration
   * @param currentErrors
   *    The [SubmissionSectionError] that define submission sections init data
   * @param prevErrors
   *    The [SubmissionSectionError] that define submission sections init errors
   */
  public checkSectionErrors(
    submissionId: string,
    sectionId: string,
    formId: string,
    currentErrors: SubmissionSectionError[],
    prevErrors: SubmissionSectionError[] = []) {
    // Remove previous error list if the current is empty
    if (isEmpty(currentErrors)) {
      this.store.dispatch(new RemoveSectionErrorsAction(submissionId, sectionId));
      this.store.dispatch(new FormClearErrorsAction(formId));
    } else if (!isEqual(currentErrors, prevErrors)) { // compare previous error list with the current one
      const dispatchedErrors = [];

      // Iterate over the current error list
      currentErrors.forEach((error: SubmissionSectionError) => {
        const errorPaths: SectionErrorPath[] = parseSectionErrorPaths(error.path);

        errorPaths.forEach((path: SectionErrorPath) => {
          if (path.fieldId) {
            // Dispatch action to add form error to the state;
            this.formService.addError(formId, path.fieldId, path.fieldIndex, error.message);
            dispatchedErrors.push(path.fieldId);
          }
        });
      });

      // Itereate over the previous error list
      prevErrors.forEach((error: SubmissionSectionError) => {
        const errorPaths: SectionErrorPath[] = parseSectionErrorPaths(error.path);

        errorPaths.forEach((path: SectionErrorPath) => {
          if (path.fieldId) {
            if (!dispatchedErrors.includes(path.fieldId)) {
              // Dispatch action to remove form error from the state;
              this.formService.removeError(formId, path.fieldId, path.fieldIndex);
            }
          }
        });
      });
    }
  }

  /**
   * Dispatch a new [RemoveSectionErrorsAction]
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   */
  public dispatchRemoveSectionErrors(submissionId, sectionId) {
    this.store.dispatch(new RemoveSectionErrorsAction(submissionId, sectionId));
  }

  /**
   * Dispatch a new [SetSectionFormId]
   *    The submission id
   * @param sectionId
   *    The section id
   * @param formId
   *    The form id
   */
  public dispatchSetSectionFormId(submissionId, sectionId, formId) {
    this.store.dispatch(new SetSectionFormId(submissionId, sectionId, formId));
  }

  /**
   * Return the data object for the specified section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param sectionType
   *    The type of section to retrieve
   * @return Observable<WorkspaceitemSectionDataType>
   *    observable of [WorkspaceitemSectionDataType]
   */
  public getSectionData(submissionId: string, sectionId: string, sectionType: SectionsType): Observable<WorkspaceitemSectionDataType> {
    return this.store.select(submissionSectionDataFromIdSelector(submissionId, sectionId)).pipe(
      map((sectionData: WorkspaceitemSectionDataType) => {
        if (sectionType === SectionsType.SubmissionForm) {
          return normalizeSectionData(sectionData);
        } else {
          return sectionData;
        }
      }),
      distinctUntilChanged(),
    );
  }

  /**
   * Get the list of validation errors present in the given section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param sectionType
   *    The type of section for which retrieve errors
   */
  getShownSectionErrors(submissionId: string, sectionId: string, sectionType: SectionsType): Observable<SubmissionSectionError[]> {
    let errorsState$: Observable<SubmissionSectionError[]>;
    if (sectionType !== SectionsType.SubmissionForm) {
      errorsState$ = this.getSectionErrors(submissionId, sectionId);
    } else {
      errorsState$ = this.getSectionState(submissionId, sectionId, sectionType).pipe(
        mergeMap((state: SubmissionSectionObject) => this.formService.getFormErrors(state.formId).pipe(
          map((formErrors: FormError[]) => {
            const pathCombiner = new JsonPatchOperationPathCombiner('sections', sectionId);
            const sectionErrors = formErrors
              .map((error) => ({
                path: pathCombiner.getPath(error.fieldId.replace(/\_/g, '.')).path,
                message: error.message
              } as SubmissionSectionError))
              .filter((sectionError: SubmissionSectionError) => findIndex(state.errorsToShow, { path: sectionError.path }) === -1);
            return [...state.errorsToShow, ...sectionErrors];
          })
        ))
      );
    }

    return errorsState$;
  }
  /**
   * Return the error list to show for the specified section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<SubmissionSectionError>
   *    observable of array of [SubmissionSectionError]
   */
  public getSectionErrors(submissionId: string, sectionId: string): Observable<SubmissionSectionError[]> {
    return this.store.select(submissionSectionErrorsFromIdSelector(submissionId, sectionId)).pipe(
      distinctUntilChanged());
  }

  /**
   * Return the error list detected by the server for the specified section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<SubmissionSectionError>
   *    observable of array of [SubmissionSectionError]
   */
  public getSectionServerErrors(submissionId: string, sectionId: string): Observable<SubmissionSectionError[]> {
    return this.store.select(submissionSectionServerErrorsFromIdSelector(submissionId, sectionId)).pipe(
      distinctUntilChanged());
  }

  /**
   * Return the state object for the specified section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param sectionType
   *    The type of section to retrieve
   * @return Observable<SubmissionSectionObject>
   *    observable of [SubmissionSectionObject]
   */
  public getSectionState(submissionId: string, sectionId: string, sectionType: SectionsType): Observable<SubmissionSectionObject> {
    return this.store.select(submissionSectionFromIdSelector(submissionId, sectionId)).pipe(
      filter((sectionObj: SubmissionSectionObject) => hasValue(sectionObj)),
      map((sectionObj: SubmissionSectionObject) => sectionObj),
      map((sectionState: SubmissionSectionObject) => {
        if (hasValue(sectionState.data) && sectionType === SectionsType.SubmissionForm) {
          return Object.assign({}, sectionState, {
            data: normalizeSectionData(sectionState.data)
          });
        } else {
          return sectionState;
        }
      }),
      distinctUntilChanged()
    );
  }

  /**
   * Check if a given section is valid
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<boolean>
   *    Emits true whenever a given section should be valid
   */
  public isSectionValid(submissionId: string, sectionId: string): Observable<boolean> {
    return this.store.select(submissionSectionFromIdSelector(submissionId, sectionId)).pipe(
      filter((sectionObj) => hasValue(sectionObj)),
      map((sectionObj: SubmissionSectionObject) => sectionObj.isValid),
      distinctUntilChanged());
  }

  /**
   * Check if a given section is active
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<boolean>
   *    Emits true whenever a given section should be active
   */
  public isSectionActive(submissionId: string, sectionId: string): Observable<boolean> {
    return this.submissionService.getActiveSectionId(submissionId).pipe(
      map((activeSectionId: string) => sectionId === activeSectionId),
      distinctUntilChanged());
  }

  /**
   * Check if a given section is enabled
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<boolean>
   *    Emits true whenever a given section should be enabled
   */
  public isSectionEnabled(submissionId: string, sectionId: string): Observable<boolean> {
    return this.store.select(submissionSectionFromIdSelector(submissionId, sectionId)).pipe(
      filter((sectionObj) => hasValue(sectionObj)),
      map((sectionObj: SubmissionSectionObject) => sectionObj.enabled),
      distinctUntilChanged());
  }

  /**
   * Check if a given section is a read only section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param submissionScope
   *    The submission scope
   * @return Observable<boolean>
   *    Emits true whenever a given section should be read only
   */
  public isSectionReadOnly(submissionId: string, sectionId: string, submissionScope: SubmissionScopeType): Observable<boolean> {
    return this.store.select(submissionSectionFromIdSelector(submissionId, sectionId)).pipe(
      filter((sectionObj) => hasValue(sectionObj)),
      map((sectionObj: SubmissionSectionObject) => {
        return isNotEmpty(sectionObj.visibility)
          && sectionObj.visibility.other === 'READONLY'
          && submissionScope !== SubmissionScopeType.WorkspaceItem;
      }),
      distinctUntilChanged());
  }

  /**
   * Check if a given section id is present in the list of sections
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @return Observable<boolean>
   *    Emits true whenever a given section id should be available
   */
  public isSectionAvailable(submissionId: string, sectionId: string): Observable<boolean> {
    return this.store.select(submissionObjectFromIdSelector(submissionId)).pipe(
      filter((submissionState: SubmissionObjectEntry) => isNotUndefined(submissionState)),
      map((submissionState: SubmissionObjectEntry) => {
        return isNotUndefined(submissionState.sections) && isNotUndefined(submissionState.sections[sectionId]);
      }),
      distinctUntilChanged());
  }

  /**
   * Check if a given section type is present in the list of sections
   *
   * @param submissionId
   *    The submission id
   * @param sectionType
   *    The section type
   * @return Observable<boolean>
   *    Emits true whenever a given section type should be available
   */
  public isSectionTypeAvailable(submissionId: string, sectionType: SectionsType): Observable<boolean> {
    return this.store.select(submissionObjectFromIdSelector(submissionId)).pipe(
      filter((submissionState: SubmissionObjectEntry) => isNotUndefined(submissionState)),
      map((submissionState: SubmissionObjectEntry) => {
        return isNotUndefined(submissionState.sections) && isNotUndefined(findKey(submissionState.sections, { sectionType: sectionType }));
      }),
      distinctUntilChanged());
  }

  /**
   * Check if given section id is of a given section type
   * @param submissionId
   * @param sectionId
   * @param sectionType
   */
  public isSectionType(submissionId: string, sectionId: string, sectionType: SectionsType): Observable<boolean> {
    return this.store.select(submissionObjectFromIdSelector(submissionId)).pipe(
      filter((submissionState: SubmissionObjectEntry) => isNotUndefined(submissionState)),
      map((submissionState: SubmissionObjectEntry) => {
        return isNotUndefined(submissionState.sections) && isNotUndefined(submissionState.sections[sectionId])
          && submissionState.sections[sectionId].sectionType === sectionType;
      }),
      distinctUntilChanged());
  }

  /**
   * Dispatch a new [EnableSectionAction] to add a new section and move page target to it
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   */
  public addSection(submissionId: string, sectionId: string) {
    this.store.dispatch(new EnableSectionAction(submissionId, sectionId));
    const config: ScrollToConfigOptions = {
      target: sectionId,
      offset: -70
    };

    this.scrollToService.scrollTo(config);
  }

  /**
   * Dispatch a new [DisableSectionAction] to remove section
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   */
  public removeSection(submissionId: string, sectionId: string) {
    this.store.dispatch(new DisableSectionAction(submissionId, sectionId));
  }

  /**
   * Dispatch a new [UpdateSectionDataAction] to update section state with new data and errors
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param data
   *    The section data
   * @param errorsToShow
   *    The list of the section's errors to show. It contains the error list
   *    to display when section is not pristine
   * @param serverValidationErrors
   *    The list of the section's errors detected by the server.
   *    They may not be shown yet if section is pristine
   * @param metadata
   *    The section metadata
   */
  public updateSectionData(
    submissionId: string,
    sectionId: string,
    data: WorkspaceitemSectionDataType,
    errorsToShow: SubmissionSectionError[] = [],
    serverValidationErrors: SubmissionSectionError[] = [],
    metadata?: string[]
  ) {
    if (isNotEmpty(data)) {
      const isAvailable$ = this.isSectionAvailable(submissionId, sectionId);
      const isEnabled$ = this.isSectionEnabled(submissionId, sectionId);

      combineLatest(isAvailable$, isEnabled$).pipe(
        take(1),
        filter(([available, enabled]: [boolean, boolean]) => available))
        .subscribe(([available, enabled]: [boolean, boolean]) => {
          this.store.dispatch(new UpdateSectionDataAction(submissionId, sectionId, data, errorsToShow, serverValidationErrors, metadata));
        });
    }
  }

  /**
   * Dispatch a new [InertSectionErrorsAction] to update section state with new error
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param error
   *    The section error
   */
  public setSectionError(submissionId: string, sectionId: string, error: SubmissionSectionError) {
    this.store.dispatch(new InertSectionErrorsAction(submissionId, sectionId, error));
  }

  /**
   * Dispatch a new [SectionStatusChangeAction] to update section state with new status
   *
   * @param submissionId
   *    The submission id
   * @param sectionId
   *    The section id
   * @param status
   *    The section status
   */
  public setSectionStatus(submissionId: string, sectionId: string, status: boolean) {
    this.store.dispatch(new SectionStatusChangeAction(submissionId, sectionId, status));
  }

  /**
   * Compute the list of selectable metadata for the section configuration.
   * @param formConfig
   */
  public computeSectionConfiguredMetadata(formConfig: string | SubmissionFormsModel): string[] {
    const metadata = [];
    const rawData = typeof formConfig === 'string' ? JSON.parse(formConfig, parseReviver) : formConfig;
    if (rawData.rows && !isEmpty(rawData.rows)) {
      rawData.rows.forEach((currentRow) => {
        if (currentRow.fields && !isEmpty(currentRow.fields)) {
          currentRow.fields.forEach((field) => {
            if (field.selectableMetadata && !isEmpty(field.selectableMetadata)) {
              field.selectableMetadata.forEach((selectableMetadata) => {
                if (!metadata.includes(selectableMetadata.metadata)) {
                  metadata.push(selectableMetadata.metadata);
                }
              });
            }
          });
        }
      });
    }
    return metadata;
  }

  /**
   * Return if the section is an informational type section.
   * @param sectionType
   */
  public getIsInformational(sectionType: SectionsType): boolean {
    if (sectionType === SectionsType.SherpaPolicies) {
      return true;
    } else {
      return false;
    }
  }

}
