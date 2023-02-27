import { Injectable, OnDestroy } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { BehaviorSubject, combineLatest as observableCombineLatest, merge as observableMerge, Observable, Subscription } from 'rxjs';
import { filter, map, startWith, take } from 'rxjs/operators';
import { PaginationComponentOptions } from '../../../shared/pagination/pagination-component-options.model';
import { SearchOptions } from '../../../shared/search/models/search-options.model';
import { PaginatedSearchOptions } from '../../../shared/search/models/paginated-search-options.model';
import { SearchFilter } from '../../../shared/search/models/search-filter.model';
import { RemoteData } from '../../data/remote-data';
import { DSpaceObjectType } from '../dspace-object-type.model';
import { SortDirection, SortOptions } from '../../cache/models/sort-options.model';
import { RouteService } from '../../services/route.service';
import { getAllSucceededRemoteDataPayload, getFirstSucceededRemoteData } from '../operators';
import { hasNoValue, hasValue, isNotEmpty, isNotEmptyOperator } from '../../../shared/empty.util';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { SearchConfig, SortConfig } from './search-filters/search-config.model';
import { PaginationService } from '../../pagination/pagination.service';
import { LinkService } from '../../cache/builders/link.service';
import { HALEndpointService } from '../hal-endpoint.service';
import { RequestService } from '../../data/request.service';
import { RemoteDataBuildService } from '../../cache/builders/remote-data-build.service';
import { GetRequest } from '../../data/request.models';
import { URLCombiner } from '../../url-combiner/url-combiner';
import { GenericConstructor } from '../generic-constructor';
import { ResponseParsingService } from '../../data/parsing.service';
import { FacetConfigResponseParsingService } from '../../data/facet-config-response-parsing.service';
import { ViewMode } from '../view-mode.model';
import { SearchFilterConfig } from '../../../shared/search/models/search-filter-config.model';
import { FacetConfigResponse } from '../../../shared/search/models/facet-config-response.model';

/**
 * Service that performs all actions that have to do with the current search configuration
 */
@Injectable()
export class SearchConfigurationService implements OnDestroy {

  /**
   * Endpoint link path for retrieving search configurations
   */
  private configurationLinkPath = 'discover/search';

  /**
   * Endpoint link path for retrieving facet config incl values
   */
  private facetLinkPathPrefix = 'discover/facets/';

  /**
   * Default pagination id
   */
  public paginationID = 'spc';

  /**
   * Emits the current search options
   */
  public searchOptions: BehaviorSubject<SearchOptions>;

  /**
   * Emits the current search options including pagination and sort
   */
  public paginatedSearchOptions: BehaviorSubject<PaginatedSearchOptions>;

  /**
   * Default pagination settings
   */
  protected defaultPagination = Object.assign(new PaginationComponentOptions(), {
    id: this.paginationID,
    pageSize: 10,
    currentPage: 1
  });

  /**
   * Default scope setting
   */
  protected defaultScope = '';

  /**
   * Default query setting
   */
  protected defaultQuery = '';

  /**
   * Emits the current default values
   */
  protected _defaults: Observable<RemoteData<PaginatedSearchOptions>>;

  /**
   * A map of subscriptions to unsubscribe from on destroy
   */
  protected subs: Map<string, Subscription[]> = new Map<string, Subscription[]>(null);

  /**
   * Initialize the search options
   * @param {RouteService} routeService
   * @param {PaginationService} paginationService
   * @param {ActivatedRoute} route
   * @param linkService
   * @param halService
   * @param requestService
   * @param rdb
   */
  constructor(protected routeService: RouteService,
              protected paginationService: PaginationService,
              protected route: ActivatedRoute,
              protected linkService: LinkService,
              protected halService: HALEndpointService,
              protected requestService: RequestService,
              protected rdb: RemoteDataBuildService,) {

    this.initDefaults();
  }

  /**
   * Default values for the Search Options
   */
  get defaults(): Observable<RemoteData<PaginatedSearchOptions>> {
    if (hasNoValue(this._defaults)) {
      const options = new PaginatedSearchOptions({
        pagination: this.defaultPagination,
        scope: this.defaultScope,
        query: this.defaultQuery
      });
      this._defaults = createSuccessfulRemoteDataObject$(options, new Date().getTime());
    }
    return this._defaults;
  }

  /**
   * @returns {Observable<string>} Emits the current configuration string
   */
  getCurrentConfiguration(defaultConfiguration: string) {
    return observableCombineLatest([
      this.routeService.getQueryParameterValue('configuration').pipe(startWith(undefined)),
      this.routeService.getRouteParameterValue('configuration').pipe(startWith(undefined))
    ]).pipe(
      map(([queryConfig, routeConfig]) => {
        return queryConfig || routeConfig || defaultConfiguration;
      })
    );
  }

  /**
   * @returns {Observable<string>} Emits the current scope's identifier
   */
  getCurrentScope(defaultScope: string) {
    return this.routeService.getQueryParameterValue('scope').pipe(map((scope) => {
      return scope || defaultScope;
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current query string
   */
  getCurrentQuery(defaultQuery: string) {
    return this.routeService.getQueryParameterValue('query').pipe(map((query) => {
      return query || defaultQuery;
    }));
  }

  /**
   * @returns {Observable<number>} Emits the current DSpaceObject type as a number
   */
  getCurrentDSOType(): Observable<DSpaceObjectType> {
    return this.routeService.getQueryParameterValue('dsoType').pipe(
      filter((type) => isNotEmpty(type) && hasValue(DSpaceObjectType[type.toUpperCase()])),
      map((type) => DSpaceObjectType[type.toUpperCase()]),);
  }

  /**
   * @returns {Observable<string>} Emits the current pagination settings
   */
  getCurrentPagination(paginationId: string, defaultPagination: PaginationComponentOptions): Observable<PaginationComponentOptions> {
    return this.paginationService.getCurrentPagination(paginationId, defaultPagination);
  }

  /**
   * @returns {Observable<string>} Emits the current sorting settings
   */
  getCurrentSort(paginationId: string, defaultSort: SortOptions): Observable<SortOptions> {
    return this.paginationService.getCurrentSort(paginationId, defaultSort);
  }

  /**
   * @returns {Observable<Params>} Emits the current active filters with their values as they are sent to the backend
   */
  getCurrentFilters(): Observable<SearchFilter[]> {
    return this.routeService.getQueryParamsWithPrefix('f.').pipe(map((filterParams) => {
      if (isNotEmpty(filterParams)) {
        const filters = [];
        Object.keys(filterParams).forEach((key) => {
          if (key.endsWith('.min') || key.endsWith('.max')) {
            const realKey = key.slice(0, -4);
            if (hasNoValue(filters.find((f) => f.key === realKey))) {
              const min = filterParams[realKey + '.min'] ? filterParams[realKey + '.min'][0] : '*';
              const max = filterParams[realKey + '.max'] ? filterParams[realKey + '.max'][0] : '*';
              filters.push(new SearchFilter(realKey, ['[' + min + ' TO ' + max + ']'], 'equals'));
            }
          } else {
            filters.push(new SearchFilter(key, filterParams[key]));
          }
        });
        return filters;
      }
      return [];
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current fixed filter as a string
   */
  getCurrentFixedFilter(): Observable<string> {
    return this.routeService.getRouteParameterValue('fixedFilterQuery');
  }

  /**
   * @returns {Observable<Params>} Emits the current active filters with their values as they are displayed in the frontend URL
   */
  getCurrentFrontendFilters(): Observable<Params> {
    return this.routeService.getQueryParamsWithPrefix('f.');
  }

  /**
   * @returns {Observable<string>} Emits the current view mode
   */
  getCurrentViewMode(defaultViewMode: ViewMode) {
    return this.routeService.getQueryParameterValue('view').pipe(map((viewMode) => {
      return viewMode || defaultViewMode;
    }));
  }

  /**
   * Creates an observable of SearchConfig every time the configuration stream emits.
   * @param configuration The search configuration
   * @param scope The search scope if exists
   */
  getConfigurationSearchConfig(configuration: string, scope?: string): Observable<SearchConfig> {
    return this.getSearchConfigurationFor(scope, configuration).pipe(
      getAllSucceededRemoteDataPayload()
    );
  }

  /**
   * Return the SortOptions list available for the given SearchConfig
   * @param searchConfig The SearchConfig object
   */
  getConfigurationSortOptions(searchConfig: SearchConfig): SortOptions[] {
    return searchConfig.sortOptions.map((entry: SortConfig) => ({
      field: entry.name,
      direction: entry.sortOrder.toLowerCase() === SortDirection.ASC.toLowerCase() ? SortDirection.ASC : SortDirection.DESC
    }));
  }

  setPaginationId(paginationId): void {
    if (isNotEmpty(paginationId)) {
      const currentValue: PaginatedSearchOptions = this.paginatedSearchOptions.getValue();
      const updatedValue: PaginatedSearchOptions = Object.assign(new PaginatedSearchOptions({}), currentValue, {
        pagination: Object.assign({}, currentValue.pagination, {
          id: paginationId
        })
      });
      // unsubscribe from subscription related to old pagination id
      this.unsubscribeFromSearchOptions(this.paginationID);

      // change to the new pagination id
      this.paginationID = paginationId;
      this.paginatedSearchOptions.next(updatedValue);
      this.setSearchSubscription(this.paginationID, this.paginatedSearchOptions.value);
    }
  }

  /**
   * Make sure to unsubscribe from all existing subscription to prevent memory leaks
   */
  ngOnDestroy(): void {
    this.subs
      .forEach((subs: Subscription[]) => subs
        .filter((sub) => hasValue(sub))
        .forEach((sub) => sub.unsubscribe())
      );

    this.subs = new Map<string, Subscription[]>(null);
  }

  /**
   * Initialize the search options
   */
  protected initDefaults() {
    this.defaults
      .pipe(getFirstSucceededRemoteData())
      .subscribe((defRD: RemoteData<PaginatedSearchOptions>) => {
        const defs = defRD.payload;
        this.paginatedSearchOptions = new BehaviorSubject<PaginatedSearchOptions>(defs);
        this.searchOptions = new BehaviorSubject<SearchOptions>(defs);
        this.setSearchSubscription(this.paginationID, defs);
      });
  }

  private setSearchSubscription(paginationID: string, defaults: PaginatedSearchOptions) {
    this.unsubscribeFromSearchOptions(paginationID);
    const subs = [
      this.subscribeToSearchOptions(defaults),
      this.subscribeToPaginatedSearchOptions(paginationID || defaults.pagination.id, defaults)
    ];
    this.subs.set(this.paginationID, subs);
  }

  /**
   * Sets up a subscription to all necessary parameters to make sure the searchOptions emits a new value every time they update
   * @param {SearchOptions} defaults Default values for when no parameters are available
   * @returns {Subscription} The subscription to unsubscribe from
   */
  private subscribeToSearchOptions(defaults: SearchOptions): Subscription {
    return observableMerge(
      this.getConfigurationPart(defaults.configuration),
      this.getScopePart(defaults.scope),
      this.getQueryPart(defaults.query),
      this.getDSOTypePart(),
      this.getFiltersPart(),
      this.getFixedFilterPart(),
      this.getViewModePart(defaults.view)
    ).subscribe((update) => {
      const currentValue: SearchOptions = this.searchOptions.getValue();
      const updatedValue: SearchOptions = Object.assign(new PaginatedSearchOptions({}), currentValue, update);
      this.searchOptions.next(updatedValue);
    });
  }

  /**
   * Sets up a subscription to all necessary parameters to make sure the paginatedSearchOptions emits a new value every time they update
   * @param {string} paginationId The pagination ID
   * @param {PaginatedSearchOptions} defaults Default values for when no parameters are available
   * @returns {Subscription} The subscription to unsubscribe from
   */
  private subscribeToPaginatedSearchOptions(paginationId: string, defaults: PaginatedSearchOptions): Subscription {
    return observableMerge(
      this.getConfigurationPart(defaults.configuration),
      this.getPaginationPart(paginationId, defaults.pagination),
      this.getSortPart(paginationId, defaults.sort),
      this.getScopePart(defaults.scope),
      this.getQueryPart(defaults.query),
      this.getDSOTypePart(),
      this.getFiltersPart(),
      this.getFixedFilterPart(),
      this.getViewModePart(defaults.view)
    ).subscribe((update) => {
      const currentValue: PaginatedSearchOptions = this.paginatedSearchOptions.getValue();
      const updatedValue: PaginatedSearchOptions = Object.assign(new PaginatedSearchOptions({}), currentValue, update);
      this.paginatedSearchOptions.next(updatedValue);
    });
  }

  /**
   * Unsubscribe from all subscriptions related to the given paginationID
   * @param paginationId The pagination id
   */
  private unsubscribeFromSearchOptions(paginationId: string): void {
    if (this.subs.has(this.paginationID)) {
      this.subs.get(this.paginationID)
        .filter((sub) => hasValue(sub))
        .forEach((sub) => sub.unsubscribe());
      this.subs.delete(paginationId);
    }
  }

  /**
   * @returns {Observable<string>} Emits the current configuration settings as a partial SearchOptions object
   */
  private getConfigurationPart(defaultConfiguration: string): Observable<any> {
    return this.getCurrentConfiguration(defaultConfiguration).pipe(map((configuration) => {
      return { configuration };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current scope's identifier
   */
  private getScopePart(defaultScope: string): Observable<any> {
    return this.getCurrentScope(defaultScope).pipe(map((scope) => {
      return { scope };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current query string as a partial SearchOptions object
   */
  private getQueryPart(defaultQuery: string): Observable<any> {
    return this.getCurrentQuery(defaultQuery).pipe(map((query) => {
      return { query };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current query string as a partial SearchOptions object
   */
  private getDSOTypePart(): Observable<any> {
    return this.getCurrentDSOType().pipe(map((dsoType) => {
      return { dsoType };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current pagination settings as a partial SearchOptions object
   */
  private getPaginationPart(paginationId: string, defaultPagination: PaginationComponentOptions): Observable<any> {
    return this.getCurrentPagination(paginationId, defaultPagination).pipe(map((pagination) => {
      return { pagination };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current sorting settings as a partial SearchOptions object
   */
  private getSortPart(paginationId: string, defaultSort: SortOptions): Observable<any> {
    return this.getCurrentSort(paginationId, defaultSort).pipe(map((sort) => {
      return { sort };
    }));
  }

  /**
   * @returns {Observable<Params>} Emits the current active filters as a partial SearchOptions object
   */
  private getFiltersPart(): Observable<any> {
    return this.getCurrentFilters().pipe(map((filters) => {
      return { filters };
    }));
  }

  /**
   * @returns {Observable<string>} Emits the current fixed filter as a partial SearchOptions object
   */
  private getFixedFilterPart(): Observable<any> {
    return this.getCurrentFixedFilter().pipe(
      isNotEmptyOperator(),
      map((fixedFilter) => {
        return { fixedFilter };
      }),
    );
  }


  /**
   * Request the search configuration for a given scope or the whole repository
   * @param {string} scope UUID of the object for which config the filter config is requested, when no scope is provided the configuration for the whole repository is loaded
   * @param {string} configurationName the name of the configuration
   * @returns {Observable<RemoteData<SearchConfig[]>>} The found configuration
   */
  getSearchConfigurationFor(scope?: string, configurationName?: string): Observable<RemoteData<SearchConfig>> {
    const href$ = this.halService.getEndpoint(this.configurationLinkPath).pipe(
      map((url: string) => this.getConfigUrl(url, scope, configurationName)),
    );

    href$.pipe(take(1)).subscribe((url: string) => {
      const request = new GetRequest(this.requestService.generateRequestId(), url);
      this.requestService.send(request, true);
    });

    return this.rdb.buildFromHref(href$);
  }

  private getConfigUrl(url: string, scope?: string, configurationName?: string) {
    const args: string[] = [];

    if (isNotEmpty(scope)) {
      args.push(`scope=${scope}`);
    }

    if (isNotEmpty(configurationName)) {
      args.push(`configuration=${configurationName}`);
    }

    if (isNotEmpty(args)) {
      url = new URLCombiner(url, `?${args.join('&')}`).toString();
    }

    return url;
  }



  /**
   * Request the filter configuration for a given scope or the whole repository
   * @param {string} scope UUID of the object for which config the filter config is requested, when no scope is provided the configuration for the whole repository is loaded
   * @param {string} configurationName the name of the configuration
   * @returns {Observable<RemoteData<SearchFilterConfig[]>>} The found filter configuration
   */
  getConfig(scope?: string, configurationName?: string): Observable<RemoteData<SearchFilterConfig[]>> {
    const href$ = this.halService.getEndpoint(this.facetLinkPathPrefix).pipe(
      map((url: string) => this.getConfigUrl(url, scope, configurationName)),
    );

    href$.pipe(take(1)).subscribe((url: string) => {
      let request = new GetRequest(this.requestService.generateRequestId(), url);
      request = Object.assign(request, {
        getResponseParser(): GenericConstructor<ResponseParsingService> {
          return FacetConfigResponseParsingService;
        }
      });
      this.requestService.send(request, true);
    });

    return this.rdb.buildFromHref(href$).pipe(
      map((rd: RemoteData<FacetConfigResponse>) => {
        if (rd.hasSucceeded) {
          let filters: SearchFilterConfig[];
          if (isNotEmpty(rd.payload.filters)) {
            filters = rd.payload.filters
              .map((f: any) => Object.assign(new SearchFilterConfig(), f));
          } else {
            filters = [];
          }

          return new RemoteData(
            rd.timeCompleted,
            rd.msToLive,
            rd.lastUpdated,
            rd.state,
            rd.errorMessage,
            filters,
            rd.statusCode,
          );
        } else {
          return rd as any as RemoteData<SearchFilterConfig[]>;
        }
      })
    );
  }


  /**
   * @returns {Observable<Params>} Emits the current view mode as a partial SearchOptions object
   */
  private getViewModePart(defaultViewMode: ViewMode): Observable<any> {
    return this.getCurrentViewMode(defaultViewMode).pipe(map((view) => {
      return { view };
    }));
  }
}
