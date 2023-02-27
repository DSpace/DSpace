import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AdminSearchPageComponent } from './admin-search-page.component';
import { ItemAdminSearchResultListElementComponent } from './admin-search-results/admin-search-result-list-element/item-search-result/item-admin-search-result-list-element.component';
import { CommunityAdminSearchResultListElementComponent } from './admin-search-results/admin-search-result-list-element/community-search-result/community-admin-search-result-list-element.component';
import { CollectionAdminSearchResultListElementComponent } from './admin-search-results/admin-search-result-list-element/collection-search-result/collection-admin-search-result-list-element.component';
import { ItemAdminSearchResultGridElementComponent } from './admin-search-results/admin-search-result-grid-element/item-search-result/item-admin-search-result-grid-element.component';
import { CommunityAdminSearchResultGridElementComponent } from './admin-search-results/admin-search-result-grid-element/community-search-result/community-admin-search-result-grid-element.component';
import { CollectionAdminSearchResultGridElementComponent } from './admin-search-results/admin-search-result-grid-element/collection-search-result/collection-admin-search-result-grid-element.component';
import { ItemAdminSearchResultActionsComponent } from './admin-search-results/item-admin-search-result-actions.component';
import { JournalEntitiesModule } from '../../entity-groups/journal-entities/journal-entities.module';
import { ResearchEntitiesModule } from '../../entity-groups/research-entities/research-entities.module';
import { SearchModule } from '../../shared/search/search.module';

const ENTRY_COMPONENTS = [
  // put only entry components that use custom decorator
  ItemAdminSearchResultListElementComponent,
  CommunityAdminSearchResultListElementComponent,
  CollectionAdminSearchResultListElementComponent,
  ItemAdminSearchResultGridElementComponent,
  CommunityAdminSearchResultGridElementComponent,
  CollectionAdminSearchResultGridElementComponent,
  ItemAdminSearchResultActionsComponent
];

@NgModule({
  imports: [
    SearchModule,
    SharedModule.withEntryComponents(),
    JournalEntitiesModule.withEntryComponents(),
    ResearchEntitiesModule.withEntryComponents()
  ],
  declarations: [
    AdminSearchPageComponent,
    ...ENTRY_COMPONENTS
  ]
})
export class AdminSearchModule {
  /**
   * NOTE: this method allows to resolve issue with components that using a custom decorator
   * which are not loaded during SSR otherwise
   */
  static withEntryComponents() {
    return {
      ngModule: SharedModule,
      providers: ENTRY_COMPONENTS.map((component) => ({provide: component}))
    };
  }
}
