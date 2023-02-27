import { first, map, switchMap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ROUTER_NAVIGATION } from '@ngrx/router-store';

import { HostWindowActionTypes } from '../shared/host-window.actions';
import {
  CollapseMenuAction,
  ExpandMenuPreviewAction,
  MenuActionTypes
} from '../shared/menu/menu.actions';
import { MenuService } from '../shared/menu/menu.service';
import { NoOpAction } from '../shared/ngrx/no-op.action';
import { MenuState } from '../shared/menu/menu-state.model';
import { MenuID } from '../shared/menu/menu-id.model';

@Injectable()
export class NavbarEffects {
  menuID = MenuID.PUBLIC;
  /**
   * Effect that collapses the public menu on window resize
   * @type {Observable<CollapseMenuAction>}
   */
   resize$ = createEffect(() => this.actions$
    .pipe(
      ofType(HostWindowActionTypes.RESIZE),
      map(() => new CollapseMenuAction(this.menuID))
    ));

  /**
   * Effect that collapses the public menu on reroute
   * @type {Observable<CollapseMenuAction>}
   */
   routeChange$ = createEffect(() => this.actions$
    .pipe(
      ofType(ROUTER_NAVIGATION),
      map(() => new CollapseMenuAction(this.menuID))
    ));
  /**
   * Effect that collapses the public menu when the admin sidebar opens
   * @type {Observable<CollapseMenuAction>}
   */
   openAdminSidebar$ = createEffect(() => this.actions$
    .pipe(
      ofType(MenuActionTypes.EXPAND_MENU_PREVIEW),
      switchMap((action: ExpandMenuPreviewAction) => {
        return this.menuService.getMenu(action.menuID).pipe(
          first(),
          map((menu: MenuState) => {
            if (menu.id === MenuID.ADMIN) {
              if (!menu.previewCollapsed && menu.collapsed) {
                return new CollapseMenuAction(MenuID.PUBLIC);
              }
            }
            return new NoOpAction();
          }));
      })
    ));
  constructor(private actions$: Actions, private menuService: MenuService) {

  }

}
