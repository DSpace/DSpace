import { Component } from '@angular/core';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { AbstractListableElementComponent } from '../../../../shared/object-collection/shared/object-collection-element/abstract-listable-element.component';
import { Item } from '../../../../core/shared/item.model';

@listableObjectComponent('Person', ViewMode.GridElement)
@Component({
  selector: 'ds-person-grid-element',
  styleUrls: ['./person-grid-element.component.scss'],
  templateUrl: './person-grid-element.component.html',
})
/**
 * The component for displaying a grid element for an item of the type Person
 */
export class PersonGridElementComponent extends AbstractListableElementComponent<Item> {
}
