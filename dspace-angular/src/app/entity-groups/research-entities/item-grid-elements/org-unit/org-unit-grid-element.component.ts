import { Component } from '@angular/core';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { AbstractListableElementComponent } from '../../../../shared/object-collection/shared/object-collection-element/abstract-listable-element.component';
import { Item } from '../../../../core/shared/item.model';

@listableObjectComponent('OrgUnit', ViewMode.GridElement)
@Component({
  selector: 'ds-org-unit-grid-element',
  styleUrls: ['./org-unit-grid-element.component.scss'],
  templateUrl: './org-unit-grid-element.component.html',
})
/**
 * The component for displaying a grid element for an item of the type Organisation Unit
 */
export class OrgUnitGridElementComponent extends AbstractListableElementComponent<Item> {
}
