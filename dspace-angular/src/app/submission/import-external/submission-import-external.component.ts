import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { BehaviorSubject, combineLatest, Subscription } from 'rxjs';
import { filter, mergeMap, switchMap, take, tap } from 'rxjs/operators';
import { NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';

import { ExternalSourceDataService } from '../../core/data/external-source-data.service';
import { ExternalSourceData } from './import-external-searchbar/submission-import-external-searchbar.component';
import { RemoteData } from '../../core/data/remote-data';
import { buildPaginatedList, PaginatedList } from '../../core/data/paginated-list.model';
import { ExternalSourceEntry } from '../../core/shared/external-source-entry.model';
import { SearchConfigurationService } from '../../core/shared/search/search-configuration.service';
import { Context } from '../../core/shared/context.model';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { RouteService } from '../../core/services/route.service';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { SubmissionImportExternalPreviewComponent } from './import-external-preview/submission-import-external-preview.component';
import { fadeIn } from '../../shared/animations/fade';
import { PageInfo } from '../../core/shared/page-info.model';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { getFinishedRemoteData } from '../../core/shared/operators';
import { NONE_ENTITY_TYPE } from '../../core/shared/item-relationships/item-type.resource-type';

/**
 * This component allows to submit a new workspaceitem importing the data from an external source.
 */
@Component({
  selector: 'ds-submission-import-external',
  styleUrls: ['./submission-import-external.component.scss'],
  templateUrl: './submission-import-external.component.html',
  animations: [fadeIn]
})
export class SubmissionImportExternalComponent implements OnInit, OnDestroy {

  /**
   * The external source search data from the routing service.
   */
  public routeData: ExternalSourceData;
  /**
   * The displayed list of entries
   */
  public entriesRD$: BehaviorSubject<RemoteData<PaginatedList<ExternalSourceEntry>>> = new BehaviorSubject<RemoteData<PaginatedList<ExternalSourceEntry>>>(null);
  /**
   * TRUE if the REST service is called to retrieve the external source items
   */
  public isLoading$: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  public reload$: BehaviorSubject<ExternalSourceData> = new BehaviorSubject<ExternalSourceData>({
    entity: '',
    query: '',
    sourceId: ''
  });
  /**
   * Configuration to use for the import buttons
   */
  public importConfig: { buttonLabel: string };
  /**
   * Suffix for button label
   */
  public label: string;
  /**
   * The ID of the list to add/remove selected items to/from
   */
  public listId: string;
  /**
   * TRUE if the selection is repeatable
   */
  public repeatable: boolean;
  /**
   * The initial pagination options
   */
  public initialPagination = Object.assign(new PaginationComponentOptions(), {
    id: 'spc',
    pageSize: 10
  });
  /**
   * The context to displaying lists for
   */
  public context: Context;
  /**
   * The modal for the entry preview
   */
  public modalRef: NgbModalRef;

  /**
   * The subscription to unsubscribe
   */
  protected subs: Subscription[] = [];

  private retrieveExternalSourcesSub: Subscription;

  /**
   * Initialize the component variables.
   * @param {SearchConfigurationService} searchConfigService
   * @param {ExternalSourceDataService} externalService
   * @param {RouteService} routeService
   * @param {Router} router
   * @param {NgbModal} modalService
   */
  constructor(
    public searchConfigService: SearchConfigurationService,
    private externalService: ExternalSourceDataService,
    private routeService: RouteService,
    private router: Router,
    private modalService: NgbModal,
  ) {
  }

  /**
   * Get the entries for the selected external source and set initial configuration.
   */
  ngOnInit(): void {
    this.listId = 'list-submission-external-sources';
    this.context = Context.EntitySearchModalWithNameVariants;
    this.repeatable = false;
    this.routeData = {entity: '', sourceId: '', query: ''};
    this.importConfig = {
      buttonLabel: 'submission.sections.describe.relationship-lookup.external-source.import-button-title.' + this.label
    };
    this.entriesRD$ = new BehaviorSubject(createSuccessfulRemoteDataObject(buildPaginatedList(new PageInfo(), [])));
    this.isLoading$ = new BehaviorSubject(false);
    this.subs.push(combineLatest(
      [
        this.routeService.getQueryParameterValue('entity'),
        this.routeService.getQueryParameterValue('sourceId'),
        this.routeService.getQueryParameterValue('query')
      ]).pipe(
      take(1)
    ).subscribe(([entity, sourceId, query]: [string, string, string]) => {
      this.reload$.next({entity: entity || NONE_ENTITY_TYPE, query: query, sourceId: sourceId});
      this.selectLabel(entity);
      this.retrieveExternalSources();
    }));
  }

  /**
   * Get the data from the searchbar and changes the router data.
   */
  public getExternalSourceData(event: ExternalSourceData): void {
    this.router.navigate(
      [],
      {
        queryParams: event,
        replaceUrl: true
      }
    ).then(() => {
      this.reload$.next(event);
      this.retrieveExternalSources();
    });
  }

  /**
   * Display an item preview by opening up an import modal window.
   * @param entry The entry to import
   */
  public import(entry): void {
    this.modalRef = this.modalService.open(SubmissionImportExternalPreviewComponent, {
      size: 'lg',
    });
    const modalComp = this.modalRef.componentInstance;
    modalComp.externalSourceEntry = entry;
    modalComp.labelPrefix = this.label;
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.subs
      .filter((sub) => hasValue(sub))
      .forEach((sub) => sub.unsubscribe());
    if (hasValue(this.retrieveExternalSourcesSub)) {
      this.retrieveExternalSourcesSub.unsubscribe();
    }

  }

  /**
   * Retrieve external source entries.
   */
  private retrieveExternalSources(): void {
    if (hasValue(this.retrieveExternalSourcesSub)) {
      this.retrieveExternalSourcesSub.unsubscribe();
    }
    this.retrieveExternalSourcesSub = this.reload$.pipe(
      filter((sourceQueryObject: ExternalSourceData) => isNotEmpty(sourceQueryObject.sourceId) && isNotEmpty(sourceQueryObject.query)),
      switchMap((sourceQueryObject: ExternalSourceData) => {
          const query = sourceQueryObject.query;
          this.routeData = sourceQueryObject;
          return this.searchConfigService.paginatedSearchOptions.pipe(
            tap(() => this.isLoading$.next(true)),
            filter((searchOptions) => searchOptions.query === query),
            mergeMap((searchOptions) => this.externalService.getExternalSourceEntries(this.routeData.sourceId, searchOptions).pipe(
              getFinishedRemoteData(),
            ))
          );
        }
      ),
    ).subscribe((rdData) => {
      this.entriesRD$.next(rdData);
      this.isLoading$.next(false);
    });
  }

  /**
   * Set the correct button label, depending on the entity.
   *
   * @param entity The entity name
   */
  private selectLabel(entity: string): void {
    this.label = entity;
    this.importConfig = {
      buttonLabel: 'submission.sections.describe.relationship-lookup.external-source.import-button-title.' + this.label
    };
  }

}
