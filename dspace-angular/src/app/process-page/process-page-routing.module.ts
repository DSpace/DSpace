import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { NewProcessComponent } from './new/new-process.component';
import { ProcessOverviewComponent } from './overview/process-overview.component';
import { ProcessPageResolver } from './process-page.resolver';
import { ProcessDetailComponent } from './detail/process-detail.component';
import { I18nBreadcrumbResolver } from '../core/breadcrumbs/i18n-breadcrumb.resolver';
import { ProcessBreadcrumbResolver } from './process-breadcrumb.resolver';
import { AuthenticatedGuard } from '../core/auth/authenticated.guard';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        resolve: { breadcrumb: I18nBreadcrumbResolver },
        data: { breadcrumbKey: 'process.overview' },
        canActivate: [AuthenticatedGuard],
        children: [
          {
            path: '',
            component: ProcessOverviewComponent,
            data: { title: 'process.overview.title' },
          },
          {
            path: 'new',
            component: NewProcessComponent,
            resolve: { breadcrumb: I18nBreadcrumbResolver },
            data: { title: 'process.new.title', breadcrumbKey: 'process.new' }
          },
          {
            path: ':id',
            component: ProcessDetailComponent,
            resolve: {
              process: ProcessPageResolver,
              breadcrumb: ProcessBreadcrumbResolver
            }
          }
        ]
      },

    ])
  ],
  providers: [
    ProcessPageResolver
  ]
})
export class ProcessPageRoutingModule {

}
