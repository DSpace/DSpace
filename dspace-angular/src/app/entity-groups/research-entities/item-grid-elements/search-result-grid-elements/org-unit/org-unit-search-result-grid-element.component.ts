import { Component } from '@angular/core';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { focusShadow } from '../../../../../shared/animations/focus';
import { ItemSearchResultGridElementComponent } from '../../../../../shared/object-grid/search-result-grid-element/item-search-result/item/item-search-result-grid-element.component';

@listableObjectComponent('OrgUnitSearchResult', ViewMode.GridElement)
@Component({
  selector: 'ds-org-unit-search-result-grid-element',
  styleUrls: ['./org-unit-search-result-grid-element.component.scss'],
  templateUrl: './org-unit-search-result-grid-element.component.html',
  animations: [focusShadow]
})
/**
 * The component for displaying a grid element for an item search result of the type Organisation Unit
 */
export class OrgUnitSearchResultGridElementComponent extends ItemSearchResultGridElementComponent {
}
