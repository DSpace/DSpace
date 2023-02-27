import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { Component } from '@angular/core';
import { SidebarSearchListElementComponent } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component';
import { Item } from '../../../../../core/shared/item.model';
import { isNotEmpty } from '../../../../../shared/empty.util';

@listableObjectComponent('JournalIssueSearchResult', ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent('JournalIssueSearchResult', ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@Component({
  selector: 'ds-journal-issue-sidebar-search-list-element',
  templateUrl: '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link ItemSearchResult} of type "JournalIssue" within the context of
 * a sidebar search modal
 */
export class JournalIssueSidebarSearchListElementComponent extends SidebarSearchListElementComponent<ItemSearchResult, Item> {
  /**
   * Get the description of the Journal Issue by returning its volume number(s) and/or issue number(s)
   */
  getDescription(): string {
    const volumeNumbers = this.allMetadataValues(['publicationvolume.volumeNumber']);
    const issueNumbers = this.allMetadataValues(['publicationissue.issueNumber']);
    let description = '';
    if (isNotEmpty(volumeNumbers)) {
      description += volumeNumbers.join(', ');
    }
    if (isNotEmpty(description) && isNotEmpty(issueNumbers)) {
      description += ' - ';
    }
    if (isNotEmpty(issueNumbers)) {
      description += issueNumbers.join(', ');
    }
    return this.undefinedIfEmpty(description);
  }
}
