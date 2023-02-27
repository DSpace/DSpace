import {Component, Input} from '@angular/core';
import {ItemOperation} from './itemOperation.model';

@Component({
  selector: 'ds-item-operation',
  templateUrl: './item-operation.component.html'
})
/**
 * Operation that can be performed on an item
 */
export class ItemOperationComponent {

  @Input() operation: ItemOperation;

}
