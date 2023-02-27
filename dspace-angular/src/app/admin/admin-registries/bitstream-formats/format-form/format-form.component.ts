import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BitstreamFormat } from '../../../../core/shared/bitstream-format.model';
import { BitstreamFormatSupportLevel } from '../../../../core/shared/bitstream-format-support-level';
import {
  DynamicCheckboxModel,
  DynamicFormArrayModel,
  DynamicFormControlLayout,
  DynamicFormControlModel,
  DynamicFormService,
  DynamicInputModel,
  DynamicSelectModel,
  DynamicTextAreaModel
} from '@ng-dynamic-forms/core';
import { Router } from '@angular/router';
import { hasValue, isEmpty } from '../../../../shared/empty.util';
import { TranslateService } from '@ngx-translate/core';
import { getBitstreamFormatsModuleRoute } from '../../admin-registries-routing-paths';
import { environment } from '../../../../../environments/environment';

/**
 * The component responsible for rendering the form to create/edit a bitstream format
 */
@Component({
  selector: 'ds-bitstream-format-form',
  templateUrl: './format-form.component.html'
})
export class FormatFormComponent implements OnInit {

  /**
   * The current bitstream format
   * This can either be and existing one or a new one
   */
  @Input() bitstreamFormat: BitstreamFormat = new BitstreamFormat();

  /**
   * EventEmitter that will emit the updated bitstream format
   */
  @Output() updatedFormat: EventEmitter<BitstreamFormat> = new EventEmitter<BitstreamFormat>();

  /**
   * The different supported support level of the bitstream format
   */
  supportLevelOptions = [{label: BitstreamFormatSupportLevel.Known, value: BitstreamFormatSupportLevel.Known},
    {label: BitstreamFormatSupportLevel.Unknown, value: BitstreamFormatSupportLevel.Unknown},
    {label: BitstreamFormatSupportLevel.Supported, value: BitstreamFormatSupportLevel.Supported}];

  /**
   * Styling element for repeatable field
   */
  arrayElementLayout: DynamicFormControlLayout = {
    grid: {
      group: 'form-row',
    },
  };

  /**
   * Styling element for element of repeatable field
   */
  arrayInputElementLayout: DynamicFormControlLayout = {
    grid: {
      host: 'col'
    }
  };

  /**
   * The form model representing the bitstream format
   */
  formModel: DynamicFormControlModel[] = [
    new DynamicInputModel({
      id: 'shortDescription',
      name: 'shortDescription',
      label: 'admin.registries.bitstream-formats.edit.shortDescription.label',
      hint: 'admin.registries.bitstream-formats.edit.shortDescription.hint',
      required: true,
      validators: {
        required: null
      },
      errorMessages: {
        required: 'Please enter a name for this bitstream format'
      },
    }),
    new DynamicInputModel({
      id: 'mimetype',
      name: 'mimetype',
      label: 'admin.registries.bitstream-formats.edit.mimetype.label',
      hint: 'admin.registries.bitstream-formats.edit.mimetype.hint',

    }),
    new DynamicTextAreaModel({
      id: 'description',
      name: 'description',
      label: 'admin.registries.bitstream-formats.edit.description.label',
      hint: 'admin.registries.bitstream-formats.edit.description.hint',
      spellCheck: environment.form.spellCheck,

    }),
    new DynamicSelectModel({
      id: 'supportLevel',
      name: 'supportLevel',
      options: this.supportLevelOptions,
      label: 'admin.registries.bitstream-formats.edit.supportLevel.label',
      hint: 'admin.registries.bitstream-formats.edit.supportLevel.hint',
      value: this.supportLevelOptions[0].value

    }),
    new DynamicCheckboxModel({
      id: 'internal',
      name: 'internal',
      label: 'Internal',
      hint: 'admin.registries.bitstream-formats.edit.internal.hint',
    }),
    new DynamicFormArrayModel({
      id: 'extensions',
      name: 'extensions',
      label: 'admin.registries.bitstream-formats.edit.extensions.label',
      groupFactory: () => [
        new DynamicInputModel({
          id: 'extension',
          placeholder: 'admin.registries.bitstream-formats.edit.extensions.placeholder',
        }, this.arrayInputElementLayout)
      ]
    }, this.arrayElementLayout),
  ];

  constructor(private dynamicFormService: DynamicFormService,
              private translateService: TranslateService,
              private router: Router) {

  }

  ngOnInit(): void {

    this.initValues();
  }

  /**
   * Initializes the form based on the provided bitstream format
   */
  initValues() {
    this.formModel.forEach(
      (fieldModel: DynamicFormControlModel) => {
        if (fieldModel.name === 'extensions') {
          if (hasValue(this.bitstreamFormat.extensions)) {
            const extenstions = this.bitstreamFormat.extensions;
            const formArray = (fieldModel as DynamicFormArrayModel);
            for (let i = 0; i < extenstions.length; i++) {
              formArray.insertGroup(i).group[0] = new DynamicInputModel({
                id: `extension-${i}`,
                value: extenstions[i]
              }, this.arrayInputElementLayout);
            }
          }
        } else {
          if (hasValue(this.bitstreamFormat[fieldModel.name])) {
            (fieldModel as DynamicInputModel).value = this.bitstreamFormat[fieldModel.name];
          }
        }
      });
  }

  /**
   * Creates an updated bistream format based on the current values in the form
   * Emits the updated bitstream format trouhg the updatedFormat emitter
   */
  onSubmit() {
    const updatedBitstreamFormat = Object.assign(new BitstreamFormat(),
      {
        id: this.bitstreamFormat.id
      });

    this.formModel.forEach(
      (fieldModel: DynamicFormControlModel) => {
        if (fieldModel.name === 'extensions') {
          const formArray = (fieldModel as DynamicFormArrayModel);
          const extensions = [];
          for (let i = 0; i < formArray.groups.length; i++) {
            const value = (formArray.get(i).get(0) as DynamicInputModel).value;
            if (!isEmpty(value)) {
              extensions.push((formArray.get(i).get(0) as DynamicInputModel).value);
            }
          }
          updatedBitstreamFormat.extensions = extensions;
        } else {
          updatedBitstreamFormat[fieldModel.name] = (fieldModel as DynamicInputModel).value;
        }
      });
    this.updatedFormat.emit(updatedBitstreamFormat);
  }

  /**
   * Cancels the edit/create action of the bitstream format and navigates back to the bitstream format registry
   */
  onCancel() {
    this.router.navigate([getBitstreamFormatsModuleRoute()]);
  }
}
