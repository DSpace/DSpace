import { Injectable } from '@angular/core';
import { AppState, keySelector } from '../../app.reducer';
import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import { AddAllCSSVariablesAction, AddCSSVariableAction, ClearCSSVariablesAction } from './css-variable.actions';
import { PaginationComponentOptions } from '../pagination/pagination-component-options.model';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { Observable } from 'rxjs';
import { hasValue, isNotEmpty } from '../empty.util';
import { KeyValuePair } from '../key-value-pair.model';
import { PageInfo } from '../../core/shared/page-info.model';
import { CSSVariablesState } from './css-variable.reducer';

/**
 * This service deals with adding and retrieving CSS variables to and from the store
 */
@Injectable({
  providedIn: 'root'
})
export class CSSVariableService {
  isSameDomain = (styleSheet) => {
    // Internal style blocks won't have an href value
    if (!styleSheet.href) {
      return true;
    }

    return styleSheet.href.indexOf(window.location.origin) === 0;
  };

  /*
   Determine if the given rule is a CSSStyleRule
   See: https://developer.mozilla.org/en-US/docs/Web/API/CSSRule#Type_constants
  */
  isStyleRule = (rule) => rule.type === 1;

  constructor(
    protected store: Store<AppState>) {
  }

  /**
   * Adds a CSS variable to the store
   * @param name The name/key of the CSS variable
   * @param value The value of the CSS variable
   */
  addCSSVariable(name: string, value: string) {
    this.store.dispatch(new AddCSSVariableAction(name, value));
  }

  /**
   * Adds multiples CSS variables to the store
   * @param variables The key-value pairs with the CSS variables to be added
   */
  addCSSVariables(variables: KeyValuePair<string, string>[]) {
    this.store.dispatch(new AddAllCSSVariablesAction(variables));
  }

  /**
   * Clears all CSS variables ƒrom the store
   */
  clearCSSVariables() {
    this.store.dispatch(new ClearCSSVariablesAction());
  }

  /**
   * Returns the value of a specific CSS key
   * @param name The name/key of the CSS value
   */
  getVariable(name: string): Observable<string> {
    return this.store.pipe(select(themeVariableByNameSelector(name)));
  }

  /**
   * Returns the CSSVariablesState of the store containing all variables
   */
  getAllVariables(): Observable<CSSVariablesState> {
    return this.store.pipe(select(themeVariablesSelector));
  }

  /**
   * Method to find CSS variables by their partially supplying their key. Case sensitive. Returns a paginated list of KeyValuePairs with CSS variables that match the query.
   * @param query The query to look for in the keys
   * @param paginationOptions The pagination options for the requested page
   */
  searchVariable(query: string, paginationOptions: PaginationComponentOptions): Observable<PaginatedList<KeyValuePair<string, string>>> {
    return this.store.pipe(select(themePaginatedVariablesByQuery(query, paginationOptions)));
  }

  /**
   * Get all custom properties on a page
   * @return array<KeyValuePair<string, string>>
   * ex; [{key: "--color-accent", value: "#b9f500"}, {key: "--color-text", value: "#252525"}, ...]
   */
  getCSSVariablesFromStylesheets(document: Document): KeyValuePair<string, string>[] {
    if (isNotEmpty(document.styleSheets)) {
      // styleSheets is array-like, so we convert it to an array.
      // Filter out any stylesheets not on this domain
      return [...document.styleSheets]
        .filter(this.isSameDomain)
        .reduce(
          (finalArr, sheet) =>
            finalArr.concat(
              // cssRules is array-like, so we convert it to an array
              [...sheet.cssRules].filter(this.isStyleRule).reduce((propValArr, rule: any) => {
                const props = [...rule.style]
                  .map((propName) => {
                      return {
                        key: propName.trim(),
                        value: rule.style.getPropertyValue(propName).trim()
                      } as KeyValuePair<string, string>;
                    }
                  )
                  // Discard any props that don't start with "--". Custom props are required to.
                  .filter(({ key }: KeyValuePair<string, string>) => key.indexOf('--') === 0);

                return [...propValArr, ...props];
              }, [])
            ),
          []
        );
    } else {
      return [];
    }
  }
}

const themeVariablesSelector = (state: AppState) => state.cssVariables;

const themeVariableByNameSelector = (name: string): MemoizedSelector<AppState, string> => {
  return keySelector<string>(name, themeVariablesSelector);
};

// Split this up into two memoized selectors so the query search gets cached separately from the pagination,
// since the entire list has to be retrieved every time anyway
const themePaginatedVariablesByQuery = (query: string, pagination: PaginationComponentOptions): MemoizedSelector<AppState, PaginatedList<KeyValuePair<string, string>>> => {
  return createSelector(themeVariablesByQuery(query), (pairs) => {
    if (hasValue(pairs)) {
      const { currentPage, pageSize } = pagination;
      const startIndex = (currentPage - 1) * pageSize;
      const endIndex = startIndex + pageSize;
      const pairsPage = pairs.slice(startIndex, endIndex);
      const totalPages = Math.ceil(pairs.length / pageSize);
      const pageInfo = new PageInfo({ currentPage, elementsPerPage: pageSize, totalElements: pairs.length, totalPages });
      return buildPaginatedList(pageInfo, pairsPage);
    } else {
      return undefined;
    }
  });
};

const themeVariablesByQuery = (query: string): MemoizedSelector<AppState, KeyValuePair<string, string>[]> => {
  return createSelector(themeVariablesSelector, (state) => {
    if (hasValue(state)) {
      return Object.keys(state)
        .filter((key: string) => key.includes(query))
        .map((key: string) => {
          return { key, value: state[key] };
        });
    } else {
      return undefined;
    }
  });
};
