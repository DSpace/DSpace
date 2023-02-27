import { ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';

import { Observable, of as observableOf, Subscription } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

import { RequestParam } from '../../../core/cache/models/request-param.model';
import { ExternalSourceDataService } from '../../../core/data/external-source-data.service';
import { ExternalSource } from '../../../core/shared/external-source.model';
import { buildPaginatedList, PaginatedList } from '../../../core/data/paginated-list.model';
import { RemoteData } from '../../../core/data/remote-data';
import { PageInfo } from '../../../core/shared/page-info.model';
import { createSuccessfulRemoteDataObject } from '../../../shared/remote-data.utils';
import { getFirstSucceededRemoteData, getFirstSucceededRemoteDataPayload } from '../../../core/shared/operators';
import { HostWindowService } from '../../../shared/host-window.service';
import { hasValue } from '../../../shared/empty.util';
import { FindListOptions } from '../../../core/data/find-list-options.model';

/**
 * Interface for the selected external source element.
 */
export interface SourceElement {
  id: string;
  name: string;
}

/**
 * Interface for the external source data to export.
 */
export interface ExternalSourceData {
  entity: string;
  query: string;
  sourceId: string;
}

/**
 * This component builds the searchbar for the submission external import.
 */
@Component({
  selector: 'ds-submission-import-external-searchbar',
  styleUrls: ['./submission-import-external-searchbar.component.scss'],
  templateUrl: './submission-import-external-searchbar.component.html'
})
export class SubmissionImportExternalSearchbarComponent implements OnInit, OnDestroy {
  /**
   * The init external source value.
   */
  @Input() public initExternalSourceData: ExternalSourceData;
  /**
   * The selected external sources.
   */
  public selectedElement: SourceElement;
  /**
   * The list of external sources.
   */
  public sourceList: SourceElement[];
  /**
   * The string used to search items in the external sources.
   */
  public searchString: string;
  /**
   * The external sources loading status.
   */
  public sourceListLoading = false;
  /**
   * Emits true if were on a small screen
   */
  public isXsOrSm$: Observable<boolean>;
  /**
   * The external source data to use to perform the search.
   */
  @Output() public externalSourceData: EventEmitter<ExternalSourceData> = new EventEmitter<ExternalSourceData>();

  /**
   * The external sources pagination data.
   */
  protected pageInfo: PageInfo;
  /**
   * The options for REST data retireval.
   */
  protected findListOptions: FindListOptions;

  /**
   * The subscription to unsubscribe
   */
  protected sub: Subscription;

  /**
   * Initialize the component variables.
   * @param {ExternalSourceDataService} externalService
   * @param {ChangeDetectorRef} cdr
   * @param {HostWindowService} windowService
   */
  constructor(
    private externalService: ExternalSourceDataService,
    private cdr: ChangeDetectorRef,
    protected windowService: HostWindowService
  ) {
  }

  /**
   * Component initialization and retrieve first page of external sources.
   */
  ngOnInit() {
    this.selectedElement = {
      id: '',
      name: 'loading'
    };
    this.searchString = '';
    this.sourceList = [];
    this.findListOptions = Object.assign({}, new FindListOptions(), {
      elementsPerPage: 5,
      currentPage: 1,
      searchParams: [
        new RequestParam('entityType', this.initExternalSourceData.entity)
      ]
    });
    this.externalService.searchBy('findByEntityType', this.findListOptions).pipe(
      catchError(() => {
        const pageInfo = new PageInfo();
        const paginatedList = buildPaginatedList(pageInfo, []);
        const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
        return observableOf(paginatedListRD);
      }),
      getFirstSucceededRemoteDataPayload(),
    ).subscribe((externalSource: PaginatedList<ExternalSource>) => {
      externalSource.page.forEach((element) => {
        this.sourceList.push({ id: element.id, name: element.name });
        if (this.initExternalSourceData.sourceId === element.id) {
          this.selectedElement = { id: element.id, name: element.name };
          this.searchString = this.initExternalSourceData.query;
        }
      });
      if (this.selectedElement.id === '') {
        this.selectedElement = this.sourceList[0];
      }
      this.pageInfo = externalSource.pageInfo;
      this.cdr.detectChanges();
    });
    this.isXsOrSm$ = this.windowService.isXsOrSm();
  }

  /**
   * Set the selected external source.
   */
  public makeSourceSelection(source): void {
    this.selectedElement = source;
  }

  /**
   * Load the next pages of external sources.
   */
  public onScroll(): void {
    if (!this.sourceListLoading && ((this.pageInfo.currentPage + 1) <= this.pageInfo.totalPages)) {
      this.sourceListLoading = true;
      this.findListOptions = Object.assign({}, new FindListOptions(), {
        elementsPerPage: 5,
        currentPage: this.findListOptions.currentPage + 1,
        searchParams: [
          new RequestParam('entityType', this.initExternalSourceData.entity)
        ]
      });
      this.externalService.searchBy('findByEntityType', this.findListOptions).pipe(
        catchError(() => {
          const pageInfo = new PageInfo();
          const paginatedList = buildPaginatedList(pageInfo, []);
          const paginatedListRD = createSuccessfulRemoteDataObject(paginatedList);
          return observableOf(paginatedListRD);
        }),
        getFirstSucceededRemoteData(),
        tap(() => this.sourceListLoading = false)
      ).subscribe((externalSource: RemoteData<PaginatedList<ExternalSource>>) => {
        externalSource.payload.page.forEach((element) => {
          this.sourceList.push({ id: element.id, name: element.name });
        });
        this.pageInfo = externalSource.payload.pageInfo;
        this.cdr.detectChanges();
      });
    }
  }

  /**
   * Passes the search parameters to the parent component.
   */
  public search(): void {
    this.externalSourceData.emit(
      {
        entity: this.initExternalSourceData.entity,
        sourceId: this.selectedElement.id,
        query: this.searchString
      }
    );
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    if (hasValue(this.sub)) {
      this.sub.unsubscribe();
    }
  }

}
