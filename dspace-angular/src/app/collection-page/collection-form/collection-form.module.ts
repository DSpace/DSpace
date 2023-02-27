import { NgModule } from '@angular/core';

import { CollectionFormComponent } from './collection-form.component';
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
    CollectionFormComponent,
  ],
  exports: [
    CollectionFormComponent
  ]
})
export class CollectionFormModule {

}
