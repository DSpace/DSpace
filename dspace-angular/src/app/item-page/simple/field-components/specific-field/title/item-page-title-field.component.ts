import { Component, Input } from '@angular/core';

import { Item } from '../../../../../core/shared/item.model';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';

@Component({
  selector: 'ds-item-page-title-field',
  templateUrl: './item-page-title-field.component.html'
})
/**
 * This component is used for displaying the title (defined by the {@link DSONameService}) of an item
 */
export class ItemPageTitleFieldComponent {

  /**
   * The item to display metadata for
   */
  @Input() item: Item;

  constructor(
    public dsoNameService: DSONameService,
  ) {
  }

}
