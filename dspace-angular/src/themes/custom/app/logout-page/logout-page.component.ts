import { Component } from '@angular/core';
import { LogoutPageComponent as BaseComponent} from '../../../../app/logout-page/logout-page.component';

@Component({
  selector: 'ds-logout-page',
  // styleUrls: ['./logout-page.component.scss'],
  styleUrls: ['../../../../app/logout-page/logout-page.component.scss'],
  // templateUrl: './logout-page.component.html'
  templateUrl: '../../../../app/logout-page/logout-page.component.html'
})
export class LogoutPageComponent extends BaseComponent {
}
