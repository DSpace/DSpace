import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
  SiteAdministratorGuard
} from '../core/data/feature-authorization/feature-authorization-guard/site-administrator.guard';
import { SystemWideAlertFormComponent } from './alert-form/system-wide-alert-form.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        canActivate: [SiteAdministratorGuard],
        component: SystemWideAlertFormComponent,
      },

    ])
  ]
})
export class SystemWideAlertRoutingModule {

}
