import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../shared/shared.module';
import { RegisterEmailFormComponent } from './register-email-form.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    RegisterEmailFormComponent,
  ],
  providers: [],
  exports: [
    RegisterEmailFormComponent,
  ]
})

/**
 * The module that contains the components related to the email registration
 */
export class RegisterEmailFormModule {

}
