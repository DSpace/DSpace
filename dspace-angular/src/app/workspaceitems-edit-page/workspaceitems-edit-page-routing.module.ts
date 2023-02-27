import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { AuthenticatedGuard } from '../core/auth/authenticated.guard';
import { ThemedSubmissionEditComponent } from '../submission/edit/themed-submission-edit.component';
import { I18nBreadcrumbResolver } from '../core/breadcrumbs/i18n-breadcrumb.resolver';
import { ThemedFullItemPageComponent } from '../item-page/full/themed-full-item-page.component';
import { ItemFromWorkspaceResolver } from './item-from-workspace.resolver';
import { WorkspaceItemPageResolver } from './workspace-item-page.resolver';

@NgModule({
  imports: [
    RouterModule.forChild([
      { path: '', redirectTo: '/home', pathMatch: 'full' },
      {
        path: ':id',
        resolve: { wsi: WorkspaceItemPageResolver },
        children: [
          {
            canActivate: [AuthenticatedGuard],
            path: 'edit',
            component: ThemedSubmissionEditComponent,
            resolve: {
              breadcrumb: I18nBreadcrumbResolver
            },
            data: { title: 'submission.edit.title', breadcrumbKey: 'submission.edit' }
          },
          {
            canActivate: [AuthenticatedGuard],
            path: 'view',
            component: ThemedFullItemPageComponent,
            resolve: {
              dso: ItemFromWorkspaceResolver,
              breadcrumb: I18nBreadcrumbResolver
            },
            data: { title: 'workspace-item.view.title', breadcrumbKey: 'workspace-item.view' }
          }
        ]
      }
    ])
  ],
  providers: [WorkspaceItemPageResolver, ItemFromWorkspaceResolver]
})
/**
 * This module defines the default component to load when navigating to the workspaceitems edit page path
 */
export class WorkspaceitemsEditPageRoutingModule { }
