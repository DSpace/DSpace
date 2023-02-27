import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

import { SubscriptionViewComponent } from './subscription-view/subscription-view.component';
import { SubscriptionModalComponent } from './subscription-modal/subscription-modal.component';
import { TranslateModule } from '@ngx-translate/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../shared.module';
import { NgbModalModule } from '@ng-bootstrap/ng-bootstrap';

const COMPONENTS = [
  SubscriptionViewComponent,
  SubscriptionModalComponent
];

@NgModule({
  declarations: [
    ...COMPONENTS
  ],
  imports: [
    CommonModule,
    NgbModalModule,
    ReactiveFormsModule,
    TranslateModule,
    RouterModule,
    SharedModule
  ],
  exports: [
    ...COMPONENTS
  ]
})
export class SubscriptionsModule {
}
