import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SubscriptionsPageModule } from './subscriptions-page.module';
import { SubscriptionsPageComponent } from './subscriptions-page.component';


@NgModule({
  imports: [
    SubscriptionsPageModule,
    RouterModule.forChild([
        {
          path: '',
          data: {
            title: 'subscriptions.title',
          },
          children: [
            {
              path: '',
              component: SubscriptionsPageComponent,
            },
          ]
        },
    ])
  ]
})
export class SubscriptionsPageRoutingModule {
}
