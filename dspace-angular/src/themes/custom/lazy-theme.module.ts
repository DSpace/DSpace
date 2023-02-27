import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminRegistriesModule } from '../../app/admin/admin-registries/admin-registries.module';
import { AdminSearchModule } from '../../app/admin/admin-search-page/admin-search.module';
import { AdminWorkflowModuleModule } from '../../app/admin/admin-workflow-page/admin-workflow.module';
import { BitstreamFormatsModule } from '../../app/admin/admin-registries/bitstream-formats/bitstream-formats.module';
import { BrowseByModule } from '../../app/browse-by/browse-by.module';
import { CollectionFormModule } from '../../app/collection-page/collection-form/collection-form.module';
import { CommunityFormModule } from '../../app/community-page/community-form/community-form.module';
import { CoreModule } from '../../app/core/core.module';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { EditItemPageModule } from '../../app/item-page/edit-item-page/edit-item-page.module';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { IdlePreloadModule } from 'angular-idle-preload';
import { JournalEntitiesModule } from '../../app/entity-groups/journal-entities/journal-entities.module';
import { MyDspaceSearchModule } from '../../app/my-dspace-page/my-dspace-search.module';
import { MenuModule } from '../../app/shared/menu/menu.module';
import { NavbarModule } from '../../app/navbar/navbar.module';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ProfilePageModule } from '../../app/profile-page/profile-page.module';
import { RegisterEmailFormModule } from '../../app/register-email-form/register-email-form.module';
import { ResearchEntitiesModule } from '../../app/entity-groups/research-entities/research-entities.module';
import { ScrollToModule } from '@nicky-lenaers/ngx-scroll-to';
import { SearchPageModule } from '../../app/search-page/search-page.module';
import { SharedModule } from '../../app/shared/shared.module';
import { StatisticsModule } from '../../app/statistics/statistics.module';
import { StoreModule } from '@ngrx/store';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { TranslateModule } from '@ngx-translate/core';
import { HomePageModule } from '../../app/home-page/home-page.module';
import { AppModule } from '../../app/app.module';
import { ItemPageModule } from '../../app/item-page/item-page.module';
import { RouterModule } from '@angular/router';
import { CommunityListPageModule } from '../../app/community-list-page/community-list-page.module';
import { InfoModule } from '../../app/info/info.module';
import { StatisticsPageModule } from '../../app/statistics-page/statistics-page.module';
import { CommunityPageModule } from '../../app/community-page/community-page.module';
import { CollectionPageModule } from '../../app/collection-page/collection-page.module';
import { SubmissionModule } from '../../app/submission/submission.module';
import { MyDSpacePageModule } from '../../app/my-dspace-page/my-dspace-page.module';
import { SearchModule } from '../../app/shared/search/search.module';
import { ResourcePoliciesModule } from '../../app/shared/resource-policies/resource-policies.module';
import { ComcolModule } from '../../app/shared/comcol/comcol.module';
import { RootModule } from '../../app/root.module';
import { FileSectionComponent } from './app/item-page/simple/field-components/file-section/file-section.component';
import { HomePageComponent } from './app/home-page/home-page.component';
import { RootComponent } from './app/root/root.component';
import { BrowseBySwitcherComponent } from './app/browse-by/browse-by-switcher/browse-by-switcher.component';
import { CommunityListPageComponent } from './app/community-list-page/community-list-page.component';
import { SearchPageComponent } from './app/search-page/search-page.component';
import { ConfigurationSearchPageComponent } from './app/search-page/configuration-search-page.component';
import { EndUserAgreementComponent } from './app/info/end-user-agreement/end-user-agreement.component';
import { PageNotFoundComponent } from './app/pagenotfound/pagenotfound.component';
import { ObjectNotFoundComponent } from './app/lookup-by-id/objectnotfound/objectnotfound.component';
import { ForbiddenComponent } from './app/forbidden/forbidden.component';
import { PrivacyComponent } from './app/info/privacy/privacy.component';
import {
  CollectionStatisticsPageComponent
} from './app/statistics-page/collection-statistics-page/collection-statistics-page.component';
import {
  CommunityStatisticsPageComponent
} from './app/statistics-page/community-statistics-page/community-statistics-page.component';
import { ItemStatisticsPageComponent } from './app/statistics-page/item-statistics-page/item-statistics-page.component';
import { SiteStatisticsPageComponent } from './app/statistics-page/site-statistics-page/site-statistics-page.component';
import { CommunityPageComponent } from './app/community-page/community-page.component';
import { CollectionPageComponent } from './app/collection-page/collection-page.component';
import { ItemPageComponent } from './app/item-page/simple/item-page.component';
import { FullItemPageComponent } from './app/item-page/full/full-item-page.component';
import { LoginPageComponent } from './app/login-page/login-page.component';
import { LogoutPageComponent } from './app/logout-page/logout-page.component';
import { CreateProfileComponent } from './app/register-page/create-profile/create-profile.component';
import { ForgotEmailComponent } from './app/forgot-password/forgot-password-email/forgot-email.component';
import { ForgotPasswordFormComponent } from './app/forgot-password/forgot-password-form/forgot-password-form.component';
import { ProfilePageComponent } from './app/profile-page/profile-page.component';
import { RegisterEmailComponent } from './app/register-page/register-email/register-email.component';
import { MyDSpacePageComponent } from './app/my-dspace-page/my-dspace-page.component';
import { SubmissionEditComponent } from './app/submission/edit/submission-edit.component';
import {
  SubmissionImportExternalComponent
} from './app/submission/import-external/submission-import-external.component';
import { SubmissionSubmitComponent } from './app/submission/submit/submission-submit.component';
import { WorkflowItemDeleteComponent
} from './app/workflowitems-edit-page/workflow-item-delete/workflow-item-delete.component';
import {
  WorkflowItemSendBackComponent
} from './app/workflowitems-edit-page/workflow-item-send-back/workflow-item-send-back.component';
import { BreadcrumbsComponent } from './app/breadcrumbs/breadcrumbs.component';
import { FeedbackComponent } from './app/info/feedback/feedback.component';
import { CommunityListComponent } from './app/community-list-page/community-list/community-list.component';

import { ComcolPageHandleComponent } from './app/shared/comcol-page-handle/comcol-page-handle.component';
import { AuthNavMenuComponent } from './app/shared/auth-nav-menu/auth-nav-menu.component';
import {
  ExpandableNavbarSectionComponent
} from './app/navbar/expandable-navbar-section/expandable-navbar-section.component';
import {
  EditItemTemplatePageComponent
} from './app/collection-page/edit-item-template-page/edit-item-template-page.component';
import { LoadingComponent } from './app/shared/loading/loading.component';
import { SearchResultsComponent } from './app/shared/search/search-results/search-results.component';
import { AdminSidebarComponent } from './app/admin/admin-sidebar/admin-sidebar.component';
import { ComcolPageBrowseByComponent } from './app/shared/comcol-page-browse-by/comcol-page-browse-by.component';
import { SearchSettingsComponent } from './app/shared/search/search-settings/search-settings.component';
import {
  CommunityPageSubCommunityListComponent
} from './app/community-page/sub-community-list/community-page-sub-community-list.component';
import {
  CommunityPageSubCollectionListComponent
} from './app/community-page/sub-collection-list/community-page-sub-collection-list.component';
import { ObjectListComponent } from './app/shared/object-list/object-list.component';

import { BrowseByMetadataPageComponent } from './app/browse-by/browse-by-metadata-page/browse-by-metadata-page.component';
import { BrowseByDatePageComponent } from './app/browse-by/browse-by-date-page/browse-by-date-page.component';
import { BrowseByTitlePageComponent } from './app/browse-by/browse-by-title-page/browse-by-title-page.component';
import {
  ExternalSourceEntryImportModalComponent
} from './app/shared/form/builder/ds-dynamic-form-ui/relation-lookup-modal/external-source-tab/external-source-entry-import-modal/external-source-entry-import-modal.component';
import { SharedBrowseByModule } from '../../app/shared/browse-by/shared-browse-by.module';
import { ResultsBackButtonModule } from '../../app/shared/results-back-button/results-back-button.module';
import { ItemVersionsModule } from '../../app/item-page/versions/item-versions.module';
import { ItemSharedModule } from '../../app/item-page/item-shared.module';
import { ResultsBackButtonComponent } from './app/shared/results-back-button/results-back-button.component';
import { DsoEditMetadataComponent } from './app/dso-shared/dso-edit-metadata/dso-edit-metadata.component';
import { DsoSharedModule } from '../../app/dso-shared/dso-shared.module';
import { SystemWideAlertModule } from '../../app/system-wide-alert/system-wide-alert.module';
import { DsoPageModule } from '../../app/shared/dso-page/dso-page.module';

const DECLARATIONS = [
  FileSectionComponent,
  HomePageComponent,
  RootComponent,
  BrowseBySwitcherComponent,
  CommunityListPageComponent,
  SearchPageComponent,
  ConfigurationSearchPageComponent,
  EndUserAgreementComponent,
  PageNotFoundComponent,
  ObjectNotFoundComponent,
  ForbiddenComponent,
  PrivacyComponent,
  CollectionStatisticsPageComponent,
  CommunityStatisticsPageComponent,
  ItemStatisticsPageComponent,
  SiteStatisticsPageComponent,
  CommunityPageComponent,
  CommunityPageSubCommunityListComponent,
  CommunityPageSubCollectionListComponent,
  CollectionPageComponent,
  ItemPageComponent,
  FullItemPageComponent,
  LoginPageComponent,
  LogoutPageComponent,
  CreateProfileComponent,
  ForgotEmailComponent,
  ForgotPasswordFormComponent,
  ProfilePageComponent,
  RegisterEmailComponent,
  MyDSpacePageComponent,
  SubmissionEditComponent,
  SubmissionImportExternalComponent,
  SubmissionSubmitComponent,
  WorkflowItemDeleteComponent,
  WorkflowItemSendBackComponent,
  BreadcrumbsComponent,
  FeedbackComponent,
  CommunityListComponent,
  ComcolPageHandleComponent,
  AuthNavMenuComponent,
  ExpandableNavbarSectionComponent,
  EditItemTemplatePageComponent,
  LoadingComponent,
  SearchResultsComponent,
  AdminSidebarComponent,
  SearchSettingsComponent,
  ComcolPageBrowseByComponent,
  ObjectListComponent,
  BrowseByMetadataPageComponent,
  BrowseByDatePageComponent,
  BrowseByTitlePageComponent,
  ExternalSourceEntryImportModalComponent,
  ResultsBackButtonComponent,
  DsoEditMetadataComponent,
];

@NgModule({
  imports: [
    AdminRegistriesModule,
    AdminSearchModule,
    AdminWorkflowModuleModule,
    AppModule,
    RootModule,
    BitstreamFormatsModule,
    BrowseByModule,
    CollectionFormModule,
    CollectionPageModule,
    CommonModule,
    CommunityFormModule,
    CommunityListPageModule,
    CommunityPageModule,
    CoreModule,
    DragDropModule,
    ItemSharedModule,
    ItemPageModule,
    EditItemPageModule,
    ItemVersionsModule,
    FormsModule,
    HomePageModule,
    HttpClientModule,
    IdlePreloadModule,
    InfoModule,
    JournalEntitiesModule,
    MenuModule,
    DsoPageModule,
    MyDspaceSearchModule,
    NavbarModule,
    NgbModule,
    ProfilePageModule,
    RegisterEmailFormModule,
    ResearchEntitiesModule,
    RouterModule,
    ScrollToModule,
    SearchPageModule,
    SharedModule,
    SharedBrowseByModule,
    ResultsBackButtonModule,
    StatisticsModule,
    StatisticsPageModule,
    StoreModule,
    StoreRouterConnectingModule,
    TranslateModule,
    SubmissionModule,
    MyDSpacePageModule,
    MyDspaceSearchModule,
    SearchModule,
    FormsModule,
    ResourcePoliciesModule,
    ComcolModule,
    DsoSharedModule,
    SystemWideAlertModule
  ],
  declarations: DECLARATIONS,
  exports: [
    CommunityPageSubCollectionListComponent
  ]
})

  /**
   * This module serves as an index for all the components in this theme.
   * It should import all other modules, so the compiler knows where to find any components referenced
   * from a component in this theme
   * It is purposefully not exported, it should never be imported anywhere else, its only purpose is
   * to give lazily loaded components a context in which they can be compiled successfully
   */
class LazyThemeModule {
}
