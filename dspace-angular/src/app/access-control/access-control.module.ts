import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared/shared.module';
import { AccessControlRoutingModule } from './access-control-routing.module';
import { EPeopleRegistryComponent } from './epeople-registry/epeople-registry.component';
import { EPersonFormComponent } from './epeople-registry/eperson-form/eperson-form.component';
import { GroupFormComponent } from './group-registry/group-form/group-form.component';
import { MembersListComponent } from './group-registry/group-form/members-list/members-list.component';
import { SubgroupsListComponent } from './group-registry/group-form/subgroup-list/subgroups-list.component';
import { GroupsRegistryComponent } from './group-registry/groups-registry.component';
import { FormModule } from '../shared/form/form.module';
import { DYNAMIC_ERROR_MESSAGES_MATCHER, DynamicErrorMessagesMatcher } from '@ng-dynamic-forms/core';
import { AbstractControl } from '@angular/forms';

/**
 * Condition for displaying error messages on email form field
 */
export const ValidateEmailErrorStateMatcher: DynamicErrorMessagesMatcher =
  (control: AbstractControl, model: any, hasFocus: boolean) => {
    return (control.touched && !hasFocus) || (control.errors?.emailTaken && hasFocus);
  };

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    RouterModule,
    AccessControlRoutingModule,
    FormModule,
  ],
  exports: [
    MembersListComponent,
  ],
  declarations: [
    EPeopleRegistryComponent,
    EPersonFormComponent,
    GroupsRegistryComponent,
    GroupFormComponent,
    SubgroupsListComponent,
    MembersListComponent,
  ],
  providers: [
    {
      provide: DYNAMIC_ERROR_MESSAGES_MATCHER,
      useValue: ValidateEmailErrorStateMatcher
    },
  ]
})
/**
 * This module handles all components related to the access control pages
 */
export class AccessControlModule {

}
