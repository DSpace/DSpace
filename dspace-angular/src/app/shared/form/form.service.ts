import { distinctUntilChanged, filter, map } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';

import { AppState } from '../../app.reducer';
import { formObjectFromIdSelector } from './selectors';
import { FormBuilderService } from './builder/form-builder.service';
import { DynamicFormControlEvent, DynamicFormControlModel, DynamicFormGroupModel } from '@ng-dynamic-forms/core';
import { isEmpty, isNotUndefined } from '../empty.util';
import uniqueId from 'lodash/uniqueId';
import {
  FormAddError,
  FormAddTouchedAction,
  FormChangeAction,
  FormInitAction,
  FormRemoveAction,
  FormRemoveErrorAction,
  FormStatusChangeAction
} from './form.actions';
import { FormEntry, FormError, FormTouchedState } from './form.reducer';
import { environment } from '../../../environments/environment';

@Injectable()
export class FormService {

  constructor(
    private formBuilderService: FormBuilderService,
    private store: Store<AppState>) {
  }

  /**
   * Method to retrieve form's status from state
   */
  public isValid(formId: string): Observable<boolean> {
    return this.store.pipe(
      select(formObjectFromIdSelector(formId)),
      filter((state) => isNotUndefined(state)),
      map((state) => state.valid),
      distinctUntilChanged()
    );
  }

  /**
   * Method to retrieve form's data from state
   */
  public getFormData(formId: string): Observable<any> {
    return this.store.pipe(
      select(formObjectFromIdSelector(formId)),
      filter((state) => isNotUndefined(state)),
      map((state) => state.data),
      distinctUntilChanged()
    );
  }

  /**
   * Method to retrieve form's touched state
   */
  public getFormTouchedState(formId: string): Observable<FormTouchedState> {
    return this.store.pipe(
      select(formObjectFromIdSelector(formId)),
      filter((state) => isNotUndefined(state)),
      map((state) => state.touched),
      distinctUntilChanged()
    );
  }

  /**
   * Method to retrieve form's errors from state
   */
  public getFormErrors(formId: string): Observable<FormError[]> {
    return this.store.pipe(
      select(formObjectFromIdSelector(formId)),
      filter((state) => isNotUndefined(state)),
      map((state) => state.errors),
      distinctUntilChanged()
    );
  }

  /**
   * Method to retrieve form's data from state
   */
  public isFormInitialized(formId: string): Observable<boolean> {
    return this.store.pipe(
      select(formObjectFromIdSelector(formId)),
      distinctUntilChanged(),
      map((state) => isNotUndefined(state))
    );
  }

  public getUniqueId(formId): string {
    return uniqueId() + '_' + formId;
  }

  /**
   * Method to validate form's fields
   */
  public validateAllFormFields(formGroup: FormGroup | FormArray) {
    Object.keys(formGroup.controls).forEach((field) => {
      const control = formGroup.get(field);
      if (control instanceof FormControl) {
        control.markAsTouched({ onlySelf: true });
        control.markAsDirty({ onlySelf: true });
      } else if (control instanceof FormGroup || control instanceof FormArray) {
        this.validateAllFormFields(control);
      }
    });
  }

  /**
   * Check if form group has an invalid form control
   * @param formGroup The form group to check
   */
  public hasValidationErrors(formGroup: FormGroup | FormArray): boolean {
    let hasErrors = false;
    const fields: string[] = Object.keys(formGroup.controls);
    for (const field of fields) {
      const control = formGroup.get(field);
      if (control instanceof FormControl) {
        hasErrors = !control.valid && control.touched;
      } else if (control instanceof FormGroup || control instanceof FormArray) {
        hasErrors = this.hasValidationErrors(control);
      }
      if (hasErrors) {
        break;
      }
    }
    return hasErrors;
  }

  public addControlErrors(field: AbstractControl, formId: string, fieldId: string, fieldIndex: number) {
    const errors: string[] = Object.keys(field.errors)
      .filter((errorKey) => field.errors[errorKey] === true)
      .map((errorKey) => `error.validation.${errorKey}`);
    errors.forEach((error) => this.addError(formId, fieldId, fieldIndex, error));
  }

  public addErrorToField(field: AbstractControl, model: DynamicFormControlModel, message: string) {
    const error = {}; // create the error object
    const errorKey = this.getValidatorNameFromMap(message);
    let errorMsg = message;

    // if form control model has no errorMessages object, create it
    if (!model.errorMessages) {
      model.errorMessages = {};
    }

    // check if error code is already present in the set of model's validators
    if (isEmpty(model.errorMessages[errorKey])) {
      // put the error message in the form control model
      model.errorMessages[errorKey] = message;
    } else {
      // Use correct error messages from the model
      errorMsg = model.errorMessages[errorKey];
    }

    if (!field.hasError(errorKey)) {
      error[errorKey] = true;
      // add the error in the form control
      field.setErrors(error);
    }

    // if the field in question is a concat group, pass down the error to its fields
    if (field instanceof FormGroup && model instanceof DynamicFormGroupModel && this.formBuilderService.isConcatGroup(model)) {
      model.group.forEach((subModel) => {
        const subField = field.controls[subModel.id];

        this.addErrorToField(subField, subModel, message);
      });
    }

    field.markAsTouched();
  }

  public removeErrorFromField(field: AbstractControl, model: DynamicFormControlModel, messageKey: string) {
    const error = {};
    const errorKey = this.getValidatorNameFromMap(messageKey);

    if (field.hasError(errorKey)) {
      error[errorKey] = null;
      field.setErrors(error);
    }

    // if the field in question is a concat group, clear the error from its fields
    if (field instanceof FormGroup && model instanceof DynamicFormGroupModel && this.formBuilderService.isConcatGroup(model)) {
      model.group.forEach((subModel) => {
        const subField = field.controls[subModel.id];

        this.removeErrorFromField(subField, subModel, messageKey);
      });
    }

    field.markAsUntouched();
  }

  public resetForm(formGroup: FormGroup, groupModel: DynamicFormControlModel[], formId: string) {
    this.formBuilderService.clearAllModelsValue(groupModel);
    formGroup.reset();
    this.store.dispatch(new FormChangeAction(formId, formGroup.value));
  }

  private getValidatorNameFromMap(validator): string {
    if (validator.includes('.')) {
      const splitArray = validator.split('.');
      if (splitArray && splitArray.length > 0) {
        validator = this.getValidatorNameFromMap(splitArray[splitArray.length - 1]);
      }
    }
    return (environment.form.validatorMap.hasOwnProperty(validator)) ? environment.form.validatorMap[validator] : validator;
  }

  public initForm(formId: string, model: DynamicFormControlModel[], valid: boolean) {
    this.store.dispatch(new FormInitAction(formId, this.formBuilderService.getValueFromModel(model), valid));
  }

  public setStatusChanged(formId: string, valid: boolean) {
    this.store.dispatch(new FormStatusChangeAction(formId, valid));
  }

  public getForm(formId: string): Observable<FormEntry> {
    return this.store.pipe(select(formObjectFromIdSelector(formId)));
  }

  public removeForm(formId: string) {
    this.store.dispatch(new FormRemoveAction(formId));
  }

  public changeForm(formId: string, model: DynamicFormControlModel[]) {
    this.store.dispatch(new FormChangeAction(formId, this.formBuilderService.getValueFromModel(model)));
  }

  public setTouched(formId: string, model: DynamicFormControlModel[], event: DynamicFormControlEvent) {
    const ids = this.formBuilderService.getMetadataIdsFromEvent(event);
    this.store.dispatch(new FormAddTouchedAction(formId, ids));
  }

  public addError(formId: string, fieldId: string, fieldIndex: number, message: string) {
    const normalizedFieldId = fieldId.replace(/\./g, '_');
    this.store.dispatch(new FormAddError(formId, normalizedFieldId, fieldIndex, message));
  }
  public removeError(formId: string, fieldId: string, fieldIndex: number) {
    const normalizedFieldId = fieldId.replace(/\./g, '_');
    this.store.dispatch(new FormRemoveErrorAction(formId, normalizedFieldId, fieldIndex));
  }
}
