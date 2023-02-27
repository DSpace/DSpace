import { Component, Inject, OnInit } from '@angular/core';
import { Angulartics2 } from 'angulartics2';
import { map, switchMap } from 'rxjs/operators';
import { SearchComponent } from '../shared/search/search.component';
import { SidebarService } from '../shared/sidebar/sidebar.service';
import { HostWindowService } from '../shared/host-window.service';
import { SEARCH_CONFIG_SERVICE } from '../my-dspace-page/my-dspace-page.component';
import { RouteService } from '../core/services/route.service';
import { SearchConfigurationService } from '../core/shared/search/search-configuration.service';
import { SearchService } from '../core/shared/search/search.service';
import { PaginatedSearchOptions } from '../shared/search/models/paginated-search-options.model';
import { SearchObjects } from '../shared/search/models/search-objects.model';
import { Router } from '@angular/router';
import { RemoteData } from '../core/data/remote-data';
import { DSpaceObject } from '../core/shared/dspace-object.model';
import { getFirstSucceededRemoteData } from '../core/shared/operators';

/**
 * This component triggers a page view statistic
 */
@Component({
  selector: 'ds-search-tracker',
  styleUrls: ['./search-tracker.component.scss'],
  templateUrl: './search-tracker.component.html',
  providers: [
    {
      provide: SEARCH_CONFIG_SERVICE,
      useClass: SearchConfigurationService
    }
  ]
})
export class SearchTrackerComponent extends SearchComponent implements OnInit {

  constructor(
    protected service: SearchService,
    protected sidebarService: SidebarService,
    protected windowService: HostWindowService,
    @Inject(SEARCH_CONFIG_SERVICE) public searchConfigService: SearchConfigurationService,
    protected routeService: RouteService,
    public angulartics2: Angulartics2,
    protected router: Router
  ) {
    super(service, sidebarService, windowService, searchConfigService, routeService, router);
  }

  ngOnInit(): void {
    // super.ngOnInit();
    this.getSearchOptions().pipe(
      switchMap((options: PaginatedSearchOptions) =>
        this.service.searchEntries(options).pipe(
          getFirstSucceededRemoteData(),
          map((rd: RemoteData<SearchObjects<DSpaceObject>>) => ({
            config: options,
            searchQueryResponse: rd.payload
          }))
        )),
    ).subscribe(({ config, searchQueryResponse }) => {
        const filters: { filter: string, operator: string, value: string, label: string; }[] = [];
        const appliedFilters = searchQueryResponse.appliedFilters || [];
        for (let i = 0, filtersLength = appliedFilters.length; i < filtersLength; i++) {
          const appliedFilter = appliedFilters[i];
          filters.push(appliedFilter);
        }
        this.angulartics2.eventTrack.next({
          action: 'search',
          properties: {
            searchOptions: config,
            page: {
              size: config.pagination.size, // same as searchQueryResponse.page.elementsPerPage
              totalElements: searchQueryResponse.pageInfo.totalElements,
              totalPages: searchQueryResponse.pageInfo.totalPages,
              number: config.pagination.currentPage, // same as searchQueryResponse.page.currentPage
            },
            sort: {
              by: config.sort.field,
              order: config.sort.direction
            },
            filters: filters,
          },
        });
      });
  }
}
