import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { GoogleRecaptchaComponent } from '../../shared/google-recaptcha/google-recaptcha.component';

import { GoogleRecaptchaService } from './google-recaptcha.service';

const PROVIDERS = [
  GoogleRecaptchaService
];

const COMPONENTS = [
  GoogleRecaptchaComponent
];

@NgModule({
  imports: [ CommonModule ],
  providers: [...PROVIDERS],
  declarations: [...COMPONENTS],
  exports: [...COMPONENTS]
})

/**
 * This module handles google recaptcha functionalities
 */
export class GoogleRecaptchaModule {}
