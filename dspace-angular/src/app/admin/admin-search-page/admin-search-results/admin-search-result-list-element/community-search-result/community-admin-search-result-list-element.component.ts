import { Component } from '@angular/core';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { Context } from '../../../../../core/shared/context.model';
import { SearchResultListElementComponent } from '../../../../../shared/object-list/search-result-list-element/search-result-list-element.component';
import { CommunitySearchResult } from '../../../../../shared/object-collection/shared/community-search-result.model';
import { Community } from '../../../../../core/shared/community.model';
import { getCommunityEditRoute } from '../../../../../community-page/community-page-routing-paths';

@listableObjectComponent(CommunitySearchResult, ViewMode.ListElement, Context.AdminSearch)
@Component({
  selector: 'ds-community-admin-search-result-list-element',
  styleUrls: ['./community-admin-search-result-list-element.component.scss'],
  templateUrl: './community-admin-search-result-list-element.component.html'
})
/**
 * The component for displaying a list element for a community search result on the admin search page
 */
export class CommunityAdminSearchResultListElementComponent extends SearchResultListElementComponent<CommunitySearchResult, Community> {
  editPath: string;

  ngOnInit() {
    super.ngOnInit();
    this.editPath = getCommunityEditRoute(this.dso.uuid);
  }
}
