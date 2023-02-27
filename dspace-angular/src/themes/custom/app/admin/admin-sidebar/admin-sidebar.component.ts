import { Component } from '@angular/core';
import { AdminSidebarComponent as BaseComponent } from '../../../../../app/admin/admin-sidebar/admin-sidebar.component';

/**
 * Component representing the admin sidebar
 */
@Component({
  selector: 'ds-admin-sidebar',
  // templateUrl: './admin-sidebar.component.html',
  templateUrl: '../../../../../app/admin/admin-sidebar/admin-sidebar.component.html',
  // styleUrls: ['./admin-sidebar.component.scss']
  styleUrls: ['../../../../../app/admin/admin-sidebar/admin-sidebar.component.scss']
})
export class AdminSidebarComponent extends BaseComponent {
}
