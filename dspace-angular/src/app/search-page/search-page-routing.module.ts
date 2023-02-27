import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ConfigurationSearchPageGuard } from './configuration-search-page.guard';
import { I18nBreadcrumbResolver } from '../core/breadcrumbs/i18n-breadcrumb.resolver';
import { I18nBreadcrumbsService } from '../core/breadcrumbs/i18n-breadcrumbs.service';
import { SearchPageModule } from './search-page.module';
import { ThemedSearchPageComponent } from './themed-search-page.component';
import { ThemedConfigurationSearchPageComponent } from './themed-configuration-search-page.component';

@NgModule({
  imports: [
    SearchPageModule,
    RouterModule.forChild([{
        path: '',
        resolve: { breadcrumb: I18nBreadcrumbResolver }, data: { title: 'search.title', breadcrumbKey: 'search' },
        children: [
          { path: '', component: ThemedSearchPageComponent },
          { path: ':configuration', component: ThemedConfigurationSearchPageComponent, canActivate: [ConfigurationSearchPageGuard] }
        ]
      }]
    )
  ],
  providers: [
    I18nBreadcrumbResolver,
    I18nBreadcrumbsService
  ]
})
export class SearchPageRoutingModule {
}
