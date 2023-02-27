import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '../app.reducer';
import { hasValue } from '../shared/empty.util';
import { Observable, Subscription } from 'rxjs';
import { MenuService } from '../shared/menu/menu.service';
import { MenuID } from '../shared/menu/menu-id.model';

/**
 * This component represents a wrapper for the horizontal navbar and the header
 */
@Component({
  selector: 'ds-header-navbar-wrapper',
  styleUrls: ['header-navbar-wrapper.component.scss'],
  templateUrl: 'header-navbar-wrapper.component.html',
})
export class HeaderNavbarWrapperComponent implements OnInit, OnDestroy {
  @HostBinding('class.open') isOpen = false;
  private sub: Subscription;
  public isNavBarCollapsed: Observable<boolean>;
  menuID = MenuID.PUBLIC;

  constructor(
    private store: Store<AppState>,
    private menuService: MenuService
  ) {
  }

  ngOnInit(): void {
    this.isNavBarCollapsed = this.menuService.isMenuCollapsed(this.menuID);
    this.sub = this.isNavBarCollapsed.subscribe((isCollapsed) => this.isOpen = !isCollapsed);
  }

  ngOnDestroy() {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }
}
