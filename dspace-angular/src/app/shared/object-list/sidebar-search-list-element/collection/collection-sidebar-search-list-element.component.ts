import { Component } from '@angular/core';
import { CollectionSearchResult } from '../../../object-collection/shared/collection-search-result.model';
import { Collection } from '../../../../core/shared/collection.model';
import { listableObjectComponent } from '../../../object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../core/shared/context.model';
import { ViewMode } from '../../../../core/shared/view-mode.model';
import { SidebarSearchListElementComponent } from '../sidebar-search-list-element.component';

@listableObjectComponent(CollectionSearchResult, ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent(CollectionSearchResult, ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@Component({
  selector: 'ds-collection-sidebar-search-list-element',
  templateUrl: '../sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link CollectionSearchResult} within the context of a sidebar search modal
 */
export class CollectionSidebarSearchListElementComponent extends SidebarSearchListElementComponent<CollectionSearchResult, Collection> {
  /**
   * Get the description of the Collection by returning its abstract
   */
  getDescription(): string {
    return this.firstMetadataValue('dc.description.abstract');
  }
}
