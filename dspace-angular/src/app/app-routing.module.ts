import { NgModule } from '@angular/core';
import { RouterModule, NoPreloading } from '@angular/router';
import { AuthBlockingGuard } from './core/auth/auth-blocking.guard';

import { AuthenticatedGuard } from './core/auth/authenticated.guard';
import {
  SiteAdministratorGuard
} from './core/data/feature-authorization/feature-authorization-guard/site-administrator.guard';
import {
  ACCESS_CONTROL_MODULE_PATH,
  ADMIN_MODULE_PATH,
  BITSTREAM_MODULE_PATH,
  ERROR_PAGE,
  FORBIDDEN_PATH,
  FORGOT_PASSWORD_PATH,
  HEALTH_PAGE_PATH,
  INFO_MODULE_PATH,
  INTERNAL_SERVER_ERROR,
  LEGACY_BITSTREAM_MODULE_PATH,
  PROFILE_MODULE_PATH,
  REGISTER_PATH,
  REQUEST_COPY_MODULE_PATH,
  WORKFLOW_ITEM_MODULE_PATH,
} from './app-routing-paths';
import { COLLECTION_MODULE_PATH } from './collection-page/collection-page-routing-paths';
import { COMMUNITY_MODULE_PATH } from './community-page/community-page-routing-paths';
import { ITEM_MODULE_PATH } from './item-page/item-page-routing-paths';
import { PROCESS_MODULE_PATH } from './process-page/process-page-routing.paths';
import { ReloadGuard } from './core/reload/reload.guard';
import { EndUserAgreementCurrentUserGuard } from './core/end-user-agreement/end-user-agreement-current-user.guard';
import { SiteRegisterGuard } from './core/data/feature-authorization/feature-authorization-guard/site-register.guard';
import { ThemedPageNotFoundComponent } from './pagenotfound/themed-pagenotfound.component';
import { ThemedForbiddenComponent } from './forbidden/themed-forbidden.component';
import {
  GroupAdministratorGuard
} from './core/data/feature-authorization/feature-authorization-guard/group-administrator.guard';
import {
  ThemedPageInternalServerErrorComponent
} from './page-internal-server-error/themed-page-internal-server-error.component';
import { ServerCheckGuard } from './core/server-check/server-check.guard';
import { MenuResolver } from './menu.resolver';
import { ThemedPageErrorComponent } from './page-error/themed-page-error.component';

@NgModule({
  imports: [
    RouterModule.forRoot([
      { path: INTERNAL_SERVER_ERROR, component: ThemedPageInternalServerErrorComponent },
      { path: ERROR_PAGE , component: ThemedPageErrorComponent },
      {
        path: '',
        canActivate: [AuthBlockingGuard],
        canActivateChild: [ServerCheckGuard],
        resolve: [MenuResolver],
        children: [
          { path: '', redirectTo: '/home', pathMatch: 'full' },
          {
            path: 'reload/:rnd',
            component: ThemedPageNotFoundComponent,
            pathMatch: 'full',
            canActivate: [ReloadGuard]
          },
          {
            path: 'home',
            loadChildren: () => import('./home-page/home-page.module')
              .then((m) => m.HomePageModule),
            data: { showBreadcrumbs: false },
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'community-list',
            loadChildren: () => import('./community-list-page/community-list-page.module')
              .then((m) => m.CommunityListPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'id',
            loadChildren: () => import('./lookup-by-id/lookup-by-id.module')
              .then((m) => m.LookupIdModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'handle',
            loadChildren: () => import('./lookup-by-id/lookup-by-id.module')
              .then((m) => m.LookupIdModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: REGISTER_PATH,
            loadChildren: () => import('./register-page/register-page.module')
              .then((m) => m.RegisterPageModule),
            canActivate: [SiteRegisterGuard]
          },
          {
            path: FORGOT_PASSWORD_PATH,
            loadChildren: () => import('./forgot-password/forgot-password.module')
              .then((m) => m.ForgotPasswordModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: COMMUNITY_MODULE_PATH,
            loadChildren: () => import('./community-page/community-page.module')
              .then((m) => m.CommunityPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: COLLECTION_MODULE_PATH,
            loadChildren: () => import('./collection-page/collection-page.module')
              .then((m) => m.CollectionPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: ITEM_MODULE_PATH,
            loadChildren: () => import('./item-page/item-page.module')
              .then((m) => m.ItemPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'entities/:entity-type',
            loadChildren: () => import('./item-page/item-page.module')
              .then((m) => m.ItemPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: LEGACY_BITSTREAM_MODULE_PATH,
            loadChildren: () => import('./bitstream-page/bitstream-page.module')
              .then((m) => m.BitstreamPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: BITSTREAM_MODULE_PATH,
            loadChildren: () => import('./bitstream-page/bitstream-page.module')
              .then((m) => m.BitstreamPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'mydspace',
            loadChildren: () => import('./my-dspace-page/my-dspace-page.module')
              .then((m) => m.MyDSpacePageModule),
            canActivate: [AuthenticatedGuard, EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'search',
            loadChildren: () => import('./search-page/search-page-routing.module')
              .then((m) => m.SearchPageRoutingModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'browse',
            loadChildren: () => import('./browse-by/browse-by-page.module')
              .then((m) => m.BrowseByPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: ADMIN_MODULE_PATH,
            loadChildren: () => import('./admin/admin.module')
              .then((m) => m.AdminModule),
            canActivate: [SiteAdministratorGuard, EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'login',
            loadChildren: () => import('./login-page/login-page.module')
              .then((m) => m.LoginPageModule)
          },
          {
            path: 'logout',
            loadChildren: () => import('./logout-page/logout-page.module')
              .then((m) => m.LogoutPageModule)
          },
          {
            path: 'submit',
            loadChildren: () => import('./submit-page/submit-page.module')
              .then((m) => m.SubmitPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'import-external',
            loadChildren: () => import('./import-external-page/import-external-page.module')
              .then((m) => m.ImportExternalPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: 'workspaceitems',
            loadChildren: () => import('./workspaceitems-edit-page/workspaceitems-edit-page.module')
              .then((m) => m.WorkspaceitemsEditPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: WORKFLOW_ITEM_MODULE_PATH,
            loadChildren: () => import('./workflowitems-edit-page/workflowitems-edit-page.module')
              .then((m) => m.WorkflowItemsEditPageModule),
            canActivate: [EndUserAgreementCurrentUserGuard]
          },
          {
            path: PROFILE_MODULE_PATH,
            loadChildren: () => import('./profile-page/profile-page.module')
              .then((m) => m.ProfilePageModule),
            canActivate: [AuthenticatedGuard, EndUserAgreementCurrentUserGuard]
          },
          {
            path: PROCESS_MODULE_PATH,
            loadChildren: () => import('./process-page/process-page.module')
              .then((m) => m.ProcessPageModule),
            canActivate: [AuthenticatedGuard, EndUserAgreementCurrentUserGuard]
          },
          {
            path: INFO_MODULE_PATH,
            loadChildren: () => import('./info/info.module').then((m) => m.InfoModule)
          },
          {
            path: REQUEST_COPY_MODULE_PATH,
            loadChildren: () => import('./request-copy/request-copy.module').then((m) => m.RequestCopyModule),
            canActivate: [AuthenticatedGuard, EndUserAgreementCurrentUserGuard]
          },
          {
            path: FORBIDDEN_PATH,
            component: ThemedForbiddenComponent
          },
          {
            path: 'statistics',
            loadChildren: () => import('./statistics-page/statistics-page-routing.module')
              .then((m) => m.StatisticsPageRoutingModule)
          },
          {
            path: HEALTH_PAGE_PATH,
            loadChildren: () => import('./health-page/health-page.module')
              .then((m) => m.HealthPageModule)
          },
          {
            path: ACCESS_CONTROL_MODULE_PATH,
            loadChildren: () => import('./access-control/access-control.module').then((m) => m.AccessControlModule),
            canActivate: [GroupAdministratorGuard],
          },
          {
            path: 'subscriptions',
            loadChildren: () => import('./subscriptions-page/subscriptions-page-routing.module')
              .then((m) => m.SubscriptionsPageRoutingModule),
            canActivate: [AuthenticatedGuard]
          },
          { path: '**', pathMatch: 'full', component: ThemedPageNotFoundComponent },
        ]
      }
    ], {
      // enableTracing: true,
      useHash: false,
      scrollPositionRestoration: 'enabled',
      anchorScrolling: 'enabled',
      initialNavigation: 'enabledBlocking',
      preloadingStrategy: NoPreloading,
      onSameUrlNavigation: 'reload',
})
  ],
  exports: [RouterModule],
})
export class AppRoutingModule {

}
