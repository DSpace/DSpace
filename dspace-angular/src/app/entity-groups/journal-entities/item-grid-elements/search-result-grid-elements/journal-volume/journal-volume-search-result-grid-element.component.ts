import { Component } from '@angular/core';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { focusShadow } from '../../../../../shared/animations/focus';
import { ItemSearchResultGridElementComponent } from '../../../../../shared/object-grid/search-result-grid-element/item-search-result/item/item-search-result-grid-element.component';

@listableObjectComponent('JournalVolumeSearchResult', ViewMode.GridElement)
@Component({
  selector: 'ds-journal-volume-search-result-grid-element',
  styleUrls: ['./journal-volume-search-result-grid-element.component.scss'],
  templateUrl: './journal-volume-search-result-grid-element.component.html',
  animations: [focusShadow]
})
/**
 * The component for displaying a grid element for an item search result of the type Journal Volume
 */
export class JournalVolumeSearchResultGridElementComponent extends ItemSearchResultGridElementComponent {
}
