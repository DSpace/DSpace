import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { SidebarState } from './sidebar.reducer';
import { createSelector, select, Store } from '@ngrx/store';
import { SidebarCollapseAction, SidebarExpandAction } from './sidebar.actions';
import { AppState } from '../../app.reducer';
import { HostWindowService } from '../host-window.service';
import { map } from 'rxjs/operators';

const sidebarStateSelector = (state: AppState) => state.sidebar;
const sidebarCollapsedSelector = createSelector(sidebarStateSelector, (sidebar: SidebarState) => sidebar.sidebarCollapsed);

/**
 * Service that performs all actions that have to do with the sidebar
 */
@Injectable()
export class SidebarService {
  /**
   * Emits true is the current screen size is mobile
   */
  private isXsOrSm$: Observable<boolean>;

  /**
   * Emits true is the sidebar's state in the store is currently collapsed
   */
  private isCollapsedInStore: Observable<boolean>;

  constructor(private store: Store<AppState>, private windowService: HostWindowService) {
    this.isXsOrSm$ = this.windowService.isXsOrSm();
    this.isCollapsedInStore = this.store.pipe(select(sidebarCollapsedSelector));
  }

  /**
   * Checks if the sidebar should currently be collapsed
   * @returns {Observable<boolean>} Emits true if the user's screen size is mobile or when the state in the store is currently collapsed
   */
  get isCollapsed(): Observable<boolean> {
    return observableCombineLatest(
      this.isXsOrSm$,
      this.isCollapsedInStore
    ).pipe(
      map(([mobile, store]) => mobile ? store : true)
    );
  }

  /**
   * Dispatches a collapse action to the store
   */
  public collapse(): void {
    this.store.dispatch(new SidebarCollapseAction());
  }

  /**
   * Dispatches an expand action to the store
   */
  public expand(): void {
    this.store.dispatch(new SidebarExpandAction());
  }
}
