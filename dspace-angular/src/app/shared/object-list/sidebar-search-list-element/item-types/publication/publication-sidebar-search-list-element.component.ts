import { listableObjectComponent } from '../../../../object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { Component } from '@angular/core';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../object-collection/shared/item-search-result.model';
import { Item } from '../../../../../core/shared/item.model';
import { SidebarSearchListElementComponent } from '../../sidebar-search-list-element.component';

@listableObjectComponent('PublicationSearchResult', ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent('PublicationSearchResult', ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@listableObjectComponent(ItemSearchResult, ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent(ItemSearchResult, ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@Component({
  selector: 'ds-publication-sidebar-search-list-element',
  templateUrl: '../../sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link ItemSearchResult} of type "Publication" within the context of
 * a sidebar search modal
 */
export class PublicationSidebarSearchListElementComponent extends SidebarSearchListElementComponent<ItemSearchResult, Item> {

}
