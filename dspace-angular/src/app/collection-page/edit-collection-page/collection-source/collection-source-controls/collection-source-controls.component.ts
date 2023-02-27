import { Component, Input, OnDestroy } from '@angular/core';
import { ScriptDataService } from '../../../../core/data/processes/script-data.service';
import { ContentSource } from '../../../../core/shared/content-source.model';
import { ProcessDataService } from '../../../../core/data/processes/process-data.service';
import {
  getAllCompletedRemoteData,
  getAllSucceededRemoteDataPayload,
  getFirstCompletedRemoteData,
  getFirstSucceededRemoteDataPayload
} from '../../../../core/shared/operators';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { hasValue, hasValueOperator } from '../../../../shared/empty.util';
import { ProcessStatus } from '../../../../process-page/processes/process-status.model';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { RequestService } from '../../../../core/data/request.service';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { Collection } from '../../../../core/shared/collection.model';
import { CollectionDataService } from '../../../../core/data/collection-data.service';
import { Process } from '../../../../process-page/processes/process.model';
import { TranslateService } from '@ngx-translate/core';
import { HttpClient } from '@angular/common/http';
import { BitstreamDataService } from '../../../../core/data/bitstream-data.service';
import { ContentSourceSetSerializer } from '../../../../core/shared/content-source-set-serializer';

/**
 * Component that contains the controls to run, reset and test the harvest
 */
@Component({
  selector: 'ds-collection-source-controls',
  styleUrls: ['./collection-source-controls.component.scss'],
  templateUrl: './collection-source-controls.component.html',
})
export class CollectionSourceControlsComponent implements OnDestroy {

  /**
   * Should the controls be enabled.
   */
  @Input() isEnabled: boolean;

  /**
   * The current collection
   */
  @Input() collection: Collection;

  /**
   * Should the control section be shown
   */
  @Input() shouldShow: boolean;

  contentSource$: Observable<ContentSource>;
  private subs: Subscription[] = [];

  testConfigRunning$ = new BehaviorSubject(false);
  importRunning$ = new BehaviorSubject(false);
  reImportRunning$ = new BehaviorSubject(false);

  constructor(private scriptDataService: ScriptDataService,
              private processDataService: ProcessDataService,
              private requestService: RequestService,
              private notificationsService: NotificationsService,
              private collectionService: CollectionDataService,
              private translateService: TranslateService,
              private httpClient: HttpClient,
              private bitstreamService: BitstreamDataService
  ) {
  }

  ngOnInit() {
    // ensure the contentSource gets updated after being set to stale
    this.contentSource$ = this.collectionService.findByHref(this.collection._links.self.href, false).pipe(
      getAllSucceededRemoteDataPayload(),
      switchMap((collection) => this.collectionService.getContentSource(collection.uuid, false)),
      getAllSucceededRemoteDataPayload()
    );
  }

  /**
   * Tests the provided content source's configuration.
   * @param contentSource - The content source to be tested
   */
  testConfiguration(contentSource) {
    this.testConfigRunning$.next(true);
    this.subs.push(this.scriptDataService.invoke('harvest', [
      {name: '-g', value: null},
      {name: '-a', value: contentSource.oaiSource},
      {name: '-i', value: new ContentSourceSetSerializer().Serialize(contentSource.oaiSetId)},
    ], []).pipe(
      getFirstCompletedRemoteData(),
      tap((rd) => {
        if (rd.hasFailed) {
          // show a notification when the script invocation fails
          this.notificationsService.error(this.translateService.get('collection.source.controls.test.submit.error'));
          this.testConfigRunning$.next(false);
        }
      }),
      // filter out responses that aren't successful since the pinging of the process only needs to happen when the invocation was successful.
      filter((rd) => rd.hasSucceeded && hasValue(rd.payload)),
      switchMap((rd) => this.processDataService.findById(rd.payload.processId, false)),
      getAllCompletedRemoteData(),
      filter((rd) => !rd.isStale && (rd.hasSucceeded || rd.hasFailed)),
      map((rd) => rd.payload),
      hasValueOperator(),
    ).subscribe((process: Process) => {
        if (process.processStatus.toString() !== ProcessStatus[ProcessStatus.COMPLETED].toString() &&
          process.processStatus.toString() !== ProcessStatus[ProcessStatus.FAILED].toString()) {
          // Ping the current process state every 5s
          setTimeout(() => {
            this.requestService.setStaleByHrefSubstring(process._links.self.href);
          }, 5000);
        }
        if (process.processStatus.toString() === ProcessStatus[ProcessStatus.FAILED].toString()) {
          this.notificationsService.error(this.translateService.get('collection.source.controls.test.failed'));
          this.testConfigRunning$.next(false);
        }
        if (process.processStatus.toString() === ProcessStatus[ProcessStatus.COMPLETED].toString()) {
          this.bitstreamService.findByHref(process._links.output.href).pipe(getFirstSucceededRemoteDataPayload()).subscribe((bitstream) => {
            this.httpClient.get(bitstream._links.content.href, {responseType: 'text'}).subscribe((data: any) => {
              const output = data.replaceAll(new RegExp('.*\\@(.*)', 'g'), '$1')
                .replaceAll('The script has started', '')
                .replaceAll('The script has completed', '');
              this.notificationsService.info(this.translateService.get('collection.source.controls.test.completed'), output);
            });
          });
          this.testConfigRunning$.next(false);
        }
      }
    ));
  }

  /**
   * Start the harvest for the current collection
   */
  importNow() {
    this.importRunning$.next(true);
    this.subs.push(this.scriptDataService.invoke('harvest', [
      {name: '-r', value: null},
      {name: '-c', value: this.collection.uuid},
    ], [])
      .pipe(
        getFirstCompletedRemoteData(),
        tap((rd) => {
          if (rd.hasFailed) {
            this.notificationsService.error(this.translateService.get('collection.source.controls.import.submit.error'));
            this.importRunning$.next(false);
          } else {
            this.notificationsService.success(this.translateService.get('collection.source.controls.import.submit.success'));
          }
        }),
        filter((rd) => rd.hasSucceeded && hasValue(rd.payload)),
        switchMap((rd) => this.processDataService.findById(rd.payload.processId, false)),
        getAllCompletedRemoteData(),
        filter((rd) => !rd.isStale && (rd.hasSucceeded || rd.hasFailed)),
        map((rd) => rd.payload),
        hasValueOperator(),
      ).subscribe((process) => {
          if (process.processStatus.toString() !== ProcessStatus[ProcessStatus.COMPLETED].toString() &&
            process.processStatus.toString() !== ProcessStatus[ProcessStatus.FAILED].toString()) {
            // Ping the current process state every 5s
            setTimeout(() => {
              this.requestService.setStaleByHrefSubstring(process._links.self.href);
              this.requestService.setStaleByHrefSubstring(this.collection._links.self.href);
            }, 5000);
          }
          if (process.processStatus.toString() === ProcessStatus[ProcessStatus.FAILED].toString()) {
            this.notificationsService.error(this.translateService.get('collection.source.controls.import.failed'));
            this.importRunning$.next(false);
          }
          if (process.processStatus.toString() === ProcessStatus[ProcessStatus.COMPLETED].toString()) {
            this.notificationsService.success(this.translateService.get('collection.source.controls.import.completed'));
            this.requestService.setStaleByHrefSubstring(this.collection._links.self.href);
            this.importRunning$.next(false);
          }
        }
      ));
  }

  /**
   * Reset and reimport the current collection
   */
  resetAndReimport() {
    this.reImportRunning$.next(true);
    this.subs.push(this.scriptDataService.invoke('harvest', [
      {name: '-o', value: null},
      {name: '-c', value: this.collection.uuid},
    ], [])
      .pipe(
        getFirstCompletedRemoteData(),
        tap((rd) => {
          if (rd.hasFailed) {
            this.notificationsService.error(this.translateService.get('collection.source.controls.reset.submit.error'));
            this.reImportRunning$.next(false);
          } else {
            this.notificationsService.success(this.translateService.get('collection.source.controls.reset.submit.success'));
          }
        }),
        filter((rd) => rd.hasSucceeded && hasValue(rd.payload)),
        switchMap((rd) => this.processDataService.findById(rd.payload.processId, false)),
        getAllCompletedRemoteData(),
        filter((rd) => !rd.isStale && (rd.hasSucceeded || rd.hasFailed)),
        map((rd) => rd.payload),
        hasValueOperator(),
      ).subscribe((process) => {
          if (process.processStatus.toString() !== ProcessStatus[ProcessStatus.COMPLETED].toString() &&
            process.processStatus.toString() !== ProcessStatus[ProcessStatus.FAILED].toString()) {
            // Ping the current process state every 5s
            setTimeout(() => {
              this.requestService.setStaleByHrefSubstring(process._links.self.href);
              this.requestService.setStaleByHrefSubstring(this.collection._links.self.href);
            }, 5000);
          }
          if (process.processStatus.toString() === ProcessStatus[ProcessStatus.FAILED].toString()) {
            this.notificationsService.error(this.translateService.get('collection.source.controls.reset.failed'));
            this.reImportRunning$.next(false);
          }
          if (process.processStatus.toString() === ProcessStatus[ProcessStatus.COMPLETED].toString()) {
            this.notificationsService.success(this.translateService.get('collection.source.controls.reset.completed'));
            this.requestService.setStaleByHrefSubstring(this.collection._links.self.href);
            this.reImportRunning$.next(false);
          }
        }
      ));
  }

  ngOnDestroy(): void {
    this.subs.forEach((sub) => {
      if (hasValue(sub)) {
        sub.unsubscribe();
      }
    });
  }
}
