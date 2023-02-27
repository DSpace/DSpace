import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../../shared/shared.module';
import { OrgUnitComponent } from './item-pages/org-unit/org-unit.component';
import { PersonComponent } from './item-pages/person/person.component';
import { ProjectComponent } from './item-pages/project/project.component';
import { OrgUnitListElementComponent } from './item-list-elements/org-unit/org-unit-list-element.component';
import { PersonListElementComponent } from './item-list-elements/person/person-list-element.component';
import { ProjectListElementComponent } from './item-list-elements/project/project-list-element.component';
import { PersonGridElementComponent } from './item-grid-elements/person/person-grid-element.component';
import { OrgUnitGridElementComponent } from './item-grid-elements/org-unit/org-unit-grid-element.component';
import { ProjectGridElementComponent } from './item-grid-elements/project/project-grid-element.component';
import { OrgUnitSearchResultListElementComponent } from './item-list-elements/search-result-list-elements/org-unit/org-unit-search-result-list-element.component';
import { PersonSearchResultListElementComponent } from './item-list-elements/search-result-list-elements/person/person-search-result-list-element.component';
import { ProjectSearchResultListElementComponent } from './item-list-elements/search-result-list-elements/project/project-search-result-list-element.component';
import { PersonSearchResultGridElementComponent } from './item-grid-elements/search-result-grid-elements/person/person-search-result-grid-element.component';
import { OrgUnitSearchResultGridElementComponent } from './item-grid-elements/search-result-grid-elements/org-unit/org-unit-search-result-grid-element.component';
import { ProjectSearchResultGridElementComponent } from './item-grid-elements/search-result-grid-elements/project/project-search-result-grid-element.component';
import { PersonItemMetadataListElementComponent } from './metadata-representations/person/person-item-metadata-list-element.component';
import { OrgUnitItemMetadataListElementComponent } from './metadata-representations/org-unit/org-unit-item-metadata-list-element.component';
import { PersonSearchResultListSubmissionElementComponent } from './submission/item-list-elements/person/person-search-result-list-submission-element.component';
import { PersonInputSuggestionsComponent } from './submission/item-list-elements/person/person-suggestions/person-input-suggestions.component';
import { NameVariantModalComponent } from './submission/name-variant-modal/name-variant-modal.component';
import { OrgUnitInputSuggestionsComponent } from './submission/item-list-elements/org-unit/org-unit-suggestions/org-unit-input-suggestions.component';
import { OrgUnitSearchResultListSubmissionElementComponent } from './submission/item-list-elements/org-unit/org-unit-search-result-list-submission-element.component';
import { ExternalSourceEntryListSubmissionElementComponent } from './submission/item-list-elements/external-source-entry/external-source-entry-list-submission-element.component';
import { OrgUnitSidebarSearchListElementComponent } from './item-list-elements/sidebar-search-list-elements/org-unit/org-unit-sidebar-search-list-element.component';
import { PersonSidebarSearchListElementComponent } from './item-list-elements/sidebar-search-list-elements/person/person-sidebar-search-list-element.component';
import { ProjectSidebarSearchListElementComponent } from './item-list-elements/sidebar-search-list-elements/project/project-sidebar-search-list-element.component';
import { ItemSharedModule } from '../../item-page/item-shared.module';
import { ResultsBackButtonModule } from '../../shared/results-back-button/results-back-button.module';
import { DsoPageModule } from '../../shared/dso-page/dso-page.module';

const ENTRY_COMPONENTS = [
// put only entry components that use custom decorator
  OrgUnitComponent,
  PersonComponent,
  ProjectComponent,
  OrgUnitListElementComponent,
  OrgUnitItemMetadataListElementComponent,
  PersonListElementComponent,
  PersonItemMetadataListElementComponent,
  ProjectListElementComponent,
  PersonGridElementComponent,
  OrgUnitGridElementComponent,
  ProjectGridElementComponent,
  OrgUnitSearchResultListElementComponent,
  PersonSearchResultListElementComponent,
  ProjectSearchResultListElementComponent,
  PersonSearchResultGridElementComponent,
  OrgUnitSearchResultGridElementComponent,
  ProjectSearchResultGridElementComponent,
  PersonSearchResultListSubmissionElementComponent,
  OrgUnitSearchResultListSubmissionElementComponent,
  OrgUnitInputSuggestionsComponent,
  ExternalSourceEntryListSubmissionElementComponent,
  OrgUnitSidebarSearchListElementComponent,
  PersonSidebarSearchListElementComponent,
  ProjectSidebarSearchListElementComponent,
];

const COMPONENTS = [
  NameVariantModalComponent,
  PersonInputSuggestionsComponent,
  ...ENTRY_COMPONENTS
];

@NgModule({
  imports: [
    CommonModule,
    ItemSharedModule,
    SharedModule,
    NgbTooltipModule,
    ResultsBackButtonModule,
    DsoPageModule,
  ],
  declarations: [
    ...COMPONENTS,
  ]
})
export class ResearchEntitiesModule {
  /**
   * NOTE: this method allows to resolve issue with components that using a custom decorator
   * which are not loaded during SSR otherwise
   */
  static withEntryComponents() {
    return {
      ngModule: ResearchEntitiesModule,
      providers: ENTRY_COMPONENTS.map((component) => ({ provide: component }))
    };
  }
}
