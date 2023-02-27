import { Component, Input, OnDestroy, OnInit, SimpleChanges } from '@angular/core';

import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, combineLatest, Observable, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import { PaginatedList } from '../../../core/data/paginated-list.model';
import { RemoteData } from '../../../core/data/remote-data';
import { OrcidHistory } from '../../../core/orcid/model/orcid-history.model';
import { OrcidQueue } from '../../../core/orcid/model/orcid-queue.model';
import { OrcidHistoryDataService } from '../../../core/orcid/orcid-history-data.service';
import { OrcidQueueDataService } from '../../../core/orcid/orcid-queue-data.service';
import { PaginationService } from '../../../core/pagination/pagination.service';
import { getFirstCompletedRemoteData } from '../../../core/shared/operators';
import { hasValue } from '../../../shared/empty.util';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { PaginationComponentOptions } from '../../../shared/pagination/pagination-component-options.model';
import { AlertType } from '../../../shared/alert/aletr-type';
import { Item } from '../../../core/shared/item.model';
import { OrcidAuthService } from '../../../core/orcid/orcid-auth.service';

@Component({
  selector: 'ds-orcid-queue',
  templateUrl: './orcid-queue.component.html',
  styleUrls: ['./orcid-queue.component.scss']
})
export class OrcidQueueComponent implements OnInit, OnDestroy {

  /**
   * The item for which showing the orcid settings
   */
  @Input() item: Item;

  /**
   * Pagination config used to display the list
   */
  public paginationOptions: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'oqp',
    pageSize: 5
  });

  /**
   * A boolean representing if results are loading
   */
  public processing$ = new BehaviorSubject<boolean>(false);

  /**
   * A list of orcid queue records
   */
  private list$: BehaviorSubject<RemoteData<PaginatedList<OrcidQueue>>> = new BehaviorSubject<RemoteData<PaginatedList<OrcidQueue>>>({} as any);

  /**
   * The AlertType enumeration
   * @type {AlertType}
   */
  AlertTypeEnum = AlertType;

  /**
   * Array to track all subscriptions and unsubscribe them onDestroy
   * @type {Array}
   */
  private subs: Subscription[] = [];

  constructor(private orcidAuthService: OrcidAuthService,
              private orcidQueueService: OrcidQueueDataService,
              protected translateService: TranslateService,
              private paginationService: PaginationService,
              private notificationsService: NotificationsService,
              private orcidHistoryService: OrcidHistoryDataService,
  ) {
  }

  ngOnInit(): void {
    this.updateList();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes.item.isFirstChange() && changes.item.currentValue !== changes.item.previousValue) {
      this.updateList();
    }
  }

  /**
   * Retrieve queue list
   */
  updateList() {
    this.subs.push(
      this.paginationService.getCurrentPagination(this.paginationOptions.id, this.paginationOptions).pipe(
        debounceTime(100),
        distinctUntilChanged(),
        tap(() => this.processing$.next(true)),
        switchMap((config: PaginationComponentOptions) => this.orcidQueueService.searchByProfileItemId(this.item.id, config, false)),
        getFirstCompletedRemoteData()
      ).subscribe((result: RemoteData<PaginatedList<OrcidQueue>>) => {
        this.processing$.next(false);
        this.list$.next(result);
        this.orcidQueueService.clearFindByProfileItemRequests();
      })
    );
  }

  /**
   * Return the list of orcid queue records
   */
  getList(): Observable<RemoteData<PaginatedList<OrcidQueue>>> {
    return this.list$.asObservable();
  }

  /**
   * Return the icon class for the queue object type
   *
   * @param orcidQueue The OrcidQueue object
   */
  getIconClass(orcidQueue: OrcidQueue): string {
    if (!orcidQueue.recordType) {
      return 'fa fa-user';
    }
    switch (orcidQueue.recordType.toLowerCase()) {
      case 'publication':
        return 'fas fa-book';
      case 'project':
        return 'fas fa-wallet';
      case 'education':
        return 'fas fa-school';
      case 'affiliation':
        return 'fas fa-university';
      case 'country':
        return 'fas fa-globe-europe';
      case 'external_ids':
      case 'researcher_urls':
        return 'fas fa-external-link-alt';
      default:
        return 'fa fa-user';
    }
  }

  /**
   * Return the icon tooltip message for the queue object type
   *
   * @param orcidQueue The OrcidQueue object
   */
  getIconTooltip(orcidQueue: OrcidQueue): string {
    if (!orcidQueue.recordType) {
      return '';
    }

    return 'person.page.orcid.sync-queue.tooltip.' + orcidQueue.recordType.toLowerCase();
  }

  /**
   * Return the icon tooltip message for the queue object operation
   *
   * @param orcidQueue The OrcidQueue object
   */
  getOperationTooltip(orcidQueue: OrcidQueue): string {
    if (!orcidQueue.operation) {
      return '';
    }

    return 'person.page.orcid.sync-queue.tooltip.' + orcidQueue.operation.toLowerCase();
  }

  /**
   * Return the icon class for the queue object operation
   *
   * @param orcidQueue The OrcidQueue object
   */
  getOperationClass(orcidQueue: OrcidQueue): string {

    if (!orcidQueue.operation) {
      return '';
    }

    switch (orcidQueue.operation.toLowerCase()) {
      case 'insert':
        return 'fas fa-plus';
      case 'update':
        return 'fas fa-edit';
      case 'delete':
        return 'fas fa-trash-alt';
      default:
        return '';
    }
  }

  /**
   * Discard a queue entry from the synchronization
   *
   * @param orcidQueue The OrcidQueue object to discard
   */
  discardEntry(orcidQueue: OrcidQueue) {
    this.processing$.next(true);
    this.subs.push(this.orcidQueueService.deleteById(orcidQueue.id).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((remoteData) => {
      this.processing$.next(false);
      if (remoteData.isSuccess) {
        this.notificationsService.success(this.translateService.get('person.page.orcid.sync-queue.discard.success'));
        this.updateList();
      } else {
        this.notificationsService.error(this.translateService.get('person.page.orcid.sync-queue.discard.error'));
      }
    }));
  }

  /**
   * Send a queue entry to orcid for the synchronization
   *
   * @param orcidQueue The OrcidQueue object to synchronize
   */
  send(orcidQueue: OrcidQueue) {
    this.processing$.next(true);
    this.subs.push(this.orcidHistoryService.sendToORCID(orcidQueue).pipe(
      getFirstCompletedRemoteData()
    ).subscribe((remoteData) => {
      this.processing$.next(false);
      if (remoteData.isSuccess) {
        this.handleOrcidHistoryRecordCreation(remoteData.payload);
      } else if (remoteData.statusCode === 422) {
        this.handleValidationErrors(remoteData);
      } else {
        this.notificationsService.error(this.translateService.get('person.page.orcid.sync-queue.send.error'));
      }
    }));
  }


  /**
   * Return the error message for Unauthorized response
   * @private
   */
  private getUnauthorizedErrorContent(): Observable<string> {
    return this.orcidAuthService.getOrcidAuthorizeUrl(this.item).pipe(
      switchMap((authorizeUrl) => this.translateService.get(
        'person.page.orcid.sync-queue.send.unauthorized-error.content',
        { orcid: authorizeUrl }
      ))
    );
  }

  /**
   * Manage notification by response
   * @private
   */
  private handleOrcidHistoryRecordCreation(orcidHistory: OrcidHistory) {
    switch (orcidHistory.status) {
      case 200:
      case 201:
      case 204:
        this.notificationsService.success(this.translateService.get('person.page.orcid.sync-queue.send.success'));
        this.updateList();
        break;
      case 400:
        this.notificationsService.error(this.translateService.get('person.page.orcid.sync-queue.send.bad-request-error'), null, { timeOut: 0 });
        break;
      case 401:
        combineLatest([
          this.translateService.get('person.page.orcid.sync-queue.send.unauthorized-error.title'),
          this.getUnauthorizedErrorContent()],
        ).subscribe(([title, content]) => {
          this.notificationsService.error(title, content, { timeOut: 0 }, true);
        });
        break;
      case 404:
        this.notificationsService.warning(this.translateService.get('person.page.orcid.sync-queue.send.not-found-warning'));
        break;
      case 409:
        this.notificationsService.error(this.translateService.get('person.page.orcid.sync-queue.send.conflict-error'), null, { timeOut: 0 });
        break;
      default:
        this.notificationsService.error(this.translateService.get('person.page.orcid.sync-queue.send.error'), null, { timeOut: 0 });
    }
  }

  /**
   * Manage validation errors
   * @private
   */
  private handleValidationErrors(remoteData: RemoteData<OrcidHistory>) {
    const translations = [this.translateService.get('person.page.orcid.sync-queue.send.validation-error')];
    const errorMessage = remoteData.errorMessage;
    if (errorMessage && errorMessage.indexOf('Error codes:') > 0) {
      errorMessage.substring(errorMessage.indexOf(':') + 1).trim().split(',')
        .forEach((error) => translations.push(this.translateService.get('person.page.orcid.sync-queue.send.validation-error.' + error)));
    }
    combineLatest(translations).subscribe((messages) => {
      const title = messages.shift();
      const content = '<ul>' + messages.map((message) => `<li>${message}</li>`).join('') + '</ul>';
      this.notificationsService.error(title, content, { timeOut: 0 }, true);
    });
  }

  /**
   * Unsubscribe from all subscriptions
   */
  ngOnDestroy(): void {
    this.list$ = null;
    this.subs.filter((subscription) => hasValue(subscription))
      .forEach((subscription) => subscription.unsubscribe());
  }

}
