import { RequestService } from '../core/data/request.service';
import { Injectable } from '@angular/core';
import { DSpaceObject } from '../core/shared/dspace-object.model';
import { map, take } from 'rxjs/operators';
import { TrackRequest } from './track-request.model';
import { hasValue, isNotEmpty } from '../shared/empty.util';
import { HALEndpointService } from '../core/shared/hal-endpoint.service';
import { SearchOptions } from '../shared/search/models/search-options.model';
import { RestRequest } from '../core/data/rest-request.model';

/**
 * The statistics service
 */
@Injectable({providedIn: 'root'})
export class StatisticsService {

  constructor(
    protected requestService: RequestService,
    protected halService: HALEndpointService,
  ) {
  }

  private sendEvent(linkPath: string, body: any) {
    const requestId = this.requestService.generateRequestId();
    this.halService.getEndpoint(linkPath).pipe(
      map((endpoint: string) => new TrackRequest(requestId, endpoint, JSON.stringify(body))),
      take(1) // otherwise the previous events will fire again
    ).subscribe((request: RestRequest) => this.requestService.send(request));
  }

  /**
   * To track a page view
   * @param dso: The dso which was viewed
   */
  trackViewEvent(dso: DSpaceObject) {
    this.sendEvent('/statistics/viewevents', {
      targetId: dso.uuid,
      targetType: (dso as any).type
    });
  }

  /**
   * To track a search
   * @param searchOptions: The query, scope, dsoType and configuration of the search. Filters from this object are ignored in favor of the filters parameter of this method.
   * @param page: An object that describes the pagination status
   * @param sort: An object that describes the sort status
   * @param filters: An array of search filters used to filter the result set
   */
  trackSearchEvent(
    searchOptions: SearchOptions,
    page: { size: number, totalElements: number, totalPages: number, number: number },
    sort: { by: string, order: string },
    filters?: { filter: string, operator: string, value: string, label: string }[]
  ) {
    const body = {
      query: searchOptions.query,
      page: {
        size: page.size,
        totalElements: page.totalElements,
        totalPages: page.totalPages,
        number: page.number
      },
      sort: {
        by: sort.by,
        order: sort.order.toLowerCase()
      },
    };
    if (hasValue(searchOptions.configuration)) {
      Object.assign(body, { configuration: searchOptions.configuration });
    }
    if (isNotEmpty(searchOptions.dsoTypes)) {
      Object.assign(body, { dsoType: searchOptions.dsoTypes[0].toLowerCase()  });
    }
    if (hasValue(searchOptions.scope)) {
      Object.assign(body, { scope: searchOptions.scope });
    }
    if (isNotEmpty(filters)) {
      const bodyFilters = [];
      for (let i = 0, arrayLength = filters.length; i < arrayLength; i++) {
        const filter = filters[i];
        bodyFilters.push({
          filter: filter.filter,
          operator: filter.operator,
          value: filter.value,
          label: filter.label
        });
      }
      Object.assign(body, { appliedFilters: bodyFilters });
    }
    this.sendEvent('/statistics/searchevents', body);
  }

}
