import { Component } from '@angular/core';
import { ProfilePageComponent as BaseComponent } from '../../../../app/profile-page/profile-page.component';

@Component({
  selector: 'ds-profile-page',
  // styleUrls: ['./profile-page.component.scss'],
  styleUrls: ['../../../../app/profile-page/profile-page.component.scss'],
  // templateUrl: './profile-page.component.html'
  templateUrl: '../../../../app/profile-page/profile-page.component.html'
})
/**
 * Component for a user to edit their profile information
 */
export class ProfilePageComponent extends BaseComponent {

}
