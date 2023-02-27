import { Component } from '@angular/core';
import { CreateProfileComponent as BaseComponent } from '../../../../../app/register-page/create-profile/create-profile.component';

/**
 * Component that renders the create profile page to be used by a user registering through a token
 */
@Component({
  selector: 'ds-create-profile',
  // styleUrls: ['./create-profile.component.scss'],
  styleUrls: ['../../../../../app/register-page/create-profile/create-profile.component.scss'],
  // templateUrl: './create-profile.component.html'
  templateUrl: '../../../../../app/register-page/create-profile/create-profile.component.html'
})
export class CreateProfileComponent extends BaseComponent {
}
