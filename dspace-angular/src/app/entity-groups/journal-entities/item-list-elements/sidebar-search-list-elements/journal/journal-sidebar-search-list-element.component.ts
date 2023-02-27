import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { Component } from '@angular/core';
import { SidebarSearchListElementComponent } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component';
import { Item } from '../../../../../core/shared/item.model';
import { isNotEmpty } from '../../../../../shared/empty.util';

@listableObjectComponent('JournalSearchResult', ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent('JournalSearchResult', ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@Component({
  selector: 'ds-journal-sidebar-search-list-element',
  templateUrl: '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link ItemSearchResult} of type "Journal" within the context of
 * a sidebar search modal
 */
export class JournalSidebarSearchListElementComponent extends SidebarSearchListElementComponent<ItemSearchResult, Item> {
  /**
   * Get the description of the Journal by returning its ISSN(s)
   */
  getDescription(): string {
    const issns = this.allMetadataValues(['creativeworkseries.issn']);
    let description = '';
    if (isNotEmpty(issns)) {
      description += issns.join(', ');
    }
    return this.undefinedIfEmpty(description);
  }
}
