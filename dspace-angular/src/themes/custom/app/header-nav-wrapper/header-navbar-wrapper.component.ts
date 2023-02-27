import { Component } from '@angular/core';
import { HeaderNavbarWrapperComponent as BaseComponent } from '../../../../app/header-nav-wrapper/header-navbar-wrapper.component';

/**
 * This component represents a wrapper for the horizontal navbar and the header
 */
@Component({
  selector: 'ds-header-navbar-wrapper',
  // styleUrls: ['header-navbar-wrapper.component.scss'],
  styleUrls: ['../../../../app/header-nav-wrapper/header-navbar-wrapper.component.scss'],
  // templateUrl: 'header-navbar-wrapper.component.html',
  templateUrl: '../../../../app/header-nav-wrapper/header-navbar-wrapper.component.html',
})
export class HeaderNavbarWrapperComponent extends BaseComponent {
}
