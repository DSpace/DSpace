import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
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

@Component({
  selector: 'ds-metadata-schema-form',
  templateUrl: './metadata-schema-form.component.html'
})
/**
 * A form used for creating and editing metadata schemas
 */
export class MetadataSchemaFormComponent implements OnInit, OnDestroy {

  /**
   * A unique id used for ds-form
   */
  formId = 'metadata-schema-form';

  /**
   * The prefix for all messages related to this form
   */
  messagePrefix = 'admin.registries.metadata.form';

  /**
   * A dynamic input model for the name field
   */
  name: DynamicInputModel;

  /**
   * A dynamic input model for the namespace field
   */
  namespace: DynamicInputModel;

  /**
   * A list of all dynamic input models
   */
  formModel: DynamicFormControlModel[];

  /**
   * Layout used for structuring the form inputs
   */
  formLayout: DynamicFormLayout = {
    name: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
      }
    },
    namespace: {
      grid: {
        host: 'col col-sm-6 d-inline-block'
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

  constructor(public registryService: RegistryService, private formBuilderService: FormBuilderService, private translateService: TranslateService) {
  }

  ngOnInit() {
    combineLatest(
      this.translateService.get(`${this.messagePrefix}.name`),
      this.translateService.get(`${this.messagePrefix}.namespace`)
    ).subscribe(([name, namespace]) => {
      this.name = new DynamicInputModel({
          id: 'name',
          label: name,
          name: 'name',
          validators: {
            required: null,
            pattern: '^[^ ,_]{1,32}$'
          },
          required: true,
        });
      this.namespace = new DynamicInputModel({
          id: 'namespace',
          label: namespace,
          name: 'namespace',
          validators: {
            required: null,
          },
          required: true,
        });
      this.formModel = [
        new DynamicFormGroupModel(
          {
            id: 'metadatadataschemagroup',
            group:[this.namespace, this.name]
          })
      ];
      this.formGroup = this.formBuilderService.createFormGroup(this.formModel);
      this.registryService.getActiveMetadataSchema().subscribe((schema) => {
        this.formGroup.patchValue({
          metadatadataschemagroup:{
            name: schema != null ? schema.prefix : '',
            namespace: schema != null ? schema.namespace : ''
          }
        });
      });
    });
  }

  /**
   * Stop editing the currently selected metadata schema
   */
  onCancel() {
    this.registryService.cancelEditMetadataSchema();
  }

  /**
   * Submit the form
   * When the schema has an id attached -> Edit the schema
   * When the schema has no id attached -> Create new schema
   * Emit the updated/created schema using the EventEmitter submitForm
   */
  onSubmit() {
    this.registryService.clearMetadataSchemaRequests().subscribe();
    this.registryService.getActiveMetadataSchema().pipe(take(1)).subscribe(
      (schema) => {
        const values = {
          prefix: this.name.value,
          namespace: this.namespace.value
        };
        if (schema == null) {
          this.registryService.createOrUpdateMetadataSchema(Object.assign(new MetadataSchema(), values)).subscribe((newSchema) => {
            this.submitForm.emit(newSchema);
          });
        } else {
          this.registryService.createOrUpdateMetadataSchema(Object.assign(new MetadataSchema(), schema, {
            id: schema.id,
            prefix: (values.prefix ? values.prefix : schema.prefix),
            namespace: (values.namespace ? values.namespace : schema.namespace)
          })).subscribe((updatedSchema) => {
            this.submitForm.emit(updatedSchema);
          });
        }
        this.clearFields();
        this.registryService.cancelEditMetadataSchema();
      }
    );
  }

  /**
   * Reset all input-fields to be empty
   */
  clearFields() {
    this.formGroup.patchValue({
      metadatadataschemagroup:{
        prefix: '',
        namespace: ''
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
