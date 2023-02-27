import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SharedModule } from '../shared/shared.module';

import { MyDspacePageRoutingModule } from './my-dspace-page-routing.module';
import { WorkspaceItemSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/workspace-item-search-result/workspace-item-search-result-list-element.component';
import { ClaimedSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/claimed-search-result/claimed-search-result-list-element.component';
import { PoolSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/pool-search-result/pool-search-result-list-element.component';
import { ItemSearchResultDetailElementComponent } from '../shared/object-detail/my-dspace-result-detail-element/item-search-result/item-search-result-detail-element.component';
import { WorkspaceItemSearchResultDetailElementComponent } from '../shared/object-detail/my-dspace-result-detail-element/workspace-item-search-result/workspace-item-search-result-detail-element.component';
import { WorkflowItemSearchResultDetailElementComponent } from '../shared/object-detail/my-dspace-result-detail-element/workflow-item-search-result/workflow-item-search-result-detail-element.component';
import { ClaimedTaskSearchResultDetailElementComponent } from '../shared/object-detail/my-dspace-result-detail-element/claimed-task-search-result/claimed-task-search-result-detail-element.component';
import { ItemSearchResultListElementSubmissionComponent } from '../shared/object-list/my-dspace-result-list-element/item-search-result/item-search-result-list-element-submission.component';
import { WorkflowItemSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/workflow-item-search-result/workflow-item-search-result-list-element.component';
import { PoolSearchResultDetailElementComponent } from '../shared/object-detail/my-dspace-result-detail-element/pool-search-result/pool-search-result-detail-element.component';
import { ClaimedApprovedSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/claimed-search-result/claimed-approved-search-result/claimed-approved-search-result-list-element.component';
import { ClaimedDeclinedSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/claimed-search-result/claimed-declined-search-result/claimed-declined-search-result-list-element.component';
import { ResearchEntitiesModule } from '../entity-groups/research-entities/research-entities.module';
import { ItemSubmitterComponent } from '../shared/object-collection/shared/mydspace-item-submitter/item-submitter.component';
import { ItemCollectionComponent } from '../shared/object-collection/shared/mydspace-item-collection/item-collection.component';
import { ItemDetailPreviewComponent } from '../shared/object-detail/my-dspace-result-detail-element/item-detail-preview/item-detail-preview.component';
import { ItemDetailPreviewFieldComponent } from '../shared/object-detail/my-dspace-result-detail-element/item-detail-preview/item-detail-preview-field/item-detail-preview-field.component';
import { ItemListPreviewComponent } from '../shared/object-list/my-dspace-result-list-element/item-list-preview/item-list-preview.component';
import { ThemedItemListPreviewComponent } from '../shared/object-list/my-dspace-result-list-element/item-list-preview/themed-item-list-preview.component';
import { MyDSpaceItemStatusComponent } from '../shared/object-collection/shared/mydspace-item-status/my-dspace-item-status.component';
import { JournalEntitiesModule } from '../entity-groups/journal-entities/journal-entities.module';
import { MyDSpaceActionsModule } from '../shared/mydspace-actions/mydspace-actions.module';
import { ClaimedDeclinedTaskSearchResultListElementComponent } from '../shared/object-list/my-dspace-result-list-element/claimed-search-result/claimed-declined-task-search-result/claimed-declined-task-search-result-list-element.component';

const ENTRY_COMPONENTS = [
  WorkspaceItemSearchResultListElementComponent,
  WorkflowItemSearchResultListElementComponent,
  ClaimedSearchResultListElementComponent,
  ClaimedApprovedSearchResultListElementComponent,
  ClaimedDeclinedSearchResultListElementComponent,
  ClaimedDeclinedTaskSearchResultListElementComponent,
  PoolSearchResultListElementComponent,
  ItemSearchResultDetailElementComponent,
  WorkspaceItemSearchResultDetailElementComponent,
  WorkflowItemSearchResultDetailElementComponent,
  ClaimedTaskSearchResultDetailElementComponent,
  PoolSearchResultDetailElementComponent,
  ItemSearchResultListElementSubmissionComponent,
];

const DECLARATIONS = [
  ...ENTRY_COMPONENTS,
  ItemSubmitterComponent,
  ItemCollectionComponent,
  ItemDetailPreviewComponent,
  ItemDetailPreviewFieldComponent,
  ItemListPreviewComponent,
  ThemedItemListPreviewComponent,
  MyDSpaceItemStatusComponent,
];

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    MyDspacePageRoutingModule,
    MyDSpaceActionsModule,
    ResearchEntitiesModule.withEntryComponents(),
    JournalEntitiesModule.withEntryComponents(),
  ],
  declarations: [
    ...DECLARATIONS,
  ]
})

/**
 * This module handles all components that are necessary for the mydspace page
 */
export class MyDspaceSearchModule {
  /**
   * NOTE: this method allows to resolve issue with components that using a custom decorator
   * which are not loaded during SSR otherwise
   */
  static withEntryComponents() {
    return {
      ngModule: MyDspaceSearchModule,
      providers: ENTRY_COMPONENTS.map((component) => ({provide: component}))
    };
  }
}
