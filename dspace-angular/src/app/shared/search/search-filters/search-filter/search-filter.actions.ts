/* eslint-disable max-classes-per-file */
import { Action } from '@ngrx/store';

import { type } from '../../../ngrx/type';
import { SearchFilterConfig } from '../../models/search-filter-config.model';

/**
 * For each action type in an action group, make a simple
 * enum object for all of this group's action types.
 *
 * The 'type' utility function coerces strings into string
 * literal types and runs a simple check to guarantee all
 * action types in the application are unique.
 */
export const SearchFilterActionTypes = {
  COLLAPSE: type('dspace/search-filter/COLLAPSE'),
  INITIALIZE: type('dspace/search-filter/INITIALIZE'),
  EXPAND: type('dspace/search-filter/EXPAND'),
  TOGGLE: type('dspace/search-filter/TOGGLE'),
  DECREMENT_PAGE: type('dspace/search-filter/DECREMENT_PAGE'),
  INCREMENT_PAGE: type('dspace/search-filter/INCREMENT_PAGE'),
  RESET_PAGE: type('dspace/search-filter/RESET_PAGE')
};

export class SearchFilterAction implements Action {
  /**
   * Name of the filter the action is performed on, used to identify the filter
   */
  filterName: string;

  /**
   * Type of action that will be performed
   */
  type;

  /**
   * Initialize with the filter's name
   * @param {string} name of the filter
   */
  constructor(name: string) {
    this.filterName = name;
  }
}

/**
 * Used to collapse a filter
 */
export class SearchFilterCollapseAction extends SearchFilterAction {
  type = SearchFilterActionTypes.COLLAPSE;
}

/**
 * Used to expand a filter
 */
export class SearchFilterExpandAction extends SearchFilterAction {
  type = SearchFilterActionTypes.EXPAND;
}

/**
 * Used to collapse a filter when it's expanded and expand it when it's collapsed
 */
export class SearchFilterToggleAction extends SearchFilterAction {
  type = SearchFilterActionTypes.TOGGLE;
}

/**
 * Used to set the initial state of a filter
 */
export class SearchFilterInitializeAction extends SearchFilterAction {
  type = SearchFilterActionTypes.INITIALIZE;
  initiallyExpanded;
  constructor(filter: SearchFilterConfig) {
    super(filter.name);
    this.initiallyExpanded = filter.isOpenByDefault;
  }
}

/**
 * Used to set the state of a filter to the previous page
 */
export class SearchFilterDecrementPageAction extends SearchFilterAction {
  type = SearchFilterActionTypes.DECREMENT_PAGE;
}

/**
 * Used to set the state of a filter to the next page
 */
export class SearchFilterIncrementPageAction extends SearchFilterAction {
  type = SearchFilterActionTypes.INCREMENT_PAGE;
}

/**
 * Used to set the state of a filter to the first page
 */
export class SearchFilterResetPageAction extends SearchFilterAction {
  type = SearchFilterActionTypes.RESET_PAGE;
}
