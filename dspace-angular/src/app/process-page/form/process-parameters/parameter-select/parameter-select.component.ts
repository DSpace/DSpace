import { Component, EventEmitter, Input, Optional, Output } from '@angular/core';
import { ProcessParameter } from '../../../processes/process-parameter.model';
import { ScriptParameter } from '../../../scripts/script-parameter.model';
import { ControlContainer, NgForm } from '@angular/forms';
import { controlContainerFactory } from '../../process-form.component';

/**
 * Component to select a single parameter for a process
 */
@Component({
  selector: 'ds-parameter-select',
  templateUrl: './parameter-select.component.html',
  styleUrls: ['./parameter-select.component.scss'],
  viewProviders: [{
    provide: ControlContainer,
    useFactory: controlContainerFactory,
    deps: [[new Optional(), NgForm]]
  }]
})
export class ParameterSelectComponent {
  @Input() index: number;

  /**
   * The current parameter value of the selected parameter
   */
  @Input() parameterValue: ProcessParameter = new ProcessParameter();

  /**
   * The available script parameters for the script
   */
  @Input() parameters: ScriptParameter[];

  /**
   * Whether or not this selected parameter can be removed from the list
   */
  @Input() removable: boolean;

  /**
   * Emits the parameter value when it's removed
   */
  @Output() removeParameter: EventEmitter<ProcessParameter> = new EventEmitter<ProcessParameter>();

  /**
   * Emits the updated parameter value when it changes
   */
  @Output() changeParameter: EventEmitter<ProcessParameter> = new EventEmitter<ProcessParameter>();

  /**
   * Returns the script parameter based on the currently selected name
   */
  get selectedScriptParameter(): ScriptParameter {
    return this.parameters.find((parameter: ScriptParameter) => parameter.name === this.selectedParameter);
  }

  /**
   * Return the currently selected parameter name
   */
  get selectedParameter(): string {
    return this.parameterValue ? this.parameterValue.name : undefined;
  }

  /**
   * Sets the currently selected parameter based on the provided parameter name
   * Emits the new value from the changeParameter output
   * @param value The parameter name to set
   */
  set selectedParameter(value: string) {
    this.parameterValue.name = value;
    this.selectedParameterValue = undefined;
    this.changeParameter.emit(this.parameterValue);
  }

  /**
   * Returns the currently selected parameter value
   */
  get selectedParameterValue(): any {
    return this.parameterValue ? this.parameterValue.value : undefined;
  }

  /**
   * Sets the currently selected value for the parameter
   * Emits the new value from the changeParameter output
   * @param value The parameter value to set
   */
  set selectedParameterValue(value: any) {
    this.parameterValue.value = value;
    this.changeParameter.emit(this.parameterValue);
  }
}
