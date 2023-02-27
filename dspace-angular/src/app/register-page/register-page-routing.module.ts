import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ThemedRegisterEmailComponent } from './register-email/themed-register-email.component';
import { ItemPageResolver } from '../item-page/item-page.resolver';
import { EndUserAgreementCookieGuard } from '../core/end-user-agreement/end-user-agreement-cookie.guard';
import { ThemedCreateProfileComponent } from './create-profile/themed-create-profile.component';
import { RegistrationGuard } from './registration.guard';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        component: ThemedRegisterEmailComponent,
        data: {title: 'register-email.title'},
      },
      {
        path: ':token',
        component: ThemedCreateProfileComponent,
        canActivate: [
          RegistrationGuard,
          EndUserAgreementCookieGuard,
        ],
      }
    ])
  ],
  providers: [
    ItemPageResolver
  ]
})
/**
 * Module related to the navigation to components used to register a new user
 */
export class RegisterPageRoutingModule {
}
