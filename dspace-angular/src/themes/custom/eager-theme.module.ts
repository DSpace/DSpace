import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../app/shared/shared.module';
import { HomeNewsComponent } from './app/home-page/home-news/home-news.component';
import { NavbarComponent } from './app/navbar/navbar.component';
import { SearchNavbarComponent } from './app/search-navbar/search-navbar.component';
import { HeaderComponent } from './app/header/header.component';
import { HeaderNavbarWrapperComponent } from './app/header-nav-wrapper/header-navbar-wrapper.component';
import { RootModule } from '../../app/root.module';
import { NavbarModule } from '../../app/navbar/navbar.module';
import { PublicationComponent } from './app/item-page/simple/item-types/publication/publication.component';
import { ItemPageModule } from '../../app/item-page/item-page.module';
import { FooterComponent } from './app/footer/footer.component';
import { JournalComponent } from './app/entity-groups/journal-entities/item-pages/journal/journal.component';
import {
  JournalIssueComponent
} from './app/entity-groups/journal-entities/item-pages/journal-issue/journal-issue.component';
import {
  JournalVolumeComponent
} from './app/entity-groups/journal-entities/item-pages/journal-volume/journal-volume.component';
import { UntypedItemComponent } from './app/item-page/simple/item-types/untyped-item/untyped-item.component';
import { ItemSharedModule } from '../../app/item-page/item-shared.module';
import {
    CreateCollectionParentSelectorComponent
} from './app/shared/dso-selector/modal-wrappers/create-collection-parent-selector/create-collection-parent-selector.component';
import {
    CreateCommunityParentSelectorComponent
} from './app/shared/dso-selector/modal-wrappers/create-community-parent-selector/create-community-parent-selector.component';
import {
    CreateItemParentSelectorComponent
} from './app/shared/dso-selector/modal-wrappers/create-item-parent-selector/create-item-parent-selector.component';
import {
    EditCollectionSelectorComponent
} from './app/shared/dso-selector/modal-wrappers/edit-collection-selector/edit-collection-selector.component';
import {
    EditCommunitySelectorComponent
} from './app/shared/dso-selector/modal-wrappers/edit-community-selector/edit-community-selector.component';
import {
    EditItemSelectorComponent
} from './app/shared/dso-selector/modal-wrappers/edit-item-selector/edit-item-selector.component';

import { CommunityListElementComponent } from './app/shared/object-list/community-list-element/community-list-element.component';
import { CollectionListElementComponent} from './app/shared/object-list/collection-list-element/collection-list-element.component';
import { CollectionDropdownComponent } from './app/shared/collection-dropdown/collection-dropdown.component';
import { SharedBrowseByModule } from '../../app/shared/browse-by/shared-browse-by.module';
import { ResultsBackButtonModule } from '../../app/shared/results-back-button/results-back-button.module';
import { DsoPageModule } from '../../app/shared/dso-page/dso-page.module';
import { FileDownloadLinkComponent } from './app/shared/file-download-link/file-download-link.component';


/**
 * Add components that use a custom decorator to ENTRY_COMPONENTS as well as DECLARATIONS.
 * This will ensure that decorator gets picked up when the app loads
 */
const ENTRY_COMPONENTS = [
  JournalComponent,
  JournalIssueComponent,
  JournalVolumeComponent,
  PublicationComponent,
  UntypedItemComponent,

  CommunityListElementComponent,
  CollectionListElementComponent,
  CollectionDropdownComponent,
  FileDownloadLinkComponent,
];

const DECLARATIONS = [
  ...ENTRY_COMPONENTS,
  HomeNewsComponent,
  HeaderComponent,
  HeaderNavbarWrapperComponent,
  NavbarComponent,
  SearchNavbarComponent,
  FooterComponent,
  CreateCollectionParentSelectorComponent,
  CreateCommunityParentSelectorComponent,
  CreateItemParentSelectorComponent,
  EditCollectionSelectorComponent,
  EditCommunitySelectorComponent,
  EditItemSelectorComponent,
];

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RootModule,
    NavbarModule,
    SharedBrowseByModule,
    ResultsBackButtonModule,
    ItemPageModule,
    ItemSharedModule,
    DsoPageModule,
  ],
  declarations: DECLARATIONS,
  providers: [
    ...ENTRY_COMPONENTS.map((component) => ({provide: component}))
  ],
})
/**
 * This module is included in the main bundle that gets downloaded at first page load. So it should
 * contain only the themed components that have to be available immediately for the first page load,
 * and the minimal set of imports required to make them work. Anything you can cut from it will make
 * the initial page load faster, but may cause the page to flicker as components that were already
 * rendered server side need to be lazy-loaded again client side
 *
 * Themed EntryComponents should also be added here
 */
export class EagerThemeModule {
}
