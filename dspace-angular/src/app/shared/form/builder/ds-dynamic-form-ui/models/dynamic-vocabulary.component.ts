import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import {
  DynamicFormControlComponent,
  DynamicFormLayoutService,
  DynamicFormValidationService
} from '@ng-dynamic-forms/core';
import { map } from 'rxjs/operators';
import { Observable, of as observableOf } from 'rxjs';

import { VocabularyService } from '../../../../../core/submission/vocabularies/vocabulary.service';
import { isNotEmpty } from '../../../../empty.util';
import { FormFieldMetadataValueObject } from '../../models/form-field-metadata-value.model';
import { VocabularyEntry } from '../../../../../core/submission/vocabularies/models/vocabulary-entry.model';
import { DsDynamicInputModel } from './ds-dynamic-input.model';
import { PageInfo } from '../../../../../core/shared/page-info.model';

/**
 * An abstract class to be extended by form components that handle vocabulary
 */
@Component({
  selector: 'ds-dynamic-vocabulary',
  template: ''
})
export abstract class DsDynamicVocabularyComponent extends DynamicFormControlComponent {

  @Input() abstract group: FormGroup;
  @Input() abstract model: DsDynamicInputModel;

  @Output() abstract blur: EventEmitter<any>;
  @Output() abstract change: EventEmitter<any>;
  @Output() abstract focus: EventEmitter<any>;

  public abstract pageInfo: PageInfo;

  protected constructor(protected vocabularyService: VocabularyService,
                        protected layoutService: DynamicFormLayoutService,
                        protected validationService: DynamicFormValidationService
  ) {
    super(layoutService, validationService);
  }

  /**
   * Sets the current value with the given value.
   * @param value The value to set.
   * @param init Representing if is init value or not.
   */
  public abstract setCurrentValue(value: any, init?: boolean);

  /**
   * Retrieves the init form value from model
   */
  getInitValueFromModel(): Observable<FormFieldMetadataValueObject> {
    let initValue$: Observable<FormFieldMetadataValueObject>;
    if (isNotEmpty(this.model.value) && (this.model.value instanceof FormFieldMetadataValueObject)) {
      let initEntry$: Observable<VocabularyEntry>;
      if (this.model.value.hasAuthority()) {
        initEntry$ = this.vocabularyService.getVocabularyEntryByID(this.model.value.authority, this.model.vocabularyOptions);
      } else {
        initEntry$ = this.vocabularyService.getVocabularyEntryByValue(this.model.value.value, this.model.vocabularyOptions);
      }
      initValue$ = initEntry$.pipe(map((initEntry: VocabularyEntry) => {
        if (isNotEmpty(initEntry)) {
          // Integrate FormFieldMetadataValueObject with retrieved information
          return new FormFieldMetadataValueObject(
            initEntry.value,
            null,
            initEntry.authority,
            initEntry.display,
            (this.model.value as any).place,
            null,
            initEntry.otherInformation || null
          );
        } else {
          return this.model.value as any;
        }
      }));
    } else if (isNotEmpty(this.model.value) && (this.model.value instanceof VocabularyEntry)) {
      initValue$ = observableOf(
        new FormFieldMetadataValueObject(
          this.model.value.value,
          null,
          this.model.value.authority,
          this.model.value.display,
          0,
          null,
          this.model.value.otherInformation || null
        )
      );
    } else {
      initValue$ = observableOf(new FormFieldMetadataValueObject(this.model.value));
    }
    return initValue$;
  }

  /**
   * Emits a blur event containing a given value.
   * @param event The value to emit.
   */
  onBlur(event: Event) {
    this.blur.emit(event);
  }

  /**
   * Emits a focus event containing a given value.
   * @param event The value to emit.
   */
  onFocus(event) {
    this.focus.emit(event);
  }

  /**
   * Emits a change event and updates model value.
   * @param updateValue
   */
  dispatchUpdate(updateValue: any) {
    this.model.value = updateValue;
    this.change.emit(updateValue);
  }

  /**
   * Update the page info object
   * @param elementsPerPage
   * @param currentPage
   * @param totalElements
   * @param totalPages
   */
  protected updatePageInfo(elementsPerPage: number, currentPage: number, totalElements?: number, totalPages?: number) {
    this.pageInfo = Object.assign(new PageInfo(), {
      elementsPerPage: elementsPerPage,
      currentPage: currentPage,
      totalElements: totalElements,
      totalPages: totalPages
    });
  }
}
