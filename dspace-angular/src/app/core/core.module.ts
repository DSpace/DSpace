import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ModuleWithProviders, NgModule, Optional, SkipSelf } from '@angular/core';

import { EffectsModule } from '@ngrx/effects';

import { Action, StoreConfig, StoreModule } from '@ngrx/store';
import { MyDSpaceGuard } from '../my-dspace-page/my-dspace.guard';

import { isNotEmpty } from '../shared/empty.util';
import { HostWindowService } from '../shared/host-window.service';
import { MenuService } from '../shared/menu/menu.service';
import { EndpointMockingRestService } from '../shared/mocks/dspace-rest/endpoint-mocking-rest.service';
import {
  MOCK_RESPONSE_MAP,
  mockResponseMap,
  ResponseMapMock
} from '../shared/mocks/dspace-rest/mocks/response-map.mock';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { SelectableListService } from '../shared/object-list/selectable-list/selectable-list.service';
import { ObjectSelectService } from '../shared/object-select/object-select.service';
import { PaginationComponentOptions } from '../shared/pagination/pagination-component-options.model';
import { SidebarService } from '../shared/sidebar/sidebar.service';
import { AuthenticatedGuard } from './auth/authenticated.guard';
import { AuthStatus } from './auth/models/auth-status.model';
import { BrowseService } from './browse/browse.service';
import { RemoteDataBuildService } from './cache/builders/remote-data-build.service';
import { ObjectCacheService } from './cache/object-cache.service';
import { SubmissionDefinitionsModel } from './config/models/config-submission-definitions.model';
import { SubmissionFormsModel } from './config/models/config-submission-forms.model';
import { SubmissionSectionModel } from './config/models/config-submission-section.model';
import { SubmissionUploadsModel } from './config/models/config-submission-uploads.model';
import { SubmissionFormsConfigDataService } from './config/submission-forms-config-data.service';
import { coreEffects } from './core.effects';
import { coreReducers } from './core.reducers';
import { BitstreamFormatDataService } from './data/bitstream-format-data.service';
import { CollectionDataService } from './data/collection-data.service';
import { CommunityDataService } from './data/community-data.service';
import { ContentSourceResponseParsingService } from './data/content-source-response-parsing.service';
import { DebugResponseParsingService } from './data/debug-response-parsing.service';
import { DefaultChangeAnalyzer } from './data/default-change-analyzer.service';
import { DSOChangeAnalyzer } from './data/dso-change-analyzer.service';
import { DSOResponseParsingService } from './data/dso-response-parsing.service';
import { DSpaceObjectDataService } from './data/dspace-object-data.service';
import { EndpointMapResponseParsingService } from './data/endpoint-map-response-parsing.service';
import { EntityTypeDataService } from './data/entity-type-data.service';
import { ExternalSourceDataService } from './data/external-source-data.service';
import { FacetConfigResponseParsingService } from './data/facet-config-response-parsing.service';
import { FacetValueResponseParsingService } from './data/facet-value-response-parsing.service';
import { FilteredDiscoveryPageResponseParsingService } from './data/filtered-discovery-page-response-parsing.service';
import { ItemDataService } from './data/item-data.service';
import { LookupRelationService } from './data/lookup-relation.service';
import { MyDSpaceResponseParsingService } from './data/mydspace-response-parsing.service';
import { ObjectUpdatesService } from './data/object-updates/object-updates.service';
import { RelationshipTypeDataService } from './data/relationship-type-data.service';
import { RelationshipDataService } from './data/relationship-data.service';
import { ResourcePolicyDataService } from './resource-policy/resource-policy-data.service';
import { SearchResponseParsingService } from './data/search-response-parsing.service';
import { SiteDataService } from './data/site-data.service';
import { DspaceRestService } from './dspace-rest/dspace-rest.service';
import { EPersonDataService } from './eperson/eperson-data.service';
import { EPerson } from './eperson/models/eperson.model';
import { Group } from './eperson/models/group.model';
import { JsonPatchOperationsBuilder } from './json-patch/builder/json-patch-operations-builder';
import { MetadataField } from './metadata/metadata-field.model';
import { MetadataSchema } from './metadata/metadata-schema.model';
import { MetadataService } from './metadata/metadata.service';
import { RegistryService } from './registry/registry.service';
import { RoleService } from './roles/role.service';
import { FeedbackDataService } from './feedback/feedback-data.service';

import { ServerResponseService } from './services/server-response.service';
import { NativeWindowFactory, NativeWindowService } from './services/window.service';
import { BitstreamFormat } from './shared/bitstream-format.model';
import { Bitstream } from './shared/bitstream.model';
import { BrowseDefinition } from './shared/browse-definition.model';
import { BrowseEntry } from './shared/browse-entry.model';
import { Bundle } from './shared/bundle.model';
import { Collection } from './shared/collection.model';
import { Community } from './shared/community.model';
import { DSpaceObject } from './shared/dspace-object.model';
import { ExternalSourceEntry } from './shared/external-source-entry.model';
import { ExternalSource } from './shared/external-source.model';
import { HALEndpointService } from './shared/hal-endpoint.service';
import { ItemType } from './shared/item-relationships/item-type.model';
import { RelationshipType } from './shared/item-relationships/relationship-type.model';
import { Relationship } from './shared/item-relationships/relationship.model';
import { Item } from './shared/item.model';
import { License } from './shared/license.model';
import { ResourcePolicy } from './resource-policy/models/resource-policy.model';
import { SearchConfigurationService } from './shared/search/search-configuration.service';
import { SearchFilterService } from './shared/search/search-filter.service';
import { SearchService } from './shared/search/search.service';
import { Site } from './shared/site.model';
import { UUIDService } from './shared/uuid.service';
import { WorkflowItem } from './submission/models/workflowitem.model';
import { WorkspaceItem } from './submission/models/workspaceitem.model';
import { SubmissionJsonPatchOperationsService } from './submission/submission-json-patch-operations.service';
import { SubmissionResponseParsingService } from './submission/submission-response-parsing.service';
import { SubmissionRestService } from './submission/submission-rest.service';
import { WorkflowItemDataService } from './submission/workflowitem-data.service';
import { WorkspaceitemDataService } from './submission/workspaceitem-data.service';
import { ClaimedTaskDataService } from './tasks/claimed-task-data.service';
import { ClaimedTask } from './tasks/models/claimed-task-object.model';
import { PoolTask } from './tasks/models/pool-task-object.model';
import { TaskObject } from './tasks/models/task-object.model';
import { PoolTaskDataService } from './tasks/pool-task-data.service';
import { TaskResponseParsingService } from './tasks/task-response-parsing.service';
import { ArrayMoveChangeAnalyzer } from './data/array-move-change-analyzer.service';
import { BitstreamDataService } from './data/bitstream-data.service';
import { environment } from '../../environments/environment';
import { storeModuleConfig } from '../app.reducer';
import { VersionDataService } from './data/version-data.service';
import { VersionHistoryDataService } from './data/version-history-data.service';
import { Version } from './shared/version.model';
import { VersionHistory } from './shared/version-history.model';
import { Script } from '../process-page/scripts/script.model';
import { Process } from '../process-page/processes/process.model';
import { ProcessDataService } from './data/processes/process-data.service';
import { ScriptDataService } from './data/processes/script-data.service';
import { WorkflowActionDataService } from './data/workflow-action-data.service';
import { WorkflowAction } from './tasks/models/workflow-action-object.model';
import { ItemTemplateDataService } from './data/item-template-data.service';
import { TemplateItem } from './shared/template-item.model';
import { Feature } from './shared/feature.model';
import { Authorization } from './shared/authorization.model';
import { FeatureDataService } from './data/feature-authorization/feature-data.service';
import { AuthorizationDataService } from './data/feature-authorization/authorization-data.service';
import {
  SiteAdministratorGuard
} from './data/feature-authorization/feature-authorization-guard/site-administrator.guard';
import { Registration } from './shared/registration.model';
import { MetadataSchemaDataService } from './data/metadata-schema-data.service';
import { MetadataFieldDataService } from './data/metadata-field-data.service';
import { TokenResponseParsingService } from './auth/token-response-parsing.service';
import { SubmissionCcLicenseDataService } from './submission/submission-cc-license-data.service';
import { SubmissionCcLicence } from './submission/models/submission-cc-license.model';
import { SubmissionCcLicenceUrl } from './submission/models/submission-cc-license-url.model';
import { SubmissionCcLicenseUrlDataService } from './submission/submission-cc-license-url-data.service';
import { VocabularyEntry } from './submission/vocabularies/models/vocabulary-entry.model';
import { Vocabulary } from './submission/vocabularies/models/vocabulary.model';
import { VocabularyEntryDetail } from './submission/vocabularies/models/vocabulary-entry-detail.model';
import { VocabularyService } from './submission/vocabularies/vocabulary.service';
import { ConfigurationDataService } from './data/configuration-data.service';
import { ConfigurationProperty } from './shared/configuration-property.model';
import { ReloadGuard } from './reload/reload.guard';
import { EndUserAgreementCurrentUserGuard } from './end-user-agreement/end-user-agreement-current-user.guard';
import { EndUserAgreementCookieGuard } from './end-user-agreement/end-user-agreement-cookie.guard';
import { EndUserAgreementService } from './end-user-agreement/end-user-agreement.service';
import { SiteRegisterGuard } from './data/feature-authorization/feature-authorization-guard/site-register.guard';
import { ShortLivedToken } from './auth/models/short-lived-token.model';
import { UsageReport } from './statistics/models/usage-report.model';
import { RootDataService } from './data/root-data.service';
import { Root } from './data/root.model';
import { SearchConfig } from './shared/search/search-filters/search-config.model';
import { SequenceService } from './shared/sequence.service';
import { CoreState } from './core-state.model';
import { GroupDataService } from './eperson/group-data.service';
import { SubmissionAccessesModel } from './config/models/config-submission-accesses.model';
import { RatingAdvancedWorkflowInfo } from './tasks/models/rating-advanced-workflow-info.model';
import { AdvancedWorkflowInfo } from './tasks/models/advanced-workflow-info.model';
import { SelectReviewerAdvancedWorkflowInfo } from './tasks/models/select-reviewer-advanced-workflow-info.model';
import { AccessStatusObject } from '../shared/object-list/access-status-badge/access-status.model';
import { AccessStatusDataService } from './data/access-status-data.service';
import { LinkHeadService } from './services/link-head.service';
import { ResearcherProfileDataService } from './profile/researcher-profile-data.service';
import { ProfileClaimService } from '../profile-page/profile-claim/profile-claim.service';
import { ResearcherProfile } from './profile/model/researcher-profile.model';
import { OrcidQueueDataService } from './orcid/orcid-queue-data.service';
import { OrcidHistoryDataService } from './orcid/orcid-history-data.service';
import { OrcidQueue } from './orcid/model/orcid-queue.model';
import { OrcidHistory } from './orcid/model/orcid-history.model';
import { OrcidAuthService } from './orcid/orcid-auth.service';
import { VocabularyDataService } from './submission/vocabularies/vocabulary.data.service';
import { VocabularyEntryDetailsDataService } from './submission/vocabularies/vocabulary-entry-details.data.service';
import { IdentifierData } from '../shared/object-list/identifier-data/identifier-data.model';
import { Subscription } from '../shared/subscriptions/models/subscription.model';
import { SupervisionOrderDataService } from './supervision-order/supervision-order-data.service';

/**
 * When not in production, endpoint responses can be mocked for testing purposes
 * If there is no mock version available for the endpoint, the actual REST response will be used just like in production mode
 */
export const restServiceFactory = (mocks: ResponseMapMock, http: HttpClient) => {
  if (environment.production) {
    return new DspaceRestService(http);
  } else {
    return new EndpointMockingRestService(mocks, http);
  }
};

const IMPORTS = [
  CommonModule,
  StoreModule.forFeature('core', coreReducers, storeModuleConfig as StoreConfig<CoreState, Action>),
  EffectsModule.forFeature(coreEffects)
];

const DECLARATIONS = [];

const EXPORTS = [];

const PROVIDERS = [
  AuthenticatedGuard,
  CommunityDataService,
  CollectionDataService,
  SiteDataService,
  DSOResponseParsingService,
  { provide: MOCK_RESPONSE_MAP, useValue: mockResponseMap },
  { provide: DspaceRestService, useFactory: restServiceFactory, deps: [MOCK_RESPONSE_MAP, HttpClient] },
  EPersonDataService,
  LinkHeadService,
  HALEndpointService,
  HostWindowService,
  ItemDataService,
  MetadataService,
  ObjectCacheService,
  PaginationComponentOptions,
  ResourcePolicyDataService,
  RegistryService,
  BitstreamFormatDataService,
  RemoteDataBuildService,
  EndpointMapResponseParsingService,
  FacetValueResponseParsingService,
  FacetConfigResponseParsingService,
  DebugResponseParsingService,
  SearchResponseParsingService,
  MyDSpaceResponseParsingService,
  ServerResponseService,
  BrowseService,
  AccessStatusDataService,
  SubmissionCcLicenseDataService,
  SubmissionCcLicenseUrlDataService,
  SubmissionFormsConfigDataService,
  SubmissionRestService,
  SubmissionResponseParsingService,
  SubmissionJsonPatchOperationsService,
  JsonPatchOperationsBuilder,
  UUIDService,
  NotificationsService,
  WorkspaceitemDataService,
  WorkflowItemDataService,
  DSpaceObjectDataService,
  ConfigurationDataService,
  DSOChangeAnalyzer,
  DefaultChangeAnalyzer,
  ArrayMoveChangeAnalyzer,
  ObjectSelectService,
  MenuService,
  ObjectUpdatesService,
  SearchService,
  RelationshipDataService,
  MyDSpaceGuard,
  RoleService,
  TaskResponseParsingService,
  ClaimedTaskDataService,
  PoolTaskDataService,
  BitstreamDataService,
  EntityTypeDataService,
  ContentSourceResponseParsingService,
  ItemTemplateDataService,
  SearchService,
  SidebarService,
  SearchFilterService,
  SearchFilterService,
  SearchConfigurationService,
  SelectableListService,
  RelationshipTypeDataService,
  ExternalSourceDataService,
  LookupRelationService,
  VersionDataService,
  VersionHistoryDataService,
  WorkflowActionDataService,
  ProcessDataService,
  ScriptDataService,
  FeatureDataService,
  AuthorizationDataService,
  SiteAdministratorGuard,
  SiteRegisterGuard,
  MetadataSchemaDataService,
  MetadataFieldDataService,
  TokenResponseParsingService,
  ReloadGuard,
  EndUserAgreementCurrentUserGuard,
  EndUserAgreementCookieGuard,
  EndUserAgreementService,
  RootDataService,
  NotificationsService,
  FilteredDiscoveryPageResponseParsingService,
  { provide: NativeWindowService, useFactory: NativeWindowFactory },
  VocabularyService,
  VocabularyDataService,
  VocabularyEntryDetailsDataService,
  SequenceService,
  GroupDataService,
  FeedbackDataService,
  ResearcherProfileDataService,
  ProfileClaimService,
  OrcidAuthService,
  OrcidQueueDataService,
  OrcidHistoryDataService,
  SupervisionOrderDataService
];

/**
 * Declaration needed to make sure all decorator functions are called in time
 */
export const models =
  [
    Root,
    DSpaceObject,
    Bundle,
    Bitstream,
    BitstreamFormat,
    Item,
    Site,
    Collection,
    Community,
    EPerson,
    Group,
    ResourcePolicy,
    MetadataSchema,
    MetadataField,
    License,
    WorkflowItem,
    WorkspaceItem,
    SubmissionCcLicence,
    SubmissionCcLicenceUrl,
    SubmissionDefinitionsModel,
    SubmissionFormsModel,
    SubmissionSectionModel,
    SubmissionUploadsModel,
    AuthStatus,
    BrowseEntry,
    BrowseDefinition,
    ClaimedTask,
    TaskObject,
    PoolTask,
    Relationship,
    RelationshipType,
    ItemType,
    ExternalSource,
    ExternalSourceEntry,
    Script,
    Process,
    Version,
    VersionHistory,
    WorkflowAction,
    AdvancedWorkflowInfo,
    RatingAdvancedWorkflowInfo,
    SelectReviewerAdvancedWorkflowInfo,
    TemplateItem,
    Feature,
    Authorization,
    Registration,
    Vocabulary,
    VocabularyEntry,
    VocabularyEntryDetail,
    ConfigurationProperty,
    ShortLivedToken,
    Registration,
    UsageReport,
    Root,
    SearchConfig,
    SubmissionAccessesModel,
    AccessStatusObject,
    ResearcherProfile,
    OrcidQueue,
    OrcidHistory,
    AccessStatusObject,
    IdentifierData,
    Subscription,
  ];

@NgModule({
  imports: [
    ...IMPORTS
  ],
  declarations: [
    ...DECLARATIONS
  ],
  exports: [
    ...EXPORTS
  ],
  providers: [
    ...PROVIDERS
  ]
})

export class CoreModule {
  static forRoot(): ModuleWithProviders<CoreModule> {
    return {
      ngModule: CoreModule,
      providers: [
        ...PROVIDERS
      ]
    };
  }

  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (isNotEmpty(parentModule)) {
      throw new Error('CoreModule is already loaded. Import it in the AppModule only');
    }
  }
}
