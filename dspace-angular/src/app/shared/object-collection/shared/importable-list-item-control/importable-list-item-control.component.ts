import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ListableObject } from '../listable-object.model';

@Component({
  selector: 'ds-importable-list-item-control',
  templateUrl: './importable-list-item-control.component.html'
})
/**
 * Component adding an import button to a list item
 */
export class ImportableListItemControlComponent {
  /**
   * The item or metadata to determine the component for
   */
  @Input() object: ListableObject;

  /**
   * Extra configuration for the import button
   */
  @Input() importConfig: { buttonLabel: string };

  /**
   * Output the object to import
   */
  @Output() importObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();
}
