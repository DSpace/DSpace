import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CoreModule } from '../core/core.module';
import { SharedModule } from '../shared/shared.module';
import { SidebarService } from '../shared/sidebar/sidebar.service';
import { ConfigurationSearchPageGuard } from './configuration-search-page.guard';
import { SearchTrackerComponent } from './search-tracker.component';
import { StatisticsModule } from '../statistics/statistics.module';
import { SearchPageComponent } from './search-page.component';
import { SearchFilterService } from '../core/shared/search/search-filter.service';
import { SearchConfigurationService } from '../core/shared/search/search-configuration.service';
import { JournalEntitiesModule } from '../entity-groups/journal-entities/journal-entities.module';
import { ResearchEntitiesModule } from '../entity-groups/research-entities/research-entities.module';
import { ThemedSearchPageComponent } from './themed-search-page.component';
import { SearchModule } from '../shared/search/search.module';

const components = [
  SearchPageComponent,
  SearchTrackerComponent,
  ThemedSearchPageComponent
];

@NgModule({
  imports: [
    CommonModule,
    SearchModule,
    SharedModule.withEntryComponents(),
    CoreModule.forRoot(),
    StatisticsModule.forRoot(),
    JournalEntitiesModule.withEntryComponents(),
    ResearchEntitiesModule.withEntryComponents()
  ],
  declarations: components,
  providers: [
    SidebarService,
    SearchFilterService,
    ConfigurationSearchPageGuard,
    SearchConfigurationService
  ],
  exports: components
})

/**
 * This module handles all components and pipes that are necessary for the search page
 */
export class SearchPageModule {
}
