import { Component, Optional, Input } from '@angular/core';
import { ValueInputComponent } from '../value-input.component';
import { ControlContainer, NgForm } from '@angular/forms';
import { controlContainerFactory } from '../../../process-form.component';

/**
 * Represents the user inputted value of a string parameter
 */
@Component({
  selector: 'ds-string-value-input',
  templateUrl: './string-value-input.component.html',
  styleUrls: ['./string-value-input.component.scss'],
  viewProviders: [ { provide: ControlContainer,
    useFactory: controlContainerFactory,
    deps: [[new Optional(), NgForm]] } ]
})
export class StringValueInputComponent extends ValueInputComponent<string> {
  /**
   * The current value of the string
   */
  value: string;

  /**
   * Initial value of the field
   */
  @Input() initialValue;

  ngOnInit() {
    this.value = this.initialValue;
  }

  setValue(value) {
    this.value = value;
    this.updateValue.emit(value);
  }
}
