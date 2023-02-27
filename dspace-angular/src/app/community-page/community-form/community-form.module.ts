import { NgModule } from '@angular/core';

import { CommunityFormComponent } from './community-form.component';
import { SharedModule } from '../../shared/shared.module';
import { ComcolModule } from '../../shared/comcol/comcol.module';
import { FormModule } from '../../shared/form/form.module';

@NgModule({
  imports: [
    ComcolModule,
    FormModule,
    SharedModule
  ],
  declarations: [
    CommunityFormComponent,
  ],
  exports: [
    CommunityFormComponent
  ]
})
export class CommunityFormModule {

}
