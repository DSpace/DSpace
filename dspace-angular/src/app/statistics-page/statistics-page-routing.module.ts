import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { I18nBreadcrumbResolver } from '../core/breadcrumbs/i18n-breadcrumb.resolver';
import { I18nBreadcrumbsService } from '../core/breadcrumbs/i18n-breadcrumbs.service';
import { StatisticsPageModule } from './statistics-page.module';
import { CollectionPageResolver } from '../collection-page/collection-page.resolver';
import { CommunityPageResolver } from '../community-page/community-page.resolver';
import { ThemedCollectionStatisticsPageComponent } from './collection-statistics-page/themed-collection-statistics-page.component';
import { ThemedCommunityStatisticsPageComponent } from './community-statistics-page/themed-community-statistics-page.component';
import { ThemedItemStatisticsPageComponent } from './item-statistics-page/themed-item-statistics-page.component';
import { ThemedSiteStatisticsPageComponent } from './site-statistics-page/themed-site-statistics-page.component';
import { ItemResolver } from '../item-page/item.resolver';
import { StatisticsAdministratorGuard } from '../core/data/feature-authorization/feature-authorization-guard/statistics-administrator.guard';

@NgModule({
  imports: [
    StatisticsPageModule,
    RouterModule.forChild([
      {
        path: '',
        resolve: {
          breadcrumb: I18nBreadcrumbResolver
        },
        data: {
          title: 'statistics.title',
          breadcrumbKey: 'statistics'
        },
        children: [
          {
            path: '',
            component: ThemedSiteStatisticsPageComponent,
          },
        ],
        canActivate: [StatisticsAdministratorGuard]
      },
      {
        path: `items/:id`,
        resolve: {
          scope: ItemResolver,
          breadcrumb: I18nBreadcrumbResolver
        },
        data: {
          title: 'statistics.title',
          breadcrumbKey: 'statistics'
        },
        component: ThemedItemStatisticsPageComponent,
        canActivate: [StatisticsAdministratorGuard]
      },
      {
        path: `collections/:id`,
        resolve: {
          scope: CollectionPageResolver,
          breadcrumb: I18nBreadcrumbResolver
        },
        data: {
          title: 'statistics.title',
          breadcrumbKey: 'statistics'
        },
        component: ThemedCollectionStatisticsPageComponent,
        canActivate: [StatisticsAdministratorGuard]
      },
      {
        path: `communities/:id`,
        resolve: {
          scope: CommunityPageResolver,
          breadcrumb: I18nBreadcrumbResolver
        },
        data: {
          title: 'statistics.title',
          breadcrumbKey: 'statistics'
        },
        component: ThemedCommunityStatisticsPageComponent,
        canActivate: [StatisticsAdministratorGuard]
      },
    ]
    )
  ],
  providers: [
    I18nBreadcrumbResolver,
    I18nBreadcrumbsService,
    CollectionPageResolver,
    CommunityPageResolver,
    ItemResolver
  ]
})
export class StatisticsPageRoutingModule {
}
