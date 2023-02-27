import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Abstract class that represents value input components
 */
@Component({
  selector: 'ds-value-input',
  template: ''
})
export abstract class ValueInputComponent<T> {
  @Input() index: number;
  /**
   * Used by the subclasses to emit the value when it's updated
   */
  @Output() updateValue: EventEmitter<T> = new EventEmitter<T>();
}
