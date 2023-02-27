import { NgModule } from '@angular/core';

import { SharedModule } from '../../shared/shared.module';
import {
  WorkflowItemSearchResultAdminWorkflowGridElementComponent
} from './admin-workflow-search-results/admin-workflow-search-result-grid-element/workflow-item/workflow-item-search-result-admin-workflow-grid-element.component';
import {
  WorkflowItemAdminWorkflowActionsComponent
} from './admin-workflow-search-results/actions/workflow-item/workflow-item-admin-workflow-actions.component';
import {
  WorkflowItemSearchResultAdminWorkflowListElementComponent
} from './admin-workflow-search-results/admin-workflow-search-result-list-element/workflow-item/workflow-item-search-result-admin-workflow-list-element.component';
import { AdminWorkflowPageComponent } from './admin-workflow-page.component';
import { SearchModule } from '../../shared/search/search.module';
import {
  WorkspaceItemAdminWorkflowActionsComponent
} from './admin-workflow-search-results/actions/workspace-item/workspace-item-admin-workflow-actions.component';
import {
  WorkspaceItemSearchResultAdminWorkflowListElementComponent
} from './admin-workflow-search-results/admin-workflow-search-result-list-element/workspace-item/workspace-item-search-result-admin-workflow-list-element.component';
import {
  WorkspaceItemSearchResultAdminWorkflowGridElementComponent
} from './admin-workflow-search-results/admin-workflow-search-result-grid-element/workspace-item/workspace-item-search-result-admin-workflow-grid-element.component';
import {
  SupervisionOrderGroupSelectorComponent
} from './admin-workflow-search-results/actions/workspace-item/supervision-order-group-selector/supervision-order-group-selector.component';
import {
  SupervisionOrderStatusComponent
} from './admin-workflow-search-results/actions/workspace-item/supervision-order-status/supervision-order-status.component';

const ENTRY_COMPONENTS = [
  // put only entry components that use custom decorator
  WorkflowItemSearchResultAdminWorkflowListElementComponent,
  WorkflowItemSearchResultAdminWorkflowGridElementComponent,
  WorkspaceItemSearchResultAdminWorkflowListElementComponent,
  WorkspaceItemSearchResultAdminWorkflowGridElementComponent
];

@NgModule({
  imports: [
    SearchModule,
    SharedModule.withEntryComponents()
  ],
  declarations: [
    AdminWorkflowPageComponent,
    SupervisionOrderGroupSelectorComponent,
    SupervisionOrderStatusComponent,
    WorkflowItemAdminWorkflowActionsComponent,
    WorkspaceItemAdminWorkflowActionsComponent,
    ...ENTRY_COMPONENTS
  ],
  exports: [
    AdminWorkflowPageComponent
  ]
})
export class AdminWorkflowModuleModule {
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
