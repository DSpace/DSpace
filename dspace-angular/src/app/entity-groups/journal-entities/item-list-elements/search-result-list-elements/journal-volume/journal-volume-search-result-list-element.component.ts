import { Component } from '@angular/core';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { ItemSearchResultListElementComponent } from '../../../../../shared/object-list/search-result-list-element/item-search-result/item-types/item/item-search-result-list-element.component';

@listableObjectComponent('JournalVolumeSearchResult', ViewMode.ListElement)
@Component({
  selector: 'ds-journal-volume-search-result-list-element',
  styleUrls: ['./journal-volume-search-result-list-element.component.scss'],
  templateUrl: './journal-volume-search-result-list-element.component.html'
})
/**
 * The component for displaying a list element for an item search result of the type Journal Volume
 */
export class JournalVolumeSearchResultListElementComponent extends ItemSearchResultListElementComponent {

  /**
   * Display thumbnails if required by configuration
   */
  showThumbnails: boolean;

  ngOnInit(): void {
    super.ngOnInit();
    this.showThumbnails = this.appConfig.browseBy.showThumbnails;
  }

}
