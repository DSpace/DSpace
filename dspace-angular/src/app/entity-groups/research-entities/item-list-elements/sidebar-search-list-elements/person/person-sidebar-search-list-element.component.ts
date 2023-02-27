import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { Context } from '../../../../../core/shared/context.model';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { Component } from '@angular/core';
import { SidebarSearchListElementComponent } from '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component';
import { Item } from '../../../../../core/shared/item.model';
import { isNotEmpty } from '../../../../../shared/empty.util';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { LinkService } from '../../../../../core/cache/builders/link.service';
import { TranslateService } from '@ngx-translate/core';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';

@listableObjectComponent('PersonSearchResult', ViewMode.ListElement, Context.SideBarSearchModal)
@listableObjectComponent('PersonSearchResult', ViewMode.ListElement, Context.SideBarSearchModalCurrent)
@Component({
  selector: 'ds-person-sidebar-search-list-element',
  templateUrl: '../../../../../shared/object-list/sidebar-search-list-element/sidebar-search-list-element.component.html'
})
/**
 * Component displaying a list element for a {@link ItemSearchResult} of type "Person" within the context of
 * a sidebar search modal
 */
export class PersonSidebarSearchListElementComponent extends SidebarSearchListElementComponent<ItemSearchResult, Item> {
  constructor(protected truncatableService: TruncatableService,
              protected linkService: LinkService,
              protected translateService: TranslateService,
              protected dsoNameService: DSONameService
  ) {
    super(truncatableService, linkService, dsoNameService);
  }

  /**
   * Get the description of the Person by returning its job title(s)
   */
  getDescription(): string {
    const titles = this.allMetadataValues(['person.jobTitle']);
    let description = '';
    if (isNotEmpty(titles)) {
      description += titles.join(', ');
    }
    return this.undefinedIfEmpty(description);
  }
}
