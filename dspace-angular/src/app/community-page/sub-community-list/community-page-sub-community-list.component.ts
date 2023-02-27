import { Component, Input, OnDestroy, OnInit } from '@angular/core';

import { BehaviorSubject, combineLatest as observableCombineLatest } from 'rxjs';

import { RemoteData } from '../../core/data/remote-data';
import { Community } from '../../core/shared/community.model';
import { fadeIn } from '../../shared/animations/fade';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { SortDirection, SortOptions } from '../../core/cache/models/sort-options.model';
import { CommunityDataService } from '../../core/data/community-data.service';
import { takeUntilCompletedRemoteData } from '../../core/shared/operators';
import { switchMap } from 'rxjs/operators';
import { PaginationService } from '../../core/pagination/pagination.service';
import { hasValue } from '../../shared/empty.util';

@Component({
  selector: 'ds-community-page-sub-community-list',
  styleUrls: ['./community-page-sub-community-list.component.scss'],
  templateUrl: './community-page-sub-community-list.component.html',
  animations: [fadeIn]
})
/**
 * Component to render the sub-communities of a Community
 */
export class CommunityPageSubCommunityListComponent implements OnInit, OnDestroy {
  @Input() community: Community;

  /**
   * Optional page size. Overrides communityList.pageSize configuration for this component.
   * Value can be added in the themed version of the parent component.
   */
  @Input() pageSize: number;

  /**
   * The pagination configuration
   */
  config: PaginationComponentOptions;

  /**
   * The pagination id
   */
  pageId = 'cmscm';

  /**
   * The sorting configuration
   */
  sortConfig: SortOptions;

  /**
   * A list of remote data objects of communities' collections
   */
  subCommunitiesRDObs: BehaviorSubject<RemoteData<PaginatedList<Community>>> = new BehaviorSubject<RemoteData<PaginatedList<Community>>>({} as any);

  constructor(private cds: CommunityDataService,
              private paginationService: PaginationService
  ) {
  }

  ngOnInit(): void {
    this.config = new PaginationComponentOptions();
    this.config.id = this.pageId;
    if (hasValue(this.pageSize)) {
      this.config.pageSize = this.pageSize;
    }
    this.config.currentPage = 1;
    this.sortConfig = new SortOptions('dc.title', SortDirection.ASC);
    this.initPage();
  }

  /**
   * Update the list of sub-communities
   */
  initPage() {
    const pagination$ = this.paginationService.getCurrentPagination(this.config.id, this.config);
    const sort$ = this.paginationService.getCurrentSort(this.config.id, this.sortConfig);

    observableCombineLatest([pagination$, sort$]).pipe(
      switchMap(([currentPagination, currentSort]) => {
        return     this.cds.findByParent(this.community.id, {
          currentPage: currentPagination.currentPage,
          elementsPerPage: currentPagination.pageSize,
          sort: { field: currentSort.field, direction: currentSort.direction }
        });
      })
    ).subscribe((results) => {
      this.subCommunitiesRDObs.next(results);
    });


    this.cds.findByParent(this.community.id, {
      currentPage: this.config.currentPage,
      elementsPerPage: this.config.pageSize,
      sort: { field: this.sortConfig.field, direction: this.sortConfig.direction }
    }).pipe(takeUntilCompletedRemoteData()).subscribe((results) => {
      this.subCommunitiesRDObs.next(results);
    });
  }

  ngOnDestroy(): void {
    this.paginationService.clearPagination(this.config.id);
  }

}
