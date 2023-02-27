import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Component which represent a DSpace dropdown selector.
 */
@Component({
  selector: 'ds-select',
  templateUrl: './ds-select.component.html',
  styleUrls: ['./ds-select.component.scss']
})
export class DsSelectComponent {

  /**
   * An optional label for the dropdown selector.
   */
  @Input()
  label: string;

  /**
   * Whether the dropdown selector is disabled.
   */
  @Input()
  disabled: boolean;

  /**
   * Emits an event when the dropdown selector is opened or closed.
   */
  @Output()
  toggled = new EventEmitter();

  /**
   * Emits an event when the dropdown selector or closed.
   */
  @Output()
  close = new EventEmitter();
}
