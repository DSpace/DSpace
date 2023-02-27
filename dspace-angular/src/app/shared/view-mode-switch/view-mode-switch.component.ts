import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';

import { Subscription } from 'rxjs';

import { SearchService } from '../../core/shared/search/search.service';
import { ViewMode } from '../../core/shared/view-mode.model';
import { isEmpty, isNotEmpty } from '../empty.util';
import { currentPath } from '../utils/route.utils';
import { Router } from '@angular/router';
import { filter } from 'rxjs/operators';

/**
 * Component to switch between list and grid views.
 */
@Component({
  selector: 'ds-view-mode-switch',
  styleUrls: ['./view-mode-switch.component.scss'],
  templateUrl: './view-mode-switch.component.html'
})
export class ViewModeSwitchComponent implements OnInit, OnDestroy {

  /**
   * True when the search component should show results on the current page
   */
  @Input() inPlaceSearch;

  /**
   * List of available view mode
   */
  @Input() viewModeList: ViewMode[];

  /**
   * The current view mode
   */
  currentMode: ViewMode = ViewMode.ListElement;

  /**
   * All available view modes
   */
  viewModeEnum = ViewMode;

  /**
   * Subscription to unsubscribe OnDestroy
   * @private
   */
  private sub: Subscription;

  /**
   * Emits event when the user select a new view mode
   */
  @Output() changeViewMode: EventEmitter<ViewMode> = new EventEmitter<ViewMode>();

  constructor(private searchService: SearchService, private router: Router) {
  }

  /**
   * Initialize the instance variables
   */
  ngOnInit(): void {
    if (isEmpty(this.viewModeList)) {
      this.viewModeList = [ViewMode.ListElement, ViewMode.GridElement];
    }

    this.sub = this.searchService.getViewMode().pipe(
      filter((viewMode: ViewMode) => isNotEmpty(viewMode))
    ).subscribe((viewMode: ViewMode) => {
      this.currentMode = viewMode;
    });
  }

  /**
   * Switch view modes
   * @param viewMode The new view mode
   */
  switchViewTo(viewMode: ViewMode) {
    if (viewMode !== this.currentMode) {
      this.changeViewMode.emit(viewMode);
    }
    this.searchService.setViewMode(viewMode, this.getSearchLinkParts());
  }

  ngOnDestroy() {
    if (this.sub !== undefined) {
      this.sub.unsubscribe();
    }
  }

  /**
   * Whether or not to show a certain view mode
   * @param viewMode The view mode to check for
   */
  isToShow(viewMode: ViewMode) {
    return this.viewModeList && this.viewModeList.includes(viewMode);
  }

  /**
   * @returns {string} The base path to the search page, or the current page when inPlaceSearch is true
   */
  public getSearchLink(): string {
    if (this.inPlaceSearch) {
      return currentPath(this.router);
    }
    return this.searchService.getSearchLink();
  }

  /**
   * @returns {string[]} The base path to the search page, or the current page when inPlaceSearch is true, split in separate pieces
   */
  public getSearchLinkParts(): string[] {
    if (this.searchService) {
      return [];
    }
    return this.getSearchLink().split('/');
  }

}
