import { ProcessBulkDeleteService } from './process-bulk-delete.service';
import { waitForAsync } from '@angular/core/testing';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { getMockTranslateService } from '../../shared/mocks/translate.service.mock';

describe('ProcessBulkDeleteService', () => {

  let service: ProcessBulkDeleteService;
  let processDataService;
  let notificationsService;
  let mockTranslateService;

  beforeEach(waitForAsync(() => {
    processDataService = jasmine.createSpyObj('processDataService', {
      delete: createSuccessfulRemoteDataObject$(null)
    });
    notificationsService = new NotificationsServiceStub();
    mockTranslateService = getMockTranslateService();
    service = new ProcessBulkDeleteService(processDataService, notificationsService, mockTranslateService);
  }));

  describe('toggleDelete', () => {
    it('should add a new value to the processesToDelete list when not yet present', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.processesToDelete).toEqual(['test-id-1', 'test-id-2']);
    });
    it('should remove a value from the processesToDelete list when already present', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.processesToDelete).toEqual(['test-id-1', 'test-id-2']);

      service.toggleDelete('test-id-1');
      expect(service.processesToDelete).toEqual(['test-id-2']);
    });
  });

  describe('isToBeDeleted', () => {
    it('should return true when the provided process id is present in the list', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.isToBeDeleted('test-id-1')).toBeTrue();
    });
    it('should return false when the provided process id is not present in the list', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.isToBeDeleted('test-id-3')).toBeFalse();
    });
  });

  describe('clearAllProcesses', () => {
    it('should clear the list of to be deleted processes', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.processesToDelete).toEqual(['test-id-1', 'test-id-2']);

      service.clearAllProcesses();
      expect(service.processesToDelete).toEqual([]);
    });
  });
  describe('getAmountOfSelectedProcesses', () => {
    it('should return the amount of the currently selected processes for deletion', () => {
      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.getAmountOfSelectedProcesses()).toEqual(2);
    });
  });
  describe('isProcessing$', () => {
    it('should return a behavior subject containing whether a delete is currently processing or not', () => {
      const result = service.isProcessing$();
      expect(result.getValue()).toBeFalse();

      result.next(true);
      expect(result.getValue()).toBeTrue();
    });
  });
  describe('hasSelected', () => {
    it('should return if the list of selected processes has values', () => {
      expect(service.hasSelected()).toBeFalse();

      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');

      expect(service.hasSelected()).toBeTrue();
    });
  });
  describe('deleteSelectedProcesses', () => {
    it('should delete all selected processes, show an error for each failed one and a notification at the end with the amount of succeeded deletions', () => {
      (processDataService.delete as jasmine.Spy).and.callFake((processId: string) => {
        if (processId.includes('error')) {
          return createFailedRemoteDataObject$();
        } else {
          return createSuccessfulRemoteDataObject$(null);
        }
      });

      service.toggleDelete('test-id-1');
      service.toggleDelete('test-id-2');
      service.toggleDelete('error-id-3');
      service.toggleDelete('test-id-4');
      service.toggleDelete('error-id-5');
      service.toggleDelete('error-id-6');
      service.toggleDelete('test-id-7');


      service.deleteSelectedProcesses();

      expect(processDataService.delete).toHaveBeenCalledWith('test-id-1');


      expect(processDataService.delete).toHaveBeenCalledWith('test-id-2');


      expect(processDataService.delete).toHaveBeenCalledWith('error-id-3');
      expect(notificationsService.error).toHaveBeenCalled();
      expect(mockTranslateService.get).toHaveBeenCalledWith('process.bulk.delete.error.body', {processId: 'error-id-3'});


      expect(processDataService.delete).toHaveBeenCalledWith('test-id-4');


      expect(processDataService.delete).toHaveBeenCalledWith('error-id-5');
      expect(notificationsService.error).toHaveBeenCalled();
      expect(mockTranslateService.get).toHaveBeenCalledWith('process.bulk.delete.error.body', {processId: 'error-id-5'});


      expect(processDataService.delete).toHaveBeenCalledWith('error-id-6');
      expect(notificationsService.error).toHaveBeenCalled();
      expect(mockTranslateService.get).toHaveBeenCalledWith('process.bulk.delete.error.body', {processId: 'error-id-6'});


      expect(processDataService.delete).toHaveBeenCalledWith('test-id-7');

      expect(notificationsService.success).toHaveBeenCalled();
      expect(mockTranslateService.get).toHaveBeenCalledWith('process.bulk.delete.success', {count: 4});

      expect(service.processesToDelete).toEqual(['error-id-3', 'error-id-5', 'error-id-6']);


    });
  });
});
