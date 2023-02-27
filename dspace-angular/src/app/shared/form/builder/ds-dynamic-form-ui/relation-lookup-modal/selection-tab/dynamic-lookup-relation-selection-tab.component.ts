import { Component, EventEmitter, Input, Output } from '@angular/core';
import { SEARCH_CONFIG_SERVICE } from '../../../../../../my-dspace-page/my-dspace-page.component';
import { SearchConfigurationService } from '../../../../../../core/shared/search/search-configuration.service';
import { Observable } from 'rxjs';
import { ListableObject } from '../../../../../object-collection/shared/listable-object.model';
import { RemoteData } from '../../../../../../core/data/remote-data';
import { map, switchMap, take } from 'rxjs/operators';
import { PaginationComponentOptions } from '../../../../../pagination/pagination-component-options.model';
import { buildPaginatedList, PaginatedList } from '../../../../../../core/data/paginated-list.model';
import { Router } from '@angular/router';
import { PaginatedSearchOptions } from '../../../../../search/models/paginated-search-options.model';
import { PageInfo } from '../../../../../../core/shared/page-info.model';
import { Context } from '../../../../../../core/shared/context.model';
import { createSuccessfulRemoteDataObject } from '../../../../../remote-data.utils';
import { PaginationService } from '../../../../../../core/pagination/pagination.service';

@Component({
  selector: 'ds-dynamic-lookup-relation-selection-tab',
  styleUrls: ['./dynamic-lookup-relation-selection-tab.component.scss'],
  templateUrl: './dynamic-lookup-relation-selection-tab.component.html',
  providers: [
    {
      provide: SEARCH_CONFIG_SERVICE,
      useClass: SearchConfigurationService
    }
  ]
})

/**
 * Tab for inside the lookup model that represents the currently selected relationships
 */
export class DsDynamicLookupRelationSelectionTabComponent {
  /**
   * A string that describes the type of relationship
   */
  @Input() relationshipType: string;

  /**
   * The ID of the list to add/remove selected items to/from
   */
  @Input() listId: string;

  /**
   * Is the selection repeatable?
   */
  @Input() repeatable: boolean;

  /**
   * The list of selected items
   */
  @Input() selection$: Observable<ListableObject[]>;

  /**
   * The paginated list of selected items
   */
  @Input() selectionRD$: Observable<RemoteData<PaginatedList<ListableObject>>>;

  /**
   * The context to display lists
   */
  @Input() context: Context;

  /**
   * Send an event to deselect an object from the list
   */
  @Output() deselectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  /**
   * Send an event to select an object from the list
   */
  @Output() selectObject: EventEmitter<ListableObject> = new EventEmitter<ListableObject>();

  /**
   * The initial pagination to use
   */
  initialPagination = Object.assign(new PaginationComponentOptions(), {
    id: 'spc',
    pageSize: 5
  });

  /**
   * The current pagination options
   */
  currentPagination$: Observable<PaginationComponentOptions>;

  constructor(private router: Router,
              private searchConfigService: SearchConfigurationService,
              private paginationService: PaginationService
  ) {
  }

  /**
   * Set up the selection and pagination on load
   */
  ngOnInit() {
    this.resetRoute();
    this.selectionRD$ = this.searchConfigService.paginatedSearchOptions
      .pipe(
        map((options: PaginatedSearchOptions) => options.pagination),
        switchMap((pagination: PaginationComponentOptions) => {
          return this.selection$.pipe(
            take(1),
            map((selected) => {
              const offset = (pagination.currentPage - 1) * pagination.pageSize;
              const end = (offset + pagination.pageSize) > selected.length ? selected.length : offset + pagination.pageSize;
              const selection = selected.slice(offset, end);
              const pageInfo = new PageInfo(
                {
                  elementsPerPage: pagination.pageSize,
                  totalElements: selected.length,
                  currentPage: pagination.currentPage,
                  totalPages: Math.ceil(selected.length / pagination.pageSize)
                });
              return createSuccessfulRemoteDataObject(buildPaginatedList(pageInfo, selection));
            })
          );
        })
      );
    this.currentPagination$ = this.paginationService.getCurrentPagination(this.searchConfigService.paginationID, this.initialPagination);
  }

  /**
   * Method to reset the route when the tab is opened to make sure no strange pagination issues appears
   */
  resetRoute() {
    this.paginationService.updateRoute(this.searchConfigService.paginationID, {
      page: 1,
      pageSize: 5
    });
  }
}
