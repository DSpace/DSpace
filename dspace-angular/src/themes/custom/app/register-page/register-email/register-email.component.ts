import { Component } from '@angular/core';
import { RegisterEmailComponent as BaseComponent } from '../../../../../app/register-page/register-email/register-email.component';

@Component({
  selector: 'ds-register-email',
  // styleUrls: ['./register-email.component.scss'],
  styleUrls: ['../../../../../app/register-page/register-email/register-email.component.scss'],
  // templateUrl: './register-email.component.html'
  templateUrl: '../../../../../app/register-page/register-email/register-email.component.html'
})
/**
 * Component responsible the email registration step when registering as a new user
 */
export class RegisterEmailComponent extends BaseComponent {
}
