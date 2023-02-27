import { Process } from '../processes/process.model';
import { Injectable } from '@angular/core';
import { ProcessDataService } from '../../core/data/processes/process-data.service';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { isNotEmpty } from '../../shared/empty.util';
import { BehaviorSubject, count, from } from 'rxjs';
import { getFirstCompletedRemoteData } from '../../core/shared/operators';
import { concatMap, filter, tap } from 'rxjs/operators';
import { RemoteData } from '../../core/data/remote-data';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
/**
 * Service to facilitate removing processes in bulk.
 */
export class ProcessBulkDeleteService {

  /**
   * Array to track the processes to be deleted
   */
  processesToDelete: string[] = [];

  /**
   * Behavior subject to track whether the delete is processing
   * @protected
   */
  protected isProcessingBehaviorSubject: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(false);

  constructor(
    protected processDataService: ProcessDataService,
    protected notificationsService: NotificationsService,
    protected translateService: TranslateService
  ) {
  }

  /**
   * Add or remove a process id to/from the list
   * If the id is already present it will be removed, otherwise it will be added.
   *
   * @param processId - The process id to add or remove
   */
  toggleDelete(processId: string) {
    if (this.isToBeDeleted(processId)) {
      this.processesToDelete.splice(this.processesToDelete.indexOf(processId), 1);
    } else {
      this.processesToDelete.push(processId);
    }
  }

  /**
   * Checks if the provided process id is present in the to be deleted list
   * @param processId
   */
  isToBeDeleted(processId: string) {
    return this.processesToDelete.includes(processId);
  }

  /**
   * Clear the list of processes to be deleted
   */
  clearAllProcesses() {
    this.processesToDelete.splice(0);
  }

  /**
   * Get the amount of processes selected for deletion
   */
  getAmountOfSelectedProcesses() {
    return this.processesToDelete.length;
  }

  /**
   * Returns a behavior subject to indicate whether the bulk delete is processing
   */
  isProcessing$() {
    return this.isProcessingBehaviorSubject;
  }

  /**
   * Returns whether there currently are values selected for deletion
   */
  hasSelected(): boolean {
    return isNotEmpty(this.processesToDelete);
  }

  /**
   * Delete all selected processes one by one
   * When the deletion for a process fails, an error notification will be shown with the process id,
   * but it will continue deleting the other processes.
   * At the end it will show a notification stating the amount of successful deletes
   * The successfully deleted processes will be removed from the list of selected values, the failed ones will be retained.
   */
  deleteSelectedProcesses() {
    this.isProcessingBehaviorSubject.next(true);

    from([...this.processesToDelete]).pipe(
      concatMap((processId) => {
        return this.processDataService.delete(processId).pipe(
          getFirstCompletedRemoteData(),
          tap((rd: RemoteData<Process>) => {
            if (rd.hasFailed) {
              this.notificationsService.error(this.translateService.get('process.bulk.delete.error.head'), this.translateService.get('process.bulk.delete.error.body', {processId: processId}));
            } else {
              this.toggleDelete(processId);
            }
          })
        );
      }),
      filter((rd: RemoteData<Process>) => rd.hasSucceeded),
      count(),
    ).subscribe((value) => {
      this.notificationsService.success(this.translateService.get('process.bulk.delete.success', {count: value}));
      this.isProcessingBehaviorSubject.next(false);
    });
  }
}
