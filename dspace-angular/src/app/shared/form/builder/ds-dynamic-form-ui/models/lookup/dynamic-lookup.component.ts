import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { of as observableOf, Subscription } from 'rxjs';
import { catchError, distinctUntilChanged } from 'rxjs/operators';
import { NgbDropdown } from '@ng-bootstrap/ng-bootstrap';
import { DynamicFormLayoutService, DynamicFormValidationService } from '@ng-dynamic-forms/core';

import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { hasValue, isEmpty, isNotEmpty } from '../../../../../empty.util';
import { PageInfo } from '../../../../../../core/shared/page-info.model';
import { FormFieldMetadataValueObject } from '../../../models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { DynamicLookupNameModel } from './dynamic-lookup-name.model';
import { ConfidenceType } from '../../../../../../core/shared/confidence-type';
import {
  PaginatedList,
  buildPaginatedList
} from '../../../../../../core/data/paginated-list.model';
import { getFirstSucceededRemoteDataPayload } from '../../../../../../core/shared/operators';
import { DsDynamicVocabularyComponent } from '../dynamic-vocabulary.component';

/**
 * Component representing a lookup or lookup-name input field
 */
@Component({
  selector: 'ds-dynamic-lookup',
  styleUrls: ['./dynamic-lookup.component.scss'],
  templateUrl: './dynamic-lookup.component.html'
})
export class DsDynamicLookupComponent extends DsDynamicVocabularyComponent implements OnDestroy, OnInit {

  @Input() group: FormGroup;
  @Input() model: any;

  @Output() blur: EventEmitter<any> = new EventEmitter<any>();
  @Output() change: EventEmitter<any> = new EventEmitter<any>();
  @Output() focus: EventEmitter<any> = new EventEmitter<any>();

  public editMode = false;
  public firstInputValue = '';
  public secondInputValue = '';
  public loading = false;
  public pageInfo: PageInfo;
  public optionsList: any;

  protected subs: Subscription[] = [];

  constructor(protected vocabularyService: VocabularyService,
              private cdr: ChangeDetectorRef,
              protected layoutService: DynamicFormLayoutService,
              protected validationService: DynamicFormValidationService
  ) {
    super(vocabularyService, layoutService, validationService);
  }

  /**
   * Converts an item from the result list to a `string` to display in the `<input>` field.
   */
  inputFormatter = (x: { display: string }, y: number) => {
    return y === 1 ? this.firstInputValue : this.secondInputValue;
  };

  /**
   * Initialize the component, setting up the init form value
   */
  ngOnInit() {
    if (isNotEmpty(this.model.value)) {
      this.setCurrentValue(this.model.value, true);
    }

    this.subs.push(this.model.valueChanges
      .subscribe((value) => {
        if (isEmpty(value)) {
          this.resetFields();
        } else if (!this.editMode) {
          this.setCurrentValue(this.model.value);
        }
      }));
  }

  /**
   * Check if model value has an authority
   */
  public hasAuthorityValue() {
    return hasValue(this.model.value)
      && typeof this.model.value === 'object'
      && this.model.value.hasAuthority();
  }

  /**
   * Check if current value has an authority
   */
  public hasEmptyValue() {
    return isNotEmpty(this.getCurrentValue());
  }

  /**
   * Clear inputs whether there is no results and authority is closed
   */
  public clearFields() {
    if (this.model.vocabularyOptions.closed) {
      this.resetFields();
    }
  }

  /**
   * Check if edit button is disabled
   */
  public isEditDisabled() {
    return !this.hasAuthorityValue();
  }

  /**
   * Check if input is disabled
   */
  public isInputDisabled() {
    return (this.model.vocabularyOptions.closed && this.hasAuthorityValue() && !this.editMode);
  }

  /**
   * Check if model is instanceof DynamicLookupNameModel
   */
  public isLookupName() {
    return (this.model instanceof DynamicLookupNameModel);
  }

  /**
   * Check if search button is disabled
   */
  public isSearchDisabled() {
    return isEmpty(this.firstInputValue) || this.editMode;
  }

  /**
   * Update model value with the typed text if vocabulary is not closed
   * @param event the typed text
   */
  public onChange(event) {
    event.preventDefault();
    if (!this.model.vocabularyOptions.closed) {
      if (isNotEmpty(this.getCurrentValue())) {
        const currentValue = new FormFieldMetadataValueObject(this.getCurrentValue());
        if (!this.editMode) {
          this.updateModel(currentValue);
        }
      } else {
        this.remove();
      }
    }
  }

  /**
   * Load more result entries
   */
  public onScroll() {
    if (!this.loading && this.pageInfo.currentPage <= this.pageInfo.totalPages) {
      this.updatePageInfo(
        this.pageInfo.elementsPerPage,
        this.pageInfo.currentPage + 1,
        this.pageInfo.totalElements,
        this.pageInfo.totalPages
      );
      this.search();
    }
  }

  /**
   * Update model value with selected entry
   * @param event the selected entry
   */
  public onSelect(event) {
    this.updateModel(event);
  }

  /**
   * Reset the current value when dropdown toggle
   */
  public openChange(isOpened: boolean) {
    if (!isOpened) {
      if (this.model.vocabularyOptions.closed && !this.hasAuthorityValue()) {
        this.setCurrentValue('');
      }
    }
  }

  /**
   * Reset the model value
   */
  public remove() {
    this.group.markAsPristine();
    this.dispatchUpdate(null);
  }

  /**
   * Saves all changes
   */
  public saveChanges() {
    if (isNotEmpty(this.getCurrentValue())) {
      const newValue = Object.assign(new VocabularyEntry(), this.model.value, {
        display: this.getCurrentValue(),
        value: this.getCurrentValue()
      });
      this.updateModel(newValue);
    } else {
      this.remove();
    }
    this.switchEditMode();
  }

  /**
   * Converts a stream of text values from the `<input>` element to the stream of the array of items
   * to display in the result list.
   */
  public search() {
    this.optionsList = null;
    this.updatePageInfo(this.model.maxOptions, 1);
    this.loading = true;

    this.subs.push(this.vocabularyService.getVocabularyEntriesByValue(
      this.getCurrentValue(),
      false,
      this.model.vocabularyOptions,
      this.pageInfo
    ).pipe(
      getFirstSucceededRemoteDataPayload(),
      catchError(() =>
        observableOf(buildPaginatedList(
          new PageInfo(),
          []
        ))
      ),
      distinctUntilChanged())
      .subscribe((list: PaginatedList<VocabularyEntry>) => {
        this.optionsList = list.page;
        this.updatePageInfo(
          list.pageInfo.elementsPerPage,
          list.pageInfo.currentPage,
          list.pageInfo.totalElements,
          list.pageInfo.totalPages
        );
        this.loading = false;
        this.cdr.detectChanges();
      }));
  }

  /**
   * Changes the edit mode flag
   */
  public switchEditMode() {
    this.editMode = !this.editMode;
  }

  /**
   * Callback functions for whenClickOnConfidenceNotAccepted event
   */
  public whenClickOnConfidenceNotAccepted(sdRef: NgbDropdown, confidence: ConfidenceType) {
    if (!this.model.readOnly) {
      sdRef.open();
      this.search();
    }
  }

  ngOnDestroy() {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
  }

  /**
   * Sets the current value with the given value.
   * @param value The value to set.
   * @param init Representing if is init value or not.
   */
  public setCurrentValue(value: any, init = false) {
    if (init) {
      this.getInitValueFromModel()
        .subscribe((formValue: FormFieldMetadataValueObject) => this.setDisplayInputValue(formValue.display));
    } else if (hasValue(value)) {
      if (value instanceof FormFieldMetadataValueObject || value instanceof VocabularyEntry) {
        this.setDisplayInputValue(value.display);
      }
    }
  }

  protected setDisplayInputValue(displayValue: string) {
    if (hasValue(displayValue)) {
      if (this.isLookupName()) {
        const values = displayValue.split((this.model as DynamicLookupNameModel).separator);

        this.firstInputValue = (values[0] || '').trim();
        this.secondInputValue = (values[1] || '').trim();
      } else {
        this.firstInputValue = displayValue || '';
      }
      this.cdr.detectChanges();
    }
  }

  /**
   * Gets the current text present in the input field(s)
   */
  protected getCurrentValue(): string {
    let result = '';
    if (!this.isLookupName()) {
      result = this.firstInputValue;
    } else {
      if (isNotEmpty(this.firstInputValue)) {
        result = this.firstInputValue;
      }
      if (isNotEmpty(this.secondInputValue)) {
        result = isEmpty(result)
          ? this.secondInputValue
          : this.firstInputValue + (this.model as DynamicLookupNameModel).separator + ' ' + this.secondInputValue;
      }
    }
    return result;
  }

  /**
   * Clear text present in the input field(s)
   */
  protected resetFields() {
    this.firstInputValue = '';
    if (this.isLookupName()) {
      this.secondInputValue = '';
    }
  }

  protected updateModel(value) {
    this.group.markAsDirty();
    this.dispatchUpdate(value);
    this.setCurrentValue(value);
    this.optionsList = null;
    this.pageInfo = null;
  }

}
