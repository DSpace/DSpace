import { distinctUntilChanged, filter, map, take } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Params, Router, RouterStateSnapshot, } from '@angular/router';

import { combineLatest, Observable } from 'rxjs';
import { createSelector, MemoizedSelector, select, Store } from '@ngrx/store';
import isEqual from 'lodash/isEqual';

import { AddParameterAction, SetParameterAction, SetParametersAction, SetQueryParameterAction, SetQueryParametersAction } from './route.actions';
import { coreSelector } from '../core.selectors';
import { hasValue } from '../../shared/empty.util';
import { historySelector } from '../history/selectors';
import { AddUrlToHistoryAction } from '../history/history.actions';
import { CoreState } from '../core-state.model';

/**
 * Selector to select all route parameters from the store
 */
export const routeParametersSelector = createSelector(
  coreSelector,
  (state: CoreState) => hasValue(state) && hasValue(state.route) ? state.route.params : undefined
);

/**
 * Selector to select all query parameters from the store
 */
export const queryParametersSelector = createSelector(
  coreSelector,
  (state: CoreState) => hasValue(state) && hasValue(state.route) ? state.route.queryParams : undefined
);

/**
 * Selector to select a specific route parameter from the store
 * @param key The key of the parameter
 */
export const routeParameterSelector = (key: string) => parameterSelector(key, routeParametersSelector);

/**
 * Selector to select a specific query parameter from the store
 * @param key The key of the parameter
 */
export const queryParameterSelector = (key: string) => parameterSelector(key, queryParametersSelector);

/**
 * Function to select a specific parameter from the store
 * @param key The key to look for
 * @param paramsSelector The selector that selects the parameters to search in
 */
export function parameterSelector(key: string, paramsSelector: (state: CoreState) => Params): MemoizedSelector<CoreState, string> {
  return createSelector(paramsSelector, (state: Params) => {
    if (hasValue(state)) {
      return state[key];
    } else {
      return undefined;
    }
  });
}

/**
 * Service to keep track of the current query parameters
 */
@Injectable({
  providedIn: 'root'
})
export class RouteService {
  constructor(private route: ActivatedRoute, private router: Router, private store: Store<CoreState>) {
    this.saveRouting();
  }

  /**
   * Retrieves all query parameter values based on a parameter name
   * @param paramName The name of the parameter to look for
   */
  getQueryParameterValues(paramName: string): Observable<string[]> {
    return this.getQueryParamMap().pipe(
      map((params) => [...params.getAll(paramName)]),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b))
    );
  }

  /**
   * Retrieves a single query parameter values based on a parameter name
   * @param paramName The name of the parameter to look for
   */
  getQueryParameterValue(paramName: string): Observable<string> {
    return this.getQueryParamMap().pipe(
      map((params) => params.get(paramName)),
      distinctUntilChanged()
    );
  }

  /**
   * Checks if the query parameter currently exists in the route
   * @param paramName The name of the parameter to look for
   */
  hasQueryParam(paramName: string): Observable<boolean> {
    return this.getQueryParamMap().pipe(
      map((params) => params.has(paramName)),
      distinctUntilChanged()
    );
  }

  /**
   * Checks if the query parameter with a specific value currently exists in the route
   * @param paramName The name of the parameter to look for
   * @param paramValue The value of the parameter to look for
   */
  hasQueryParamWithValue(paramName: string, paramValue: string): Observable<boolean> {
    return this.getQueryParamMap().pipe(
      map((params) => params.getAll(paramName).indexOf(paramValue) > -1),
      distinctUntilChanged()
    );
  }

  getRouteParameterValue(paramName: string): Observable<string> {
    return this.store.pipe(select(routeParameterSelector(paramName)));
  }

  getRouteDataValue(datafield: string): Observable<any> {
    return this.route.data.pipe(map((data) => data[datafield]), distinctUntilChanged());
  }

  /**
   * Retrieves all query parameters of which the parameter name starts with the given prefix
   * @param prefix The prefix of the parameter name to look for
   */
  getQueryParamsWithPrefix(prefix: string): Observable<Params> {
    return this.getQueryParamMap().pipe(
      map((qparams) => {
        const params = {};
        qparams.keys
          .filter((key) => key.startsWith(prefix))
          .forEach((key) => {
            params[key] = [...qparams.getAll(key)];
          });
        return params;
      }),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
    );
  }

  public getQueryParamMap(): Observable<any> {
    return this.route.queryParamMap.pipe(
      map((paramMap) => {
        const snapshot: RouterStateSnapshot = this.router.routerState.snapshot;
        // Due to an Angular bug, sometimes change of QueryParam is not detected so double checks with route snapshot
        if (!isEqual(paramMap, snapshot.root.queryParamMap)) {
          return snapshot.root.queryParamMap;
        } else {
          return paramMap;
        }
      }));
  }

  public saveRouting(): void {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.store.dispatch(new AddUrlToHistoryAction(event.urlAfterRedirects));
      });
  }

  private getRouteParams(): Observable<Params> {
    let active = this.route;
    while (active.firstChild) {
      active = active.firstChild;
    }
    return active.params;
  }

  public getHistory(): Observable<string[]> {
    return this.store.pipe(select(historySelector));
  }

  /**
   * Return the current url retrieved from history
   */
  public getCurrentUrl(): Observable<string> {
    return this.getHistory().pipe(
      map((history: string[]) => history[history.length - 1] || '')
    );
  }

  /**
   * Return the current url retrieved from history
   */
  public getPreviousUrl(): Observable<string> {
    return this.getHistory().pipe(
      map((history: string[]) => history[history.length - 2] || '')
    );
  }

  /**
   * Add a parameter to the current route
   * @param key   The parameter name
   * @param value The parameter value
   */
  public addParameter(key, value) {
    this.store.dispatch(new AddParameterAction(key, value));
  }

  /**
   * Set a parameter in the current route (overriding the previous value)
   * @param key   The parameter name
   * @param value The parameter value
   */
  public setParameter(key, value) {
    this.store.dispatch(new SetParameterAction(key, value));
  }

  public setQueryParameter(key, value) {
    this.store.dispatch(new SetQueryParameterAction(key, value));
  }

  /**
   * Sets the current route parameters and query parameters in the store
   */
  public setCurrentRouteInfo() {
    combineLatest(this.getRouteParams(), this.route.queryParams)
      .pipe(take(1))
      .subscribe(
        ([params, queryParams]: [Params, Params]) => {
          this.store.dispatch(new SetParametersAction(params));
          this.store.dispatch(new SetQueryParametersAction(queryParams));
        }
      );
  }
}
