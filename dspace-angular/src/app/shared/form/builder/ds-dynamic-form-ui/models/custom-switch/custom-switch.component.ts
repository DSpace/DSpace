import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormGroup } from '@angular/forms';

import { DynamicNGBootstrapCheckboxComponent } from '@ng-dynamic-forms/ui-ng-bootstrap';
import { DynamicFormLayoutService, DynamicFormValidationService } from '@ng-dynamic-forms/core';

import { DynamicCustomSwitchModel } from './custom-switch.model';

@Component({
  selector: 'ds-custom-switch',
  styleUrls: ['./custom-switch.component.scss'],
  templateUrl: './custom-switch.component.html',
})
/**
 * Component displaying a custom switch usable in dynamic forms
 * Extends from bootstrap's checkbox component but displays a switch instead
 */
export class CustomSwitchComponent extends DynamicNGBootstrapCheckboxComponent {
  /**
   * Use the model's ID for the input element
   */
  @Input() bindId = true;

  /**
   * The formgroup containing this component
   */
  @Input() group: FormGroup;

  /**
   * The model used for displaying the switch
   */
  @Input() model: DynamicCustomSwitchModel;

  /**
   * Emit an event when the input is selected
   */
  @Output() selected = new EventEmitter<number>();

  /**
   * Emit an event when the input value is removed
   */
  @Output() remove = new EventEmitter<number>();

  /**
   * Emit an event when the input is blurred out
   */
  @Output() blur = new EventEmitter<any>();

  /**
   * Emit an event when the input value changes
   */
  @Output() change = new EventEmitter<any>();

  /**
   * Emit an event when the input is focused
   */
  @Output() focus = new EventEmitter<any>();

  constructor(layoutService: DynamicFormLayoutService, validationService: DynamicFormValidationService) {
    super(layoutService, validationService);
  }
}
