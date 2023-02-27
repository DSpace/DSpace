import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { TranslateModule } from '@ngx-translate/core';

import { ResourcePoliciesComponent } from './resource-policies.component';
import { ResourcePolicyFormComponent } from './form/resource-policy-form.component';
import { ResourcePolicyEditComponent } from './edit/resource-policy-edit.component';
import { ResourcePolicyCreateComponent } from './create/resource-policy-create.component';
import { FormModule } from '../form/form.module';
import { ResourcePolicyResolver } from './resolvers/resource-policy.resolver';
import { ResourcePolicyTargetResolver } from './resolvers/resource-policy-target.resolver';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SharedModule } from '../shared.module';
import { ResourcePolicyEntryComponent } from './entry/resource-policy-entry.component';

const COMPONENTS = [
  ResourcePoliciesComponent,
  ResourcePolicyEntryComponent,
  ResourcePolicyFormComponent,
  ResourcePolicyEditComponent,
  ResourcePolicyCreateComponent,
];

const PROVIDERS = [
  ResourcePolicyResolver,
  ResourcePolicyTargetResolver
];

@NgModule({
  declarations: [
    ...COMPONENTS
  ],
  imports: [
    NgbModule,
    CommonModule,
    FormModule,
    TranslateModule,
    SharedModule
  ],
  providers: [
    ...PROVIDERS
  ],
  exports: [
    ...COMPONENTS
  ]
})
export class ResourcePoliciesModule { }
