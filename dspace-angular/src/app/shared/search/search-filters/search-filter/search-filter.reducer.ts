import { SearchFilterAction, SearchFilterActionTypes, SearchFilterInitializeAction } from './search-filter.actions';

/**
 * Interface that represents the state for a single filters
 */
export interface SearchFilterState {
  filterCollapsed: boolean;
  page: number;
}

/**
 * Interface that represents the state for all available filters
 */
export interface SearchFiltersState {
  [name: string]: SearchFilterState;
}

const initialState: SearchFiltersState = Object.create(null);

/**
 * Performs a search filter action on the current state
 * @param {SearchFiltersState} state The state before the action is performed
 * @param {SearchFilterAction} action The action that should be performed
 * @returns {SearchFiltersState} The state after the action is performed
 */
export function filterReducer(state = initialState, action: SearchFilterAction): SearchFiltersState {

  switch (action.type) {

    case SearchFilterActionTypes.INITIALIZE: {
      const initAction = (action as SearchFilterInitializeAction);
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: !initAction.initiallyExpanded,
          page: 1
        }
      });
      return state;
    }

    case SearchFilterActionTypes.COLLAPSE: {
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: true,
          page: state[action.filterName].page
        }
      });
    }

    case SearchFilterActionTypes.EXPAND: {
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: false,
          page: state[action.filterName].page
        }
      });

    }

    case SearchFilterActionTypes.DECREMENT_PAGE: {
      const page = state[action.filterName].page - 1;
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: state[action.filterName].filterCollapsed,
          page: (page >= 1 ? page : 1)
        }
      });
    }

    case SearchFilterActionTypes.INCREMENT_PAGE: {
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: state[action.filterName].filterCollapsed,
          page: state[action.filterName].page + 1
        }
      });

    }
    case SearchFilterActionTypes.RESET_PAGE: {
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: state[action.filterName].filterCollapsed,
          page: 1
        }
      });

    }

    case SearchFilterActionTypes.TOGGLE: {
      return Object.assign({}, state, {
        [action.filterName]: {
          filterCollapsed: !state[action.filterName].filterCollapsed,
          page: state[action.filterName].page
        }
      });

    }

    default: {
      return state;
    }
  }
}
