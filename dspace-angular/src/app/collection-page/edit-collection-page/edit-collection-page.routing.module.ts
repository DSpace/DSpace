import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { CollectionItemMapperComponent } from '../collection-item-mapper/collection-item-mapper.component';
import { EditCollectionPageComponent } from './edit-collection-page.component';
import { CollectionMetadataComponent } from './collection-metadata/collection-metadata.component';
import { CollectionRolesComponent } from './collection-roles/collection-roles.component';
import { CollectionSourceComponent } from './collection-source/collection-source.component';
import { CollectionCurateComponent } from './collection-curate/collection-curate.component';
import { CollectionAuthorizationsComponent } from './collection-authorizations/collection-authorizations.component';
import { I18nBreadcrumbResolver } from '../../core/breadcrumbs/i18n-breadcrumb.resolver';
import { ResourcePolicyTargetResolver } from '../../shared/resource-policies/resolvers/resource-policy-target.resolver';
import { ResourcePolicyCreateComponent } from '../../shared/resource-policies/create/resource-policy-create.component';
import { ResourcePolicyResolver } from '../../shared/resource-policies/resolvers/resource-policy.resolver';
import { ResourcePolicyEditComponent } from '../../shared/resource-policies/edit/resource-policy-edit.component';
import { CollectionAdministratorGuard } from '../../core/data/feature-authorization/feature-authorization-guard/collection-administrator.guard';

/**
 * Routing module that handles the routing for the Edit Collection page administrator functionality
 */
@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        resolve: {
          breadcrumb: I18nBreadcrumbResolver
        },
        data: { breadcrumbKey: 'collection.edit' },
        component: EditCollectionPageComponent,
        canActivate: [CollectionAdministratorGuard],
        children: [
          {
            path: '',
            redirectTo: 'metadata',
            pathMatch: 'full'
          },
          {
            path: 'metadata',
            component: CollectionMetadataComponent,
            data: {
              title: 'collection.edit.tabs.metadata.title',
              hideReturnButton: true,
              showBreadcrumbs: true
            }
          },
          {
            path: 'roles',
            component: CollectionRolesComponent,
            data: { title: 'collection.edit.tabs.roles.title', showBreadcrumbs: true }
          },
          {
            path: 'source',
            component: CollectionSourceComponent,
            data: { title: 'collection.edit.tabs.source.title', showBreadcrumbs: true }
          },
          {
            path: 'curate',
            component: CollectionCurateComponent,
            data: { title: 'collection.edit.tabs.curate.title', showBreadcrumbs: true }
          },
/*          {
            path: 'authorizations',
            component: CollectionAuthorizationsComponent,
            data: { title: 'collection.edit.tabs.authorizations.title', showBreadcrumbs: true }
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
                component: CollectionAuthorizationsComponent,
                data: { title: 'collection.edit.tabs.authorizations.title', showBreadcrumbs: true }
              }
            ]
          },
          {
            path: 'mapper',
            component: CollectionItemMapperComponent,
            data: { title: 'collection.edit.tabs.item-mapper.title', hideReturnButton: true, showBreadcrumbs: true }
          },
        ]
      }
    ])
  ],
  providers: [
    ResourcePolicyResolver,
    ResourcePolicyTargetResolver
  ]
})
export class EditCollectionPageRoutingModule {

}
