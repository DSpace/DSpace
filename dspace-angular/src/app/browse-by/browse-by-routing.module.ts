import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { BrowseByGuard } from './browse-by-guard';
import { BrowseByDSOBreadcrumbResolver } from './browse-by-dso-breadcrumb.resolver';
import { BrowseByI18nBreadcrumbResolver } from './browse-by-i18n-breadcrumb.resolver';
import { ThemedBrowseBySwitcherComponent } from './browse-by-switcher/themed-browse-by-switcher.component';
import { DSOEditMenuResolver } from '../shared/dso-page/dso-edit-menu.resolver';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        resolve: {
          breadcrumb: BrowseByDSOBreadcrumbResolver,
          menu: DSOEditMenuResolver
        },
        children: [
          {
            path: ':id',
            component: ThemedBrowseBySwitcherComponent,
            canActivate: [BrowseByGuard],
            resolve: { breadcrumb: BrowseByI18nBreadcrumbResolver },
            data: { title: 'browse.title.page', breadcrumbKey: 'browse.metadata' }
          }
        ]
      }])
  ],
  providers: [
    BrowseByI18nBreadcrumbResolver,
    BrowseByDSOBreadcrumbResolver
  ]
})
export class BrowseByRoutingModule {

}
