import { Component } from '@angular/core';
import { focusShadow } from '../../../../../shared/animations/focus';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { ItemSearchResultGridElementComponent } from '../../../../../shared/object-grid/search-result-grid-element/item-search-result/item/item-search-result-grid-element.component';

@listableObjectComponent('JournalSearchResult', ViewMode.GridElement)
@Component({
  selector: 'ds-journal-search-result-grid-element',
  styleUrls: ['./journal-search-result-grid-element.component.scss'],
  templateUrl: './journal-search-result-grid-element.component.html',
  animations: [focusShadow]
})
/**
 * The component for displaying a grid element for an item search result of the type Journal
 */
export class JournalSearchResultGridElementComponent extends ItemSearchResultGridElementComponent {
}
