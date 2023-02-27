import {of as observableOf } from 'rxjs';

export class SidebarServiceStub {
  isCollapsed = observableOf(true);

  collapse(): void {
    this.isCollapsed = observableOf(true);
  }

  expand(): void {
    this.isCollapsed = observableOf(false);
  }

}
