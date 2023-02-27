import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import {
  DynamicFormControlModel,
  DynamicFormGroupModel,
  DynamicFormLayout,
  DynamicInputModel
} from '@ng-dynamic-forms/core';
import { FormGroup } from '@angular/forms';
import { RegistryService } from '../../../../core/registry/registry.service';
import { FormBuilderService } from '../../../../shared/form/builder/form-builder.service';
import { take } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { combineLatest } from 'rxjs';
import { MetadataSchema } from '../../../../core/metadata/metadata-schema.model';
import { MetadataField } from '../../../../core/metadata/metadata-field.model';

@Component({
  selector: 'ds-metadata-field-form',
  templateUrl: './metadata-field-form.component.html'
})
/**
 * A form used for creating and editing metadata fields
 */
export class MetadataFieldFormComponent implements OnInit, OnDestroy {

  /**
   * A unique id used for ds-form
   */
  formId = 'metadata-field-form';

  /**
   * The prefix for all messages related to this form
   */
  messagePrefix = 'admin.registries.schema.form';

  /**
   * The metadata schema this field is attached to
   */
  @Input() metadataSchema: MetadataSchema;

  /**
   * A dynamic input model for the element field
   */
  element: DynamicInputModel;

  /**
   * A dynamic input model for the qualifier field
   */
  qualifier: DynamicInputModel;

  /**
   * A dynamic input model for the scopeNote field
   */
  scopeNote: DynamicInputModel;

  /**
   * A list of all dynamic input models
   */
  formModel: DynamicFormControlModel[];

  /**
   * Layout used for structuring the form inputs
   */
  formLayout: DynamicFormLayout = {
    element: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
      }
    },
    qualifier: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
      }
    },
    scopeNote: {
      grid: {
        host: 'col col-sm-12 d-inline-block'
      }
    }
  };

  /**
   * A FormGroup that combines all inputs
   */
  formGroup: FormGroup;

  /**
   * An EventEmitter that's fired whenever the form is being submitted
   */
  @Output() submitForm: EventEmitter<any> = new EventEmitter();

  constructor(public registryService: RegistryService,
              private formBuilderService: FormBuilderService,
              private translateService: TranslateService) {
  }

  /**
   * Initialize the component, setting up the necessary Models for the dynamic form
   */
  ngOnInit() {
    combineLatest(
      this.translateService.get(`${this.messagePrefix}.element`),
      this.translateService.get(`${this.messagePrefix}.qualifier`),
      this.translateService.get(`${this.messagePrefix}.scopenote`)
    ).subscribe(([element, qualifier, scopenote]) => {
      this.element = new DynamicInputModel({
        id: 'element',
        label: element,
        name: 'element',
        validators: {
          required: null,
        },
        required: true,
      });
      this.qualifier = new DynamicInputModel({
        id: 'qualifier',
        label: qualifier,
        name: 'qualifier',
        required: false,
      });
      this.scopeNote = new DynamicInputModel({
        id: 'scopeNote',
        label: scopenote,
        name: 'scopeNote',
        required: false,
      });
      this.formModel = [
        new DynamicFormGroupModel(
        {
          id: 'metadatadatafieldgroup',
          group:[this.element, this.qualifier, this.scopeNote]
        })
      ];
      this.formGroup = this.formBuilderService.createFormGroup(this.formModel);
      this.registryService.getActiveMetadataField().subscribe((field) => {
        this.formGroup.patchValue({
          metadatadatafieldgroup: {
            element: field != null ? field.element : '',
            qualifier: field != null ? field.qualifier : '',
            scopeNote: field != null ? field.scopeNote : ''
          }
        });
      });
    });
  }

  /**
   * Stop editing the currently selected metadata field
   */
  onCancel() {
    this.registryService.cancelEditMetadataField();
  }

  /**
   * Submit the form
   * When the field has an id attached -> Edit the field
   * When the field has no id attached -> Create new field
   * Emit the updated/created field using the EventEmitter submitForm
   */
  onSubmit() {
    this.registryService.getActiveMetadataField().pipe(take(1)).subscribe(
      (field) => {
        const values = {
          element: this.element.value,
          qualifier: this.qualifier.value,
          scopeNote: this.scopeNote.value
        };
        if (field == null) {
          this.registryService.createMetadataField(Object.assign(new MetadataField(), values), this.metadataSchema).subscribe((newField) => {
            this.submitForm.emit(newField);
          });
        } else {
          this.registryService.updateMetadataField(Object.assign(new MetadataField(), field, {
            id: field.id,
            element: (values.element ? values.element : field.element),
            qualifier: (values.qualifier ? values.qualifier : field.qualifier),
            scopeNote: (values.scopeNote ? values.scopeNote : field.scopeNote)
          })).subscribe((updatedField) => {
            this.submitForm.emit(updatedField);
          });
        }
        this.clearFields();
        this.registryService.cancelEditMetadataField();
      }
    );
  }

  /**
   * Reset all input-fields to be empty
   */
  clearFields() {
    this.formGroup.patchValue({
      metadatadatafieldgroup: {
        element: '',
        qualifier: '',
        scopeNote: ''
      }
    });
  }

  /**
   * Cancel the current edit when component is destroyed
   */
  ngOnDestroy(): void {
    this.onCancel();
  }
}
