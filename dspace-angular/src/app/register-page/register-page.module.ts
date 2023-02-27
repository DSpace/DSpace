import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared/shared.module';
import { RegisterPageRoutingModule } from './register-page-routing.module';
import { RegisterEmailComponent } from './register-email/register-email.component';
import { CreateProfileComponent } from './create-profile/create-profile.component';
import { RegisterEmailFormModule } from '../register-email-form/register-email-form.module';
import { ProfilePageModule } from '../profile-page/profile-page.module';
import { ThemedCreateProfileComponent } from './create-profile/themed-create-profile.component';
import { ThemedRegisterEmailComponent } from './register-email/themed-register-email.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RegisterPageRoutingModule,
    RegisterEmailFormModule,
    ProfilePageModule,
  ],
  declarations: [
    RegisterEmailComponent,
    ThemedRegisterEmailComponent,
    CreateProfileComponent,
    ThemedCreateProfileComponent
  ],
  providers: []
})

/**
 * Module related to components used to register a new user
 */
export class RegisterPageModule {

}
