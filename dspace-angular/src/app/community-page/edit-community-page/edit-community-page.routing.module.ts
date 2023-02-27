import { EditCommunityPageComponent } from './edit-community-page.component';
import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommunityMetadataComponent } from './community-metadata/community-metadata.component';
import { CommunityRolesComponent } from './community-roles/community-roles.component';
import { CommunityCurateComponent } from './community-curate/community-curate.component';
import { I18nBreadcrumbResolver } from '../../core/breadcrumbs/i18n-breadcrumb.resolver';
import { CommunityAuthorizationsComponent } from './community-authorizations/community-authorizations.component';
import { ResourcePolicyTargetResolver } from '../../shared/resource-policies/resolvers/resource-policy-target.resolver';
import { ResourcePolicyCreateComponent } from '../../shared/resource-policies/create/resource-policy-create.component';
import { ResourcePolicyResolver } from '../../shared/resource-policies/resolvers/resource-policy.resolver';
import { ResourcePolicyEditComponent } from '../../shared/resource-policies/edit/resource-policy-edit.component';
import { CommunityAdministratorGuard } from '../../core/data/feature-authorization/feature-authorization-guard/community-administrator.guard';

/**
 * Routing module that handles the routing for the Edit Community page administrator functionality
 */
@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        resolve: {
          breadcrumb: I18nBreadcrumbResolver
        },
        data: { breadcrumbKey: 'community.edit' },
        component: EditCommunityPageComponent,
        canActivate: [CommunityAdministratorGuard],
        children: [
          {
            path: '',
            redirectTo: 'metadata',
            pathMatch: 'full'
          },
          {
            path: 'metadata',
            component: CommunityMetadataComponent,
            data: {
              title: 'community.edit.tabs.metadata.title',
              hideReturnButton: true,
              showBreadcrumbs: true
            }
          },
          {
            path: 'roles',
            component: CommunityRolesComponent,
            data: { title: 'community.edit.tabs.roles.title', showBreadcrumbs: true }
          },
          {
            path: 'curate',
            component: CommunityCurateComponent,
            data: { title: 'community.edit.tabs.curate.title', showBreadcrumbs: true }
          },
          /*{
            path: 'authorizations',
            component: CommunityAuthorizationsComponent,
            data: { title: 'community.edit.tabs.authorizations.title', showBreadcrumbs: true }
          },*/
          {
            path: 'authorizations',
            data: { showBreadcrumbs: true },
            children: [
              {
                path: 'create',
                resolve: {
                  resourcePolicyTarget: ResourcePolicyTargetResolver
                },
                component: ResourcePolicyCreateComponent,
                data: { title: 'resource-policies.create.page.title' }
              },
              {
                path: 'edit',
                resolve: {
                  resourcePolicy: ResourcePolicyResolver
                },
                component: ResourcePolicyEditComponent,
                data: { title: 'resource-policies.edit.page.title' }
              },
              {
                path: '',
                component: CommunityAuthorizationsComponent,
                data: { title: 'community.edit.tabs.authorizations.title', showBreadcrumbs: true, hideReturnButton: true }
              }
            ]
          }
        ]
      }
    ])
  ],
  providers: [
    ResourcePolicyResolver,
    ResourcePolicyTargetResolver
  ]
})
export class EditCommunityPageRoutingModule {

}
