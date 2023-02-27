import { Injectable } from '@angular/core';
import { NavigationExtras, Router } from '@angular/router';
import { RouteService } from '../services/route.service';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { combineLatest as observableCombineLatest, Observable } from 'rxjs';
import { filter, map, take } from 'rxjs/operators';
import { SortDirection, SortOptions } from '../cache/models/sort-options.model';
import { hasValue, isEmpty, isNotEmpty } from '../../shared/empty.util';
import { difference } from '../../shared/object.util';
import { FindListOptions } from '../data/find-list-options.model';
import { isNumeric } from '../../shared/numeric.util';

@Injectable({
  providedIn: 'root',
})
/**
 * Service to manage the pagination of different components
 * The pagination information will be stored in the route based on a paginationID.
 * The following params are used for the different kind of pagination information:
 *    - For the page: {paginationID}.p
 *    - For the page size: {paginationID}.rpp
 *    - For the sort direction: {paginationID}.sd
 *    - For the sort field: {paginationID}.sf
 */
export class PaginationService {

  /**
   * Sort on title ASC by default
   * @type {SortOptions}
   */
  private defaultSortOptions = new SortOptions('dc.title', SortDirection.ASC);

  private clearParams = {};

  constructor(protected routeService: RouteService,
              protected router: Router
  ) {
  }

  /**
   * Method to retrieve the current pagination settings for an ID based on the router params and default options
   * @param paginationId - The id to check the pagination for
   * @param defaultPagination - The default pagination values to be used when no route info is present
   * @returns {Observable<PaginationComponentOptions>} Retrieves the current pagination settings based on the router params
   */
  getCurrentPagination(paginationId: string, defaultPagination: PaginationComponentOptions): Observable<PaginationComponentOptions> {
    const page$ = this.routeService.getQueryParameterValue(`${paginationId}.page`);
    const size$ = this.routeService.getQueryParameterValue(`${paginationId}.rpp`);
    return observableCombineLatest([page$, size$]).pipe(
      map(([page, size]) => {
        return Object.assign(new PaginationComponentOptions(), defaultPagination, {
          currentPage: this.convertToNumeric(page, defaultPagination.currentPage),
          pageSize: this.getBestMatchPageSize(size, defaultPagination)
        });
      })
    );
  }

  /**
   * Method to retrieve the current sort options for an ID based on the router params and default options
   * @param paginationId - The id to check the sort options for
   * @param defaultSort - The default sort options to be used when no route info is present
   * @param ignoreDefault - Indicate whether the default should be ignored
   * @returns {Observable<SortOptions>} Retrieves the current sort options based on the router params
   */
  getCurrentSort(paginationId: string, defaultSort: SortOptions, ignoreDefault?: boolean): Observable<SortOptions> {
    if (!ignoreDefault && (isEmpty(defaultSort) || !hasValue(defaultSort))) {
      defaultSort = this.defaultSortOptions;
    }
    const sortDirection$ = this.routeService.getQueryParameterValue(`${paginationId}.sd`);
    const sortField$ = this.routeService.getQueryParameterValue(`${paginationId}.sf`);
    return observableCombineLatest([sortDirection$, sortField$]).pipe(map(([sortDirection, sortField]) => {
        const field = sortField || defaultSort?.field;
        const direction = SortDirection[sortDirection] || defaultSort?.direction;
        return new SortOptions(field, direction);
      })
    );
  }

  /**
   * Method to retrieve the current find list options for an ID based on the router params and default options
   * @param paginationId - The id to check the find list options for
   * @param defaultFindList - The default find list options to be used when no route info is present
   * @param ignoreDefault - Indicate whether the default should be ignored
   * @returns {Observable<FindListOptions>} Retrieves the current find list options based on the router params
   */
  getFindListOptions(paginationId: string, defaultFindList: FindListOptions, ignoreDefault?: boolean): Observable<FindListOptions> {
    const paginationComponentOptions = new PaginationComponentOptions();
    paginationComponentOptions.currentPage = defaultFindList.currentPage;
    paginationComponentOptions.pageSize = defaultFindList.elementsPerPage;
    const currentPagination$ = this.getCurrentPagination(paginationId, paginationComponentOptions);
    const currentSortOptions$ = this.getCurrentSort(paginationId, defaultFindList.sort, ignoreDefault);

    return observableCombineLatest([currentPagination$, currentSortOptions$]).pipe(
      filter(([currentPagination, currentSortOptions]) => hasValue(currentPagination) && hasValue(currentSortOptions)),
      map(([currentPagination, currentSortOptions]) => {
        return Object.assign(new FindListOptions(), defaultFindList, {
          sort: currentSortOptions,
          currentPage: currentPagination.currentPage,
          elementsPerPage: currentPagination.pageSize
        });
      }));
  }

  /**
   * Reset the current page for the provided pagination ID to 1.
   * @param paginationId - The pagination id for which to reset the page
   */
  resetPage(paginationId: string) {
    this.updateRoute(paginationId, {page: 1});
  }


  /**
   * Update the route with the provided information
   * @param paginationId - The pagination ID for which to update the route with info
   * @param params - The page related params to update in the route
   * @param extraParams - Addition params unrelated to the pagination that need to be added to the route
   * @param retainScrollPosition - Scroll to the pagination component after updating the route instead of the top of the page
   * @param navigationExtras - Extra parameters to pass on to `router.navigate`. Can be used to override values set by this service.
   */
  updateRoute(
    paginationId: string,
    params: {
      page?: number
      pageSize?: number
      sortField?: string
      sortDirection?: SortDirection
    },
    extraParams?,
    retainScrollPosition?: boolean,
    navigationExtras?: NavigationExtras,
  ) {

    this.updateRouteWithUrl(paginationId, [], params, extraParams, retainScrollPosition, navigationExtras);
  }

  /**
   * Update the route with the provided information
   * @param paginationId - The pagination ID for which to update the route with info
   * @param url - The url to navigate to
   * @param params - The page related params to update in the route
   * @param extraParams - Addition params unrelated to the pagination that need to be added to the route
   * @param retainScrollPosition - Scroll to the pagination component after updating the route instead of the top of the page
   * @param navigationExtras - Extra parameters to pass on to `router.navigate`. Can be used to override values set by this service.
   */
  updateRouteWithUrl(
    paginationId: string,
    url: string[],
    params: {
      page?: number
      pageSize?: number
      sortField?: string
      sortDirection?: SortDirection
    },
    extraParams?,
    retainScrollPosition?: boolean,
    navigationExtras?: NavigationExtras,
  ) {
    this.getCurrentRouting(paginationId).subscribe((currentFindListOptions) => {
      const currentParametersWithIdName = this.getParametersWithIdName(paginationId, currentFindListOptions);
      const parametersWithIdName = this.getParametersWithIdName(paginationId, params);
      if (isNotEmpty(difference(parametersWithIdName, currentParametersWithIdName)) || isNotEmpty(extraParams) || isNotEmpty(this.clearParams)) {
        const queryParams = Object.assign({}, this.clearParams, currentParametersWithIdName,
          parametersWithIdName, extraParams);
        if (retainScrollPosition) {
          this.router.navigate(url, {
            queryParams: queryParams,
            queryParamsHandling: 'merge',
            fragment: `p-${paginationId}`,
            ...navigationExtras,
          });
        } else {
          this.router.navigate(url, {
            queryParams: queryParams,
            queryParamsHandling: 'merge',
            ...navigationExtras,
          });
        }
        this.clearParams = {};
      }
    });
  }

  /**
   * Add the params to be cleared to the clearParams variable.
   * When the updateRoute or updateRouteWithUrl these params will be removed from the route pagination
   * @param paginationId - The ID for which to clear the params
   */
  clearPagination(paginationId: string) {
    const params = {};
    params[`${paginationId}.page`] = null;
    params[`${paginationId}.rpp`] = null;
    params[`${paginationId}.sf`] = null;
    params[`${paginationId}.sd`] = null;

    Object.assign(this.clearParams, params);
  }

  /**
   * Retrieve the page parameter for the provided id
   * @param paginationId - The ID for which to retrieve the page param
   */
  getPageParam(paginationId: string) {
    return `${paginationId}.page`;
  }

  private getCurrentRouting(paginationId: string) {
    return this.getFindListOptions(paginationId, {}, true).pipe(
      take(1),
      map((findListoptions: FindListOptions) => {
        return {
          page: findListoptions.currentPage,
          pageSize: findListoptions.elementsPerPage,
          sortField: findListoptions.sort.field,
          sortDirection: findListoptions.sort.direction,
        };
      })
    );
  }

  private getParametersWithIdName(paginationId: string, params: {
    page?: number
    pageSize?: number
    sortField?: string
    sortDirection?: SortDirection
  }) {
    const paramsWithIdName = {};
    if (hasValue(params.page)) {
      paramsWithIdName[`${paginationId}.page`] = `${params.page}`;
    }
    if (hasValue(params.pageSize)) {
      paramsWithIdName[`${paginationId}.rpp`] = `${params.pageSize}`;
    }
    if (hasValue(params.sortField)) {
      paramsWithIdName[`${paginationId}.sf`] = `${params.sortField}`;
    }
    if (hasValue(params.sortDirection)) {
      paramsWithIdName[`${paginationId}.sd`] = `${params.sortDirection}`;
    }
    return paramsWithIdName;
  }

  private convertToNumeric(param, defaultValue) {
    let result = defaultValue;
    if (isNumeric(param)) {
      result = +param;
    }
    return result;
  }


  private getBestMatchPageSize(pageSize: any, defaultPagination: PaginationComponentOptions): number {
    const numberPageSize = this.convertToNumeric(pageSize, defaultPagination.pageSize);
    const differenceList = defaultPagination.pageSizeOptions.map((pageSizeOption) => {
      return Math.abs(pageSizeOption - numberPageSize);
    });
    const minDifference = Math.min.apply(Math, differenceList);
    return defaultPagination.pageSizeOptions[differenceList.indexOf(minDifference)];
  }

}
