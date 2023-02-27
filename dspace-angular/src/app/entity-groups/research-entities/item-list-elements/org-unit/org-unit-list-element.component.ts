import { Component } from '@angular/core';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { AbstractListableElementComponent } from '../../../../shared/object-collection/shared/object-collection-element/abstract-listable-element.component';
import { Item } from '../../../../core/shared/item.model';

@listableObjectComponent('OrgUnit', ViewMode.ListElement)
@Component({
  selector: 'ds-org-unit-list-element',
  styleUrls: ['./org-unit-list-element.component.scss'],
  templateUrl: './org-unit-list-element.component.html'
})
/**
 * The component for displaying a list element for an item of the type Organisation Unit
 */
export class OrgUnitListElementComponent extends AbstractListableElementComponent<Item> {
}
