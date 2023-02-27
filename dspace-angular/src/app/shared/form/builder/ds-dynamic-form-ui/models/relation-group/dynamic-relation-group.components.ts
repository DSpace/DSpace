import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { combineLatest, Observable, of as observableOf, Subscription } from 'rxjs';
import { filter, map, mergeMap, scan } from 'rxjs/operators';
import {
  DynamicFormControlComponent,
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicFormLayoutService,
  DynamicFormValidationService,
  DynamicInputModel
} from '@ng-dynamic-forms/core';
import isEqual from 'lodash/isEqual';
import isObject from 'lodash/isObject';

import { DynamicRelationGroupModel } from './dynamic-relation-group.model';
import { FormBuilderService } from '../../../form-builder.service';
import { SubmissionFormsModel } from '../../../../../../core/config/models/config-submission-forms.model';
import { FormService } from '../../../../form.service';
import { FormComponent } from '../../../../form.component';
import { Chips } from '../../../../chips/models/chips.model';
import { hasValue, isEmpty, isNotEmpty, isNotNull } from '../../../../../empty.util';
import { shrinkInOut } from '../../../../../animations/shrink';
import { ChipsItem } from '../../../../chips/models/chips-item.model';
import { hasOnlyEmptyProperties } from '../../../../../object.util';
import { VocabularyService } from '../../../../../../core/submission/vocabularies/vocabulary.service';
import { FormFieldMetadataValueObject } from '../../../models/form-field-metadata-value.model';
import { environment } from '../../../../../../../environments/environment';
import { PLACEHOLDER_PARENT_METADATA } from '../../ds-dynamic-form-constants';
import { getFirstSucceededRemoteDataPayload } from '../../../../../../core/shared/operators';
import { VocabularyEntryDetail } from '../../../../../../core/submission/vocabularies/models/vocabulary-entry-detail.model';

/**
 * Component representing a group input field
 */
@Component({
  selector: 'ds-dynamic-relation-group',
  styleUrls: ['./dynamic-relation-group.component.scss'],
  templateUrl: './dynamic-relation-group.component.html',
  animations: [shrinkInOut]
})
export class DsDynamicRelationGroupComponent extends DynamicFormControlComponent implements OnDestroy, OnInit {

  @Input() formId: string;
  @Input() group: FormGroup;
  @Input() model: DynamicRelationGroupModel;

  @Output() blur: EventEmitter<any> = new EventEmitter<any>();
  @Output() change: EventEmitter<any> = new EventEmitter<any>();
  @Output() focus: EventEmitter<any> = new EventEmitter<any>();

  public chips: Chips;
  public formCollapsed = observableOf(false);
  public formModel: DynamicFormControlModel[];
  public editMode = false;

  private selectedChipItem: ChipsItem;
  private subs: Subscription[] = [];

  @ViewChild('formRef') private formRef: FormComponent;

  constructor(private vocabularyService: VocabularyService,
              private formBuilderService: FormBuilderService,
              private formService: FormService,
              private cdr: ChangeDetectorRef,
              protected layoutService: DynamicFormLayoutService,
              protected validationService: DynamicFormValidationService
  ) {
    super(layoutService, validationService);
  }

  ngOnInit() {
    const config = { rows: this.model.formConfiguration } as SubmissionFormsModel;
    if (!this.model.isEmpty()) {
      this.formCollapsed = observableOf(true);
    }
    this.model.valueChanges.subscribe((value: any[]) => {
      if ((isNotEmpty(value) && !(value.length === 1 && hasOnlyEmptyProperties(value[0])))) {
        this.collapseForm();
      } else {
        this.expandForm();
      }
    });

    this.formId = this.formService.getUniqueId(this.model.id);
    this.formModel = this.formBuilderService.modelFromConfiguration(
      this.model.submissionId,
      config,
      this.model.scopeUUID,
      {},
      this.model.submissionScope,
      this.model.readOnly);
    this.initChipsFromModelValue();
  }

  isMandatoryFieldEmpty() {
    let res = true;
    this.formModel.forEach((row) => {
      const modelRow = row as DynamicFormGroupModel;
      modelRow.group.forEach((model: DynamicInputModel) => {
        if (model.name === this.model.mandatoryField) {
          res = model.value == null;
          return;
        }
      });
    });
    return res;
  }

  onBlur(event) {
    this.blur.emit();
  }

  onChipSelected(event) {
    this.expandForm();
    this.selectedChipItem = this.chips.getChipByIndex(event);
    this.formModel.forEach((row) => {
      const modelRow = row as DynamicFormGroupModel;
      modelRow.group.forEach((model: DynamicInputModel) => {
        const value = (this.selectedChipItem.item[model.name] === PLACEHOLDER_PARENT_METADATA
          || this.selectedChipItem.item[model.name].value === PLACEHOLDER_PARENT_METADATA)
          ? null
          : this.selectedChipItem.item[model.name];

        const nextValue = (this.formBuilderService.isInputModel(model) && isNotNull(value) && (typeof value !== 'string')) ?
          value.value : value;
        model.value = nextValue;

      });
    });

    this.editMode = true;
  }

  onFocus(event) {
    this.focus.emit(event);
  }

  collapseForm() {
    this.formCollapsed = observableOf(true);
    this.clear();
  }

  expandForm() {
    this.formCollapsed = observableOf(false);
  }

  clear() {
    if (this.editMode) {
      this.selectedChipItem.editMode = false;
      this.selectedChipItem = null;
      this.editMode = false;
    }
    this.resetForm();
    if (!this.model.isEmpty()) {
      this.formCollapsed = observableOf(true);
    }
  }

  save() {
    if (this.editMode) {
      this.modifyChip();
    } else {
      this.addToChips();
    }
  }

  delete() {
    this.chips.remove(this.selectedChipItem);
    this.clear();
  }

  ngOnDestroy(): void {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
  }

  private addToChips() {
    if (!this.formRef.formGroup.valid) {
      this.formService.validateAllFormFields(this.formRef.formGroup);
      return;
    }

    // Item to add
    if (!this.isMandatoryFieldEmpty()) {
      const item = this.buildChipItem();
      this.chips.add(item);

      this.resetForm();
    }
  }

  private modifyChip() {
    if (!this.formRef.formGroup.valid) {
      this.formService.validateAllFormFields(this.formRef.formGroup);
      return;
    }

    if (!this.isMandatoryFieldEmpty()) {
      const item = this.buildChipItem();
      this.chips.update(this.selectedChipItem.id, item);
      this.resetForm();
      this.cdr.detectChanges();
    }
  }

  private buildChipItem() {
    const item = Object.create({});
    this.formModel.forEach((row) => {
      const modelRow = row as DynamicFormGroupModel;
      modelRow.group.forEach((control: DynamicInputModel) => {
        item[control.name] = control.value || PLACEHOLDER_PARENT_METADATA;
      });
    });
    return item;
  }

  private initChipsFromModelValue() {

    let initChipsValue$: Observable<any[]>;
    if (this.model.isEmpty()) {
      this.initChips([]);
    } else {
      initChipsValue$ = observableOf(this.model.value as any[]);

      // If authority
      this.subs.push(initChipsValue$.pipe(
        mergeMap((valueModel) => {
          const returnList: Observable<any>[] = [];
          valueModel.forEach((valueObj) => {
            const returnObj = Object.keys(valueObj).map((fieldName) => {
              let return$: Observable<any>;
              if (isObject(valueObj[fieldName]) && valueObj[fieldName].hasAuthority() && isNotEmpty(valueObj[fieldName].authority)) {
                const fieldId = fieldName.replace(/\./g, '_');
                const model = this.formBuilderService.findById(fieldId, this.formModel);
                return$ = this.vocabularyService.findEntryDetailById(
                  valueObj[fieldName].authority,
                  (model as any).vocabularyOptions.name
                ).pipe(
                  getFirstSucceededRemoteDataPayload(),
                  map((entryDetail: VocabularyEntryDetail) => Object.assign(
                    new FormFieldMetadataValueObject(),
                    valueObj[fieldName],
                    {
                      otherInformation: entryDetail.otherInformation
                    })
                  ));
              } else {
                return$ = observableOf(valueObj[fieldName]);
              }
              return return$.pipe(map((entry) => ({ [fieldName]: entry })));
            });

            returnList.push(combineLatest(returnObj));
          });
          return returnList;
        }),
        mergeMap((valueListObj: Observable<any>, index: number) => {
          return valueListObj.pipe(
            map((valueObj: any) => ({
                index: index, value: valueObj.reduce(
                (acc: any, value: any) => Object.assign({}, acc, value)
                )
              })
            )
          );
        }),
        scan((acc: any[], valueObj: any) => {
          if (acc.length === 0) {
            acc.push(valueObj.value);
          } else {
            acc.splice(valueObj.index, 0, valueObj.value);
          }
          return acc;
        }, []),
        filter((modelValues: any[]) => (this.model.value as any[]).length === modelValues.length)
      ).subscribe((modelValue) => {
        this.model.value = modelValue;
        this.initChips(modelValue);
        this.cdr.markForCheck();
      }));
    }
  }

  private initChips(initChipsValue) {
    this.chips = new Chips(
      initChipsValue,
      'value',
      this.model.mandatoryField,
      environment.submission.icons.metadata);
    this.subs.push(
      this.chips.chipsItems
        .subscribe(() => {
          const items = this.chips.getChipsItems();
          // Does not emit change if model value is equal to the current value
          if (!isEqual(items, this.model.value)) {
            if (!(isEmpty(items) && this.model.isEmpty())) {
              this.model.value = items;
              this.change.emit();
            }
          }
        }),
    );
  }

  private resetForm() {
    if (this.formRef) {
      this.formService.resetForm(this.formRef.formGroup, this.formModel, this.formId);
    }
  }

}
