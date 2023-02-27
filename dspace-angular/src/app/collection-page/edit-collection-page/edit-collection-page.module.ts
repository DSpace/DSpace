import { NgModule } from '@angular/core';
import { EditCollectionPageComponent } from './edit-collection-page.component';
import { CommonModule } from '@angular/common';
import { SharedModule } from '../../shared/shared.module';
import { EditCollectionPageRoutingModule } from './edit-collection-page.routing.module';
import { CollectionMetadataComponent } from './collection-metadata/collection-metadata.component';
import { CollectionRolesComponent } from './collection-roles/collection-roles.component';
import { CollectionCurateComponent } from './collection-curate/collection-curate.component';
import { CollectionSourceComponent } from './collection-source/collection-source.component';
import { CollectionAuthorizationsComponent } from './collection-authorizations/collection-authorizations.component';
import { CollectionFormModule } from '../collection-form/collection-form.module';
import { CollectionSourceControlsComponent } from './collection-source/collection-source-controls/collection-source-controls.component';
import { ResourcePoliciesModule } from '../../shared/resource-policies/resource-policies.module';
import { FormModule } from '../../shared/form/form.module';
import { ComcolModule } from '../../shared/comcol/comcol.module';

/**
 * Module that contains all components related to the Edit Collection page administrator functionality
 */
@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    EditCollectionPageRoutingModule,
    CollectionFormModule,
    ResourcePoliciesModule,
    FormModule,
    ComcolModule,
  ],
  declarations: [
    EditCollectionPageComponent,
    CollectionMetadataComponent,
    CollectionRolesComponent,
    CollectionCurateComponent,
    CollectionSourceComponent,

    CollectionSourceControlsComponent,
    CollectionAuthorizationsComponent
  ]
})
export class EditCollectionPageModule {

}
