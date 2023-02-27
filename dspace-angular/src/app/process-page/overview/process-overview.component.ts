import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { RemoteData } from '../../core/data/remote-data';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { Process } from '../processes/process.model';
import { PaginationComponentOptions } from '../../shared/pagination/pagination-component-options.model';
import { EPersonDataService } from '../../core/eperson/eperson-data.service';
import { getFirstSucceededRemoteDataPayload } from '../../core/shared/operators';
import { EPerson } from '../../core/eperson/models/eperson.model';
import { map, switchMap } from 'rxjs/operators';
import { ProcessDataService } from '../../core/data/processes/process-data.service';
import { PaginationService } from '../../core/pagination/pagination.service';
import { FindListOptions } from '../../core/data/find-list-options.model';
import { ProcessBulkDeleteService } from './process-bulk-delete.service';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { hasValue } from '../../shared/empty.util';

@Component({
  selector: 'ds-process-overview',
  templateUrl: './process-overview.component.html',
})
/**
 * Component displaying a list of all processes in a paginated table
 */
export class ProcessOverviewComponent implements OnInit, OnDestroy {

  /**
   * List of all processes
   */
  processesRD$: Observable<RemoteData<PaginatedList<Process>>>;

  /**
   * The current pagination configuration for the page used by the FindAll method
   */
  config: FindListOptions = Object.assign(new FindListOptions(), {
    elementsPerPage: 20
  });

  /**
   * The current pagination configuration for the page
   */
  pageConfig: PaginationComponentOptions = Object.assign(new PaginationComponentOptions(), {
    id: 'po',
    pageSize: 20
  });

  /**
   * Date format to use for start and end time of processes
   */
  dateFormat = 'yyyy-MM-dd HH:mm:ss';

  processesToDelete: string[] = [];
  private modalRef: any;

  isProcessingSub: Subscription;

  constructor(protected processService: ProcessDataService,
              protected paginationService: PaginationService,
              protected ePersonService: EPersonDataService,
              protected modalService: NgbModal,
              public processBulkDeleteService: ProcessBulkDeleteService,
  ) {
  }

  ngOnInit(): void {
    this.setProcesses();
    this.processBulkDeleteService.clearAllProcesses();
  }

  /**
   * Send a request to fetch all processes for the current page
   */
  setProcesses() {
    this.processesRD$ = this.paginationService.getFindListOptions(this.pageConfig.id, this.config).pipe(
      switchMap((config) => this.processService.findAll(config, true, false))
    );
  }

  /**
   * Get the name of an EPerson by ID
   * @param id  ID of the EPerson
   */
  getEpersonName(id: string): Observable<string> {
    return this.ePersonService.findById(id).pipe(
      getFirstSucceededRemoteDataPayload(),
      map((eperson: EPerson) => eperson.name)
    );
  }

  ngOnDestroy(): void {
    this.paginationService.clearPagination(this.pageConfig.id);
    if (hasValue(this.isProcessingSub)) {
      this.isProcessingSub.unsubscribe();
    }
  }

  /**
   * Open a given modal.
   * @param content   - the modal content.
   */
  openDeleteModal(content) {
    this.modalRef = this.modalService.open(content);
  }

  /**
   * Close the modal.
   */
  closeModal() {
    this.modalRef.close();
  }

  /**
   * Delete the previously selected processes using the processBulkDeleteService
   * After the deletion has started, subscribe to the isProcessing$ and when it is set
   * to false after the processing is done, close the modal and reinitialise the processes
   */
  deleteSelected() {
    this.processBulkDeleteService.deleteSelectedProcesses();

    if (hasValue(this.isProcessingSub)) {
      this.isProcessingSub.unsubscribe();
    }
    this.isProcessingSub = this.processBulkDeleteService.isProcessing$()
      .subscribe((isProcessing) => {
      if (!isProcessing) {
        this.closeModal();
        this.setProcesses();
      }
    });
  }
}
