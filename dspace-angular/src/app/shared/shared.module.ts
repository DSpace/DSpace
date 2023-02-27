import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DragDropModule } from '@angular/cdk/drag-drop';
import {
  NgbDropdownModule,
  NgbNavModule,
  NgbPaginationModule,
  NgbTooltipModule,
  NgbTypeaheadModule,
} from '@ng-bootstrap/ng-bootstrap';
import { MissingTranslationHandler, TranslateModule } from '@ngx-translate/core';
import { InfiniteScrollModule } from 'ngx-infinite-scroll';
import { ConfirmationModalComponent } from './confirmation-modal/confirmation-modal.component';
import {
  ExportMetadataSelectorComponent
} from './dso-selector/modal-wrappers/export-metadata-selector/export-metadata-selector.component';
import {
  ExportBatchSelectorComponent
} from './dso-selector/modal-wrappers/export-batch-selector/export-batch-selector.component';
import {
  ImportBatchSelectorComponent
} from './dso-selector/modal-wrappers/import-batch-selector/import-batch-selector.component';
import { ItemListElementComponent } from './object-list/item-list-element/item-types/item/item-list-element.component';
import { EnumKeysPipe } from './utils/enum-keys-pipe';
import { FileSizePipe } from './utils/file-size-pipe';
import { MetadataFieldValidator } from './utils/metadatafield-validator.directive';
import { SafeUrlPipe } from './utils/safe-url-pipe';
import { ConsolePipe } from './utils/console.pipe';
import {
  CollectionListElementComponent
} from './object-list/collection-list-element/collection-list-element.component';
import { CommunityListElementComponent } from './object-list/community-list-element/community-list-element.component';
import {
  SearchResultListElementComponent
} from './object-list/search-result-list-element/search-result-list-element.component';
import { ObjectListComponent } from './object-list/object-list.component';
import { ThemedObjectListComponent } from './object-list/themed-object-list.component';
import {
  CollectionGridElementComponent
} from './object-grid/collection-grid-element/collection-grid-element.component';
import { CommunityGridElementComponent } from './object-grid/community-grid-element/community-grid-element.component';
import {
  AbstractListableElementComponent
} from './object-collection/shared/object-collection-element/abstract-listable-element.component';
import { ObjectGridComponent } from './object-grid/object-grid.component';
import { ObjectCollectionComponent } from './object-collection/object-collection.component';
import { ErrorComponent } from './error/error.component';
import { LoadingComponent } from './loading/loading.component';
import { PaginationComponent } from './pagination/pagination.component';
import { ThumbnailComponent } from '../thumbnail/thumbnail.component';
import { SearchFormComponent } from './search-form/search-form.component';
import {
  SearchResultGridElementComponent
} from './object-grid/search-result-grid-element/search-result-grid-element.component';
import { ViewModeSwitchComponent } from './view-mode-switch/view-mode-switch.component';
import { VarDirective } from './utils/var.directive';
import { AuthNavMenuComponent } from './auth-nav-menu/auth-nav-menu.component';
import { ThemedAuthNavMenuComponent } from './auth-nav-menu/themed-auth-nav-menu.component';
import { LogOutComponent } from './log-out/log-out.component';
import { DragClickDirective } from './utils/drag-click.directive';
import { TruncatePipe } from './utils/truncate.pipe';
import { TruncatableComponent } from './truncatable/truncatable.component';
import { TruncatableService } from './truncatable/truncatable.service';
import { TruncatablePartComponent } from './truncatable/truncatable-part/truncatable-part.component';
import { MockAdminGuard } from './mocks/admin-guard.service.mock';
import { AlertComponent } from './alert/alert.component';
import {
  SearchResultDetailElementComponent
} from './object-detail/my-dspace-result-detail-element/search-result-detail-element.component';
import { ObjectDetailComponent } from './object-detail/object-detail.component';
import { ObjNgFor } from './utils/object-ngfor.pipe';
import {
  BrowseEntryListElementComponent
} from './object-list/browse-entry-list-element/browse-entry-list-element.component';
import { DebounceDirective } from './utils/debounce.directive';
import { ClickOutsideDirective } from './utils/click-outside.directive';
import { EmphasizePipe } from './utils/emphasize.pipe';
import { InputSuggestionsComponent } from './input-suggestions/input-suggestions.component';
import { CapitalizePipe } from './utils/capitalize.pipe';
import { ObjectKeysPipe } from './utils/object-keys-pipe';
import { LangSwitchComponent } from './lang-switch/lang-switch.component';
import {
  PlainTextMetadataListElementComponent
} from './object-list/metadata-representation-list-element/plain-text/plain-text-metadata-list-element.component';
import {
  BrowseLinkMetadataListElementComponent
} from './object-list/metadata-representation-list-element/browse-link/browse-link-metadata-list-element.component';
import {
  ItemMetadataListElementComponent
} from './object-list/metadata-representation-list-element/item/item-metadata-list-element.component';
import {
  MetadataRepresentationListElementComponent
} from './object-list/metadata-representation-list-element/metadata-representation-list-element.component';
import { ObjectValuesPipe } from './utils/object-values-pipe';
import { InListValidator } from './utils/in-list-validator.directive';
import { AutoFocusDirective } from './utils/auto-focus.directive';
import { StartsWithDateComponent } from './starts-with/date/starts-with-date.component';
import { StartsWithTextComponent } from './starts-with/text/starts-with-text.component';
import { DSOSelectorComponent } from './dso-selector/dso-selector/dso-selector.component';
import {
  CreateCommunityParentSelectorComponent
} from './dso-selector/modal-wrappers/create-community-parent-selector/create-community-parent-selector.component';
import {
  ThemedCreateCommunityParentSelectorComponent
} from './dso-selector/modal-wrappers/create-community-parent-selector/themed-create-community-parent-selector.component';
import {
  CreateItemParentSelectorComponent
} from './dso-selector/modal-wrappers/create-item-parent-selector/create-item-parent-selector.component';
import {
  ThemedCreateItemParentSelectorComponent
} from './dso-selector/modal-wrappers/create-item-parent-selector/themed-create-item-parent-selector.component';
import {
  CreateCollectionParentSelectorComponent
} from './dso-selector/modal-wrappers/create-collection-parent-selector/create-collection-parent-selector.component';
import {
  ThemedCreateCollectionParentSelectorComponent
} from './dso-selector/modal-wrappers/create-collection-parent-selector/themed-create-collection-parent-selector.component';
import {
  CommunitySearchResultListElementComponent
} from './object-list/search-result-list-element/community-search-result/community-search-result-list-element.component';
import {
  CollectionSearchResultListElementComponent
} from './object-list/search-result-list-element/collection-search-result/collection-search-result-list-element.component';
import {
  EditItemSelectorComponent
} from './dso-selector/modal-wrappers/edit-item-selector/edit-item-selector.component';
import {
  ThemedEditItemSelectorComponent
} from './dso-selector/modal-wrappers/edit-item-selector/themed-edit-item-selector.component';
import {
  EditCommunitySelectorComponent
} from './dso-selector/modal-wrappers/edit-community-selector/edit-community-selector.component';
import {
  ThemedEditCommunitySelectorComponent
} from './dso-selector/modal-wrappers/edit-community-selector/themed-edit-community-selector.component';
import {
  EditCollectionSelectorComponent
} from './dso-selector/modal-wrappers/edit-collection-selector/edit-collection-selector.component';
import {
  ThemedEditCollectionSelectorComponent
} from './dso-selector/modal-wrappers/edit-collection-selector/themed-edit-collection-selector.component';
import { RoleDirective } from './roles/role.directive';
import { UserMenuComponent } from './auth-nav-menu/user-menu/user-menu.component';
import {
  CollectionSearchResultGridElementComponent
} from './object-grid/search-result-grid-element/collection-search-result/collection-search-result-grid-element.component';
import {
  CommunitySearchResultGridElementComponent
} from './object-grid/search-result-grid-element/community-search-result/community-search-result-grid-element.component';
import { PageSizeSelectorComponent } from './page-size-selector/page-size-selector.component';
import { AbstractTrackableComponent } from './trackable/abstract-trackable.component';
import {
  ComcolMetadataComponent
} from './comcol/comcol-forms/edit-comcol-page/comcol-metadata/comcol-metadata.component';
import { ItemSelectComponent } from './object-select/item-select/item-select.component';
import { CollectionSelectComponent } from './object-select/collection-select/collection-select.component';
import {
  FilterInputSuggestionsComponent
} from './input-suggestions/filter-suggestions/filter-input-suggestions.component';
import {
  DsoInputSuggestionsComponent
} from './input-suggestions/dso-input-suggestions/dso-input-suggestions.component';
import { ItemGridElementComponent } from './object-grid/item-grid-element/item-types/item/item-grid-element.component';
import { TypeBadgeComponent } from './object-list/type-badge/type-badge.component';
import { AccessStatusBadgeComponent } from './object-list/access-status-badge/access-status-badge.component';
import {
  MetadataRepresentationLoaderComponent
} from './metadata-representation/metadata-representation-loader.component';
import { MetadataRepresentationDirective } from './metadata-representation/metadata-representation.directive';
import {
  ListableObjectComponentLoaderComponent
} from './object-collection/shared/listable-object/listable-object-component-loader.component';
import {
  ItemSearchResultListElementComponent
} from './object-list/search-result-list-element/item-search-result/item-types/item/item-search-result-list-element.component';
import { ListableObjectDirective } from './object-collection/shared/listable-object/listable-object.directive';
import {
  ItemMetadataRepresentationListElementComponent
} from './object-list/metadata-representation-list-element/item/item-metadata-representation-list-element.component';
import { PageWithSidebarComponent } from './sidebar/page-with-sidebar.component';
import { SidebarDropdownComponent } from './sidebar/sidebar-dropdown.component';
import {
  SelectableListItemControlComponent
} from './object-collection/shared/selectable-list-item-control/selectable-list-item-control.component';
import {
  ImportableListItemControlComponent
} from './object-collection/shared/importable-list-item-control/importable-list-item-control.component';
import { LogInContainerComponent } from './log-in/container/log-in-container.component';
import { LogInPasswordComponent } from './log-in/methods/password/log-in-password.component';
import { LogInComponent } from './log-in/log-in.component';
import { MissingTranslationHelper } from './translate/missing-translation.helper';
import { FileValidator } from './utils/require-file.validator';
import { FileValueAccessorDirective } from './utils/file-value-accessor.directive';
import {
  ModifyItemOverviewComponent
} from '../item-page/edit-item-page/modify-item-overview/modify-item-overview.component';
import { ClaimedTaskActionsDirective } from './mydspace-actions/claimed-task/switcher/claimed-task-actions.directive';
import { ImpersonateNavbarComponent } from './impersonate-navbar/impersonate-navbar.component';
import { NgForTrackByIdDirective } from './ng-for-track-by-id.directive';
import { FileDownloadLinkComponent } from './file-download-link/file-download-link.component';
import { ThemedFileDownloadLinkComponent } from './file-download-link/themed-file-download-link.component';
import { CollectionDropdownComponent } from './collection-dropdown/collection-dropdown.component';
import { EntityDropdownComponent } from './entity-dropdown/entity-dropdown.component';
import { CurationFormComponent } from '../curation-form/curation-form.component';
import {
  PublicationSidebarSearchListElementComponent
} from './object-list/sidebar-search-list-element/item-types/publication/publication-sidebar-search-list-element.component';
import {
  SidebarSearchListElementComponent
} from './object-list/sidebar-search-list-element/sidebar-search-list-element.component';
import {
  CollectionSidebarSearchListElementComponent
} from './object-list/sidebar-search-list-element/collection/collection-sidebar-search-list-element.component';
import {
  CommunitySidebarSearchListElementComponent
} from './object-list/sidebar-search-list-element/community/community-sidebar-search-list-element.component';
import {
  BundleListElementComponent
} from './object-list/bundle-list-element/bundle-list-element.component';
import {
  AuthorizedCollectionSelectorComponent
} from './dso-selector/dso-selector/authorized-collection-selector/authorized-collection-selector.component';
import { HoverClassDirective } from './hover-class.directive';
import {
  ValidationSuggestionsComponent
} from './input-suggestions/validation-suggestions/validation-suggestions.component';
import {
  ItemSearchResultGridElementComponent
} from './object-grid/search-result-grid-element/item-search-result/item/item-search-result-grid-element.component';
import { SearchNavbarComponent } from '../search-navbar/search-navbar.component';
import { ThemedSearchNavbarComponent } from '../search-navbar/themed-search-navbar.component';
import { ScopeSelectorModalComponent } from './search-form/scope-selector-modal/scope-selector-modal.component';
import { DsSelectComponent } from './ds-select/ds-select.component';
import { ContextHelpDirective } from './context-help.directive';
import { ContextHelpWrapperComponent } from './context-help-wrapper/context-help-wrapper.component';
import { RSSComponent } from './rss-feed/rss.component';
import { BrowserOnlyPipe } from './utils/browser-only.pipe';
import { ThemedLoadingComponent } from './loading/themed-loading.component';
import { SearchExportCsvComponent } from './search/search-export-csv/search-export-csv.component';
import {
  ItemPageTitleFieldComponent
} from '../item-page/simple/field-components/specific-field/title/item-page-title-field.component';
import { MarkdownPipe } from './utils/markdown.pipe';
import { GoogleRecaptchaModule } from '../core/google-recaptcha/google-recaptcha.module';
import { MenuModule } from './menu/menu.module';
import {
  ListableNotificationObjectComponent
} from './object-list/listable-notification-object/listable-notification-object.component';
import { ThemedCollectionDropdownComponent } from './collection-dropdown/themed-collection-dropdown.component';
import { MetadataFieldWrapperComponent } from './metadata-field-wrapper/metadata-field-wrapper.component';
import { ShortNumberPipe } from './utils/short-number.pipe';
import {
  LogInExternalProviderComponent
} from './log-in/methods/log-in-external-provider/log-in-external-provider.component';
import {
  AdvancedClaimedTaskActionSelectReviewerComponent
} from './mydspace-actions/claimed-task/select-reviewer/advanced-claimed-task-action-select-reviewer.component';
import {
  AdvancedClaimedTaskActionRatingComponent
} from './mydspace-actions/claimed-task/rating/advanced-claimed-task-action-rating.component';
import { ClaimedTaskActionsDeclineTaskComponent } from './mydspace-actions/claimed-task/decline-task/claimed-task-actions-decline-task.component';
import {
  DsoPageSubscriptionButtonComponent
} from './dso-page/dso-page-subscription-button/dso-page-subscription-button.component';
import { EpersonGroupListComponent } from './eperson-group-list/eperson-group-list.component';
import { EpersonSearchBoxComponent } from './eperson-group-list/eperson-search-box/eperson-search-box.component';
import { GroupSearchBoxComponent } from './eperson-group-list/group-search-box/group-search-box.component';

const MODULES = [
  CommonModule,
  FormsModule,
  InfiniteScrollModule,
  NgbNavModule,
  NgbTypeaheadModule,
  NgbPaginationModule,
  NgbDropdownModule,
  NgbTooltipModule,
  ReactiveFormsModule,
  RouterModule,
  DragDropModule,
  GoogleRecaptchaModule,
  MenuModule
];

const ROOT_MODULES = [
  TranslateModule.forChild({
    missingTranslationHandler: { provide: MissingTranslationHandler, useClass: MissingTranslationHelper },
    useDefaultLang: true
  })
];

const PIPES = [
  // put shared pipes here
  EnumKeysPipe,
  FileSizePipe,
  SafeUrlPipe,
  TruncatePipe,
  EmphasizePipe,
  CapitalizePipe,
  ObjectKeysPipe,
  ObjectValuesPipe,
  ConsolePipe,
  ObjNgFor,
  BrowserOnlyPipe,
  MarkdownPipe,
  ShortNumberPipe
];

const COMPONENTS = [
  // put shared components here
  AlertComponent,
  AuthNavMenuComponent,
  ThemedAuthNavMenuComponent,
  UserMenuComponent,
  DsSelectComponent,
  ErrorComponent,
  LangSwitchComponent,
  LoadingComponent,
  ThemedLoadingComponent,
  LogInComponent,
  LogOutComponent,
  ObjectListComponent,
  ThemedObjectListComponent,
  ObjectDetailComponent,
  ObjectGridComponent,
  AbstractListableElementComponent,
  ObjectCollectionComponent,
  PaginationComponent,
  RSSComponent,
  SearchFormComponent,
  PageWithSidebarComponent,
  SidebarDropdownComponent,
  ThumbnailComponent,
  ViewModeSwitchComponent,
  TruncatableComponent,
  TruncatablePartComponent,
  InputSuggestionsComponent,
  FilterInputSuggestionsComponent,
  ValidationSuggestionsComponent,
  DsoInputSuggestionsComponent,
  DSOSelectorComponent,
  SearchExportCsvComponent,
  PageSizeSelectorComponent,
  ListableObjectComponentLoaderComponent,
  AbstractTrackableComponent,
  ComcolMetadataComponent,
  TypeBadgeComponent,
  AccessStatusBadgeComponent,
  ItemSelectComponent,
  CollectionSelectComponent,
  MetadataRepresentationLoaderComponent,
  SelectableListItemControlComponent,
  ImportableListItemControlComponent,
  LogInContainerComponent,
  ModifyItemOverviewComponent,
  ImpersonateNavbarComponent,
  EntityDropdownComponent,
  ExportMetadataSelectorComponent,
  ImportBatchSelectorComponent,
  ExportBatchSelectorComponent,
  ConfirmationModalComponent,
  AuthorizedCollectionSelectorComponent,
  SearchNavbarComponent,
  ItemPageTitleFieldComponent,
  ThemedSearchNavbarComponent,
  ListableNotificationObjectComponent,
  DsoPageSubscriptionButtonComponent,
  MetadataFieldWrapperComponent,
  ContextHelpWrapperComponent,
  EpersonGroupListComponent,
  EpersonSearchBoxComponent,
  GroupSearchBoxComponent
];

const ENTRY_COMPONENTS = [
  // put only entry components that use custom decorator
  BundleListElementComponent,
  CollectionListElementComponent,
  CommunityListElementComponent,
  SearchResultListElementComponent,
  CommunitySearchResultListElementComponent,
  CollectionSearchResultListElementComponent,
  CollectionGridElementComponent,
  CommunityGridElementComponent,
  CommunitySearchResultGridElementComponent,
  CollectionSearchResultGridElementComponent,
  SearchResultGridElementComponent,
  ItemListElementComponent,
  ItemGridElementComponent,
  ItemSearchResultListElementComponent,
  ItemSearchResultGridElementComponent,
  BrowseEntryListElementComponent,
  SearchResultDetailElementComponent,
  StartsWithDateComponent,
  StartsWithTextComponent,
  CreateCommunityParentSelectorComponent,
  ThemedCreateCommunityParentSelectorComponent,
  CreateCollectionParentSelectorComponent,
  ThemedCreateCollectionParentSelectorComponent,
  CreateItemParentSelectorComponent,
  ThemedCreateItemParentSelectorComponent,
  EditCommunitySelectorComponent,
  ThemedEditCommunitySelectorComponent,
  EditCollectionSelectorComponent,
  ThemedEditCollectionSelectorComponent,
  EditItemSelectorComponent,
  ThemedEditItemSelectorComponent,
  PlainTextMetadataListElementComponent,
  BrowseLinkMetadataListElementComponent,
  ItemMetadataListElementComponent,
  MetadataRepresentationListElementComponent,
  ItemMetadataRepresentationListElementComponent,
  LogInPasswordComponent,
  LogInExternalProviderComponent,
  ClaimedTaskActionsDeclineTaskComponent,
  CollectionDropdownComponent,
  ThemedCollectionDropdownComponent,
  FileDownloadLinkComponent,
  ThemedFileDownloadLinkComponent,
  CurationFormComponent,
  ExportMetadataSelectorComponent,
  ImportBatchSelectorComponent,
  ExportBatchSelectorComponent,
  ConfirmationModalComponent,
  SidebarSearchListElementComponent,
  PublicationSidebarSearchListElementComponent,
  CollectionSidebarSearchListElementComponent,
  CommunitySidebarSearchListElementComponent,
  ScopeSelectorModalComponent,
  ListableNotificationObjectComponent,
  AdvancedClaimedTaskActionSelectReviewerComponent,
  AdvancedClaimedTaskActionRatingComponent,
  EpersonGroupListComponent,
  EpersonSearchBoxComponent,
  GroupSearchBoxComponent
];

const PROVIDERS = [
  TruncatableService,
  MockAdminGuard,
  AbstractTrackableComponent
];

const DIRECTIVES = [
  VarDirective,
  DragClickDirective,
  DebounceDirective,
  ClickOutsideDirective,
  InListValidator,
  AutoFocusDirective,
  RoleDirective,
  MetadataRepresentationDirective,
  ListableObjectDirective,
  ClaimedTaskActionsDirective,
  FileValueAccessorDirective,
  FileValidator,
  NgForTrackByIdDirective,
  MetadataFieldValidator,
  HoverClassDirective,
  ContextHelpDirective,
];

@NgModule({
  imports: [
    ...MODULES,
    ...ROOT_MODULES
  ],
  declarations: [
    ...PIPES,
    ...COMPONENTS,
    ...ENTRY_COMPONENTS,
    ...DIRECTIVES,
  ],
  providers: [
    ...PROVIDERS
  ],
  exports: [
    ...MODULES,
    ...PIPES,
    ...COMPONENTS,
    ...ENTRY_COMPONENTS,
    ...DIRECTIVES,
    TranslateModule,
  ]
})

/**
 * This module handles all components and pipes that need to be shared among multiple other modules
 */
export class SharedModule {
  /**
   * NOTE: this method allows to resolve issue with components that using a custom decorator
   * which are not loaded during SSR otherwise
   */
  static withEntryComponents() {
    return {
      ngModule: SharedModule,
      providers: ENTRY_COMPONENTS.map((component) => ({ provide: component }))
    };
  }
}
