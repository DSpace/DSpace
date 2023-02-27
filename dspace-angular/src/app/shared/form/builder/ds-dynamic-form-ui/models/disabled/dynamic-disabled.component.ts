import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { DynamicFormControlComponent, DynamicFormLayoutService, DynamicFormValidationService } from '@ng-dynamic-forms/core';

import { DynamicDisabledModel } from './dynamic-disabled.model';

/**
 * Component representing a simple disabled input field
 */
@Component({
  selector: 'ds-dynamic-disabled',
  templateUrl: './dynamic-disabled.component.html'
})
/**
 * Component for displaying a form input with a disabled property
 */
export class DsDynamicDisabledComponent extends DynamicFormControlComponent {

  @Input() formId: string;
  @Input() group: FormGroup;
  @Input() model: DynamicDisabledModel;
  modelValuesString = '';

  @Output() blur: EventEmitter<any> = new EventEmitter<any>();
  @Output() change: EventEmitter<any> = new EventEmitter<any>();
  @Output() focus: EventEmitter<any> = new EventEmitter<any>();

  constructor(protected layoutService: DynamicFormLayoutService,
              protected validationService: DynamicFormValidationService
  ) {
    super(layoutService, validationService);
  }
}
