import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthenticatedGuard } from '../core/auth/authenticated.guard';
import { ThemedLogoutPageComponent } from './themed-logout-page.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        canActivate: [AuthenticatedGuard],
        path: '',
        component: ThemedLogoutPageComponent,
        data: { title: 'logout.title' }
      }
    ])
  ]
})
export class LogoutPageRoutingModule { }
